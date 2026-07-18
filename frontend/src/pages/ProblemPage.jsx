import { useEffect, useMemo, useRef, useState, useCallback } from 'react'
import { useParams, Link } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { motion, AnimatePresence } from 'framer-motion'
import { fetchProblem, submitSolution, runSolution, getHint } from '../api/client.js'
import { useUser } from '../context/UserContext.jsx'
import VerdictBadge from '../components/VerdictBadge.jsx'
import Loader from '../components/Loader.jsx'
import Typewriter from '../components/Typewriter.jsx'

const LANGUAGES = [
  { id: 'PYTHON', label: 'Python', monaco: 'python', ext: 'py' },
  { id: 'JAVASCRIPT', label: 'JavaScript', monaco: 'javascript', ext: 'js' },
  { id: 'JAVA', label: 'Java', monaco: 'java', ext: 'java' },
  { id: 'CPP', label: 'C++', monaco: 'cpp', ext: 'cpp' },
  { id: 'C', label: 'C', monaco: 'c', ext: 'c' },
  { id: 'GO', label: 'Go', monaco: 'go', ext: 'go' },
]

const DIFF = {
  EASY: { label: 'Easy', color: '#34d399' },
  MEDIUM: { label: 'Medium', color: '#fbbf24' },
  HARD: { label: 'Hard', color: '#fb7185' },
}

/** Tiny, dependency-free markdown renderer (headings, inline code, fenced blocks). */
function MarkdownLite({ text }) {
  if (!text) return null
  const blocks = text.split(/```/)
  return (
    <div className="prose-arena space-y-2">
      {blocks.map((block, i) => {
        if (i % 2 === 1) {
          return (
            <pre key={i}>
              <code>{block.replace(/^\n/, '')}</code>
            </pre>
          )
        }
        return block.split('\n').map((line, j) => {
          if (line.startsWith('### ')) return <h3 key={`${i}-${j}`}>{line.slice(4)}</h3>
          if (line.trim() === '') return null
          const parts = line.split(/(`[^`]+`)/g)
          return (
            <p key={`${i}-${j}`}>
              {parts.map((part, k) =>
                part.startsWith('`') && part.endsWith('`') ? (
                  <code key={k}>{part.slice(1, -1)}</code>
                ) : (
                  <span key={k}>{part}</span>
                )
              )}
            </p>
          )
        })
      })}
    </div>
  )
}

function Field({ label, value, color }) {
  return (
    <div>
      <div className="mb-1 text-[11px] font-medium text-zinc-500">{label}</div>
      <pre className={`max-h-28 overflow-auto rounded-lg border border-white/5 bg-white/[0.02] p-2.5 font-mono text-xs ${color}`}>
        {value === '' ? '(empty)' : value}
      </pre>
    </div>
  )
}

export default function ProblemPage() {
  const { slug } = useParams()
  const { user } = useUser()
  const [problem, setProblem] = useState(null)
  const [status, setStatus] = useState('loading')
  const [language, setLanguage] = useState('PYTHON')
  const [codeByLang, setCodeByLang] = useState({})
  const [judging, setJudging] = useState(false)
  const [action, setAction] = useState(null) // 'run' | 'submit'
  const [result, setResult] = useState(null)
  const [activeCase, setActiveCase] = useState(0)
  const [error, setError] = useState('')
  const [leftPct, setLeftPct] = useState(44)

  // Socratic hints
  const [hintOpen, setHintOpen] = useState(false)
  const [hints, setHints] = useState([])
  const [hintBusy, setHintBusy] = useState(false)
  const [hintErr, setHintErr] = useState('')

  const containerRef = useRef(null)
  const draggingRef = useRef(false)

  useEffect(() => {
    setStatus('loading')
    setResult(null)
    setError('')
    setHints([])
    setHintErr('')
    fetchProblem(slug)
      .then((data) => {
        setProblem(data)
        setCodeByLang(data.starterCode ?? {})
        const available = Object.keys(data.starterCode ?? {})
        const preferred = available.includes('PYTHON') ? 'PYTHON' : available[0]
        if (preferred) setLanguage(preferred)
        setStatus('ready')
      })
      .catch((err) => {
        setError(err?.message ?? 'Failed to load')
        setStatus('error')
      })
  }, [slug])

  // Draggable split handle
  const onDragMove = useCallback((e) => {
    if (!draggingRef.current || !containerRef.current) return
    const rect = containerRef.current.getBoundingClientRect()
    const pct = ((e.clientX - rect.left) / rect.width) * 100
    setLeftPct(Math.min(68, Math.max(26, pct)))
  }, [])

  useEffect(() => {
    const stop = () => {
      draggingRef.current = false
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
    }
    window.addEventListener('mousemove', onDragMove)
    window.addEventListener('mouseup', stop)
    return () => {
      window.removeEventListener('mousemove', onDragMove)
      window.removeEventListener('mouseup', stop)
    }
  }, [onDragMove])

  const startDrag = () => {
    draggingRef.current = true
    document.body.style.cursor = 'col-resize'
    document.body.style.userSelect = 'none'
  }

  const meta = LANGUAGES.find((l) => l.id === language) ?? LANGUAGES[0]
  const monacoLang = useMemo(() => meta.monaco, [meta])
  const code = codeByLang[language] ?? ''

  const orderedLangs = LANGUAGES.filter((l) => (problem?.starterCode ?? {})[l.id] !== undefined)

  function onCodeChange(value) {
    setCodeByLang((prev) => ({ ...prev, [language]: value ?? '' }))
  }

  async function judge(kind) {
    setAction(kind)
    setJudging(true)
    setResult(null)
    setError('')
    setActiveCase(0)
    try {
      const fn = kind === 'run' ? runSolution : submitSolution
      const res = await fn(slug, language, code, user?.username)
      setResult(res)
    } catch (err) {
      setError(err?.response?.data?.message ?? err?.message ?? 'Request failed')
    } finally {
      setJudging(false)
    }
  }

  const nextHintLevel = hints.length + 1
  async function fetchHint() {
    if (nextHintLevel > 3 || hintBusy) return
    setHintBusy(true)
    setHintErr('')
    try {
      const h = await getHint(slug, nextHintLevel, language, code)
      setHints((prev) => [...prev, h])
    } catch (err) {
      setHintErr(err?.response?.data?.message ?? 'Could not fetch a hint.')
    } finally {
      setHintBusy(false)
    }
  }

  if (status === 'loading') {
    return (
      <div className="flex h-[calc(100vh-64px)] items-center justify-center">
        <Loader label="Loading problem" />
      </div>
    )
  }

  if (status === 'error') {
    return (
      <div className="mx-auto max-w-3xl px-6 py-24 text-center">
        <p className="text-accent-rose">⚠ {error}</p>
        <Link to="/" className="btn-ghost mt-6 inline-flex">
          ← Back to problems
        </Link>
      </div>
    )
  }

  const diff = DIFF[problem.difficulty] ?? DIFF.EASY

  return (
    <div ref={containerRef} className="flex h-[calc(100vh-64px)] overflow-hidden">
      {/* ---------------- Left: problem statement ---------------- */}
      <aside
        className="flex min-w-0 flex-col border-r border-white/5 bg-ink-900/60 backdrop-blur-sm"
        style={{ width: `${leftPct}%` }}
      >
        <div className="flex items-center justify-between border-b border-white/5 px-5 py-3">
          <Link to="/" className="inline-flex items-center gap-1.5 text-sm text-zinc-400 transition-colors hover:text-white">
            ← Problems
          </Link>
          <span
            className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium"
            style={{ color: diff.color, backgroundColor: `${diff.color}1a` }}
          >
            <span className="dot" style={{ backgroundColor: diff.color }} />
            {diff.label}
          </span>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
          <h1 className="font-display text-2xl font-bold tracking-tight text-white">{problem.title}</h1>

          <div className="mt-3 mb-5 flex flex-wrap gap-1.5">
            {(problem.tags ?? []).map((t) => (
              <span key={t} className="chip text-zinc-400">{t}</span>
            ))}
            <span className="chip text-zinc-500">⏱ {problem.timeLimitMs}ms</span>
            <span className="chip text-zinc-500">▦ {problem.memoryLimitMb}MB</span>
          </div>

          <MarkdownLite text={problem.description} />

          {problem.sampleTestCases?.length > 0 && (
            <div className="mt-7">
              <h3 className="mb-3 font-display text-xs font-semibold uppercase tracking-wider text-brand-400">
                Sample cases
              </h3>
              <div className="space-y-3">
                {problem.sampleTestCases.map((tc, i) => (
                  <div key={i} className="grid grid-cols-2 gap-3">
                    <div>
                      <div className="mb-1 text-[11px] font-medium text-zinc-500">Input</div>
                      <pre className="rounded-lg border border-white/5 bg-white/[0.02] p-2.5 font-mono text-xs text-accent-cyan">{tc.input}</pre>
                    </div>
                    <div>
                      <div className="mb-1 text-[11px] font-medium text-zinc-500">Output</div>
                      <pre className="rounded-lg border border-white/5 bg-white/[0.02] p-2.5 font-mono text-xs text-accent-emerald">{tc.expectedOutput}</pre>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </aside>

      {/* ---------------- Splitter ---------------- */}
      <div
        onMouseDown={startDrag}
        className="group relative w-1.5 shrink-0 cursor-col-resize bg-white/5 transition-colors hover:bg-brand-500/50"
        title="Drag to resize"
      >
        <div className="absolute inset-y-0 -left-1 -right-1" />
      </div>

      {/* ---------------- Right: editor + console ---------------- */}
      <section className="flex min-w-0 flex-1 flex-col bg-ink-900/40">
        {/* Toolbar */}
        <div className="flex items-center justify-between gap-3 border-b border-white/5 bg-ink-850/70 px-3 py-2 backdrop-blur-sm">
          <div className="flex items-center gap-2">
            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
              className="rounded-lg border border-white/10 bg-ink-800 px-3 py-1.5 text-sm font-medium text-zinc-200 outline-none transition-colors hover:border-brand-500/50 focus:border-brand-500"
            >
              {orderedLangs.map((l) => (
                <option key={l.id} value={l.id} className="bg-ink-800">
                  {l.label}
                </option>
              ))}
            </select>
            <span className="hidden font-mono text-xs text-zinc-600 sm:inline">main.{meta.ext}</span>
          </div>

          <div className="flex items-center gap-2">
            <button onClick={() => setHintOpen(true)} className="btn-ghost !py-1.5 !text-sm" title="Socratic hints">
              💡 Hint
            </button>
            <button onClick={() => judge('run')} disabled={judging} className="btn-ghost !py-1.5 !text-sm">
              {judging && action === 'run' ? 'Running…' : '▷ Run'}
            </button>
            <button onClick={() => judge('submit')} disabled={judging} className="btn-primary !py-1.5 !text-sm">
              {judging && action === 'submit' ? 'Judging…' : 'Submit ▶'}
            </button>
          </div>
        </div>

        {/* Editor */}
        <div className="min-h-0 flex-1">
          <Editor
            height="100%"
            theme="vs-dark"
            language={monacoLang}
            value={code}
            onChange={onCodeChange}
            options={{
              fontSize: 14,
              fontFamily: 'JetBrains Mono, monospace',
              fontLigatures: true,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              padding: { top: 14 },
              smoothScrolling: true,
              cursorBlinking: 'smooth',
              automaticLayout: true,
              renderLineHighlight: 'all',
            }}
          />
        </div>

        {/* Console / result */}
        <div className="flex h-[38%] min-h-[180px] flex-col border-t border-white/10 bg-ink-900/80">
          <div className="flex items-center justify-between border-b border-white/5 px-4 py-2">
            <span className="text-sm font-semibold text-zinc-300">Console</span>
            {result && !result.contestMode && (
              <span className="text-xs text-zinc-500">
                <span className="font-semibold text-zinc-300">{result.passedTests}</span>/
                {result.totalTests} passed
                {result.runtimeMs != null && <span className="ml-2">· {result.runtimeMs}ms</span>}
              </span>
            )}
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto p-4">
            <AnimatePresence mode="wait">
              {judging && (
                <motion.div key="judging" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
                  <Loader label={action === 'run' ? 'Running samples' : 'Judging'} />
                </motion.div>
              )}

              {!judging && error && (
                <motion.div key="error" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="text-sm text-accent-rose">
                  ⚠ {error}
                </motion.div>
              )}

              {!judging && result && (
                <motion.div key="result" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="space-y-3">
                  <div className="flex flex-wrap items-center gap-3">
                    <VerdictBadge verdict={result.verdict} />
                    {!result.contestMode && (
                      <span className="text-xs text-zinc-500">
                        <span className="font-semibold text-zinc-300">{result.passedTests}</span>/
                        {result.totalTests} {action === 'run' ? 'samples' : 'tests'} passed
                        {result.runtimeMs != null && <span className="ml-2">· {result.runtimeMs}ms</span>}
                      </span>
                    )}
                    {result.contestMode && (
                      <span className="chip !text-accent-amber text-[10px]">contest · result hidden</span>
                    )}
                  </div>

                  {result.cases && result.cases.length > 0 ? (
                    <div>
                      <div className="mb-3 flex flex-wrap gap-1.5">
                        {result.cases.map((c, i) => (
                          <button
                            key={i}
                            onClick={() => setActiveCase(i)}
                            className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
                              activeCase === i ? 'bg-white/10 text-white' : 'text-zinc-400 hover:text-zinc-200'
                            }`}
                          >
                            <span style={{ color: c.passed ? '#34d399' : '#fb7185' }}>{c.passed ? '✓' : '✕'}</span>{' '}
                            Case {c.index}
                          </button>
                        ))}
                      </div>
                      {(() => {
                        const c = result.cases[activeCase] ?? result.cases[0]
                        const hasIo = c.input != null || c.expectedOutput != null || c.actualOutput != null
                        return (
                          <div className="space-y-2">
                            <div className="text-xs">
                              <span
                                style={{ color: c.passed ? '#34d399' : '#fb7185' }}
                                className="font-semibold"
                              >
                                {c.passed ? 'Passed' : c.verdict.replace(/_/g, ' ')}
                              </span>
                              {c.runtimeMs != null && <span className="ml-2 text-zinc-500">{c.runtimeMs}ms</span>}
                            </div>
                            {hasIo ? (
                              <>
                                <Field label="Input" value={c.input} color="text-accent-cyan" />
                                <Field label="Expected" value={c.expectedOutput} color="text-accent-emerald" />
                                <Field
                                  label="Your output"
                                  value={c.actualOutput}
                                  color={c.passed ? 'text-accent-emerald' : 'text-accent-rose'}
                                />
                              </>
                            ) : (
                              <p className="text-xs text-zinc-500">
                                Hidden test case — details are not shown
                                {result.contestMode ? ' during a contest' : ''}.
                              </p>
                            )}
                          </div>
                        )
                      })()}
                    </div>
                  ) : (
                    result.message && (
                      <pre className="overflow-auto rounded-lg border border-white/5 bg-white/[0.02] p-3 font-mono text-xs text-zinc-400">
                        {result.message}
                      </pre>
                    )
                  )}
                </motion.div>
              )}

              {!judging && !result && !error && (
                <motion.div key="idle" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex h-full items-center justify-center text-center text-sm text-zinc-500">
                  <span><span className="text-zinc-300">Run</span> tests the sample cases · <span className="text-zinc-300">Submit</span> checks all cases.</span>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </section>

      {/* ---------------- Socratic hint drawer ---------------- */}
      <AnimatePresence>
        {hintOpen && (
          <motion.div
            className="fixed inset-0 z-40 bg-black/50 backdrop-blur-sm"
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            onClick={() => setHintOpen(false)}
          >
            <motion.aside
              className="absolute right-0 top-0 flex h-full w-full max-w-md flex-col border-l border-white/10 bg-ink-900"
              initial={{ x: '100%' }} animate={{ x: 0 }} exit={{ x: '100%' }}
              transition={{ type: 'tween', duration: 0.25 }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="flex items-center justify-between border-b border-white/10 px-5 py-3">
                <h3 className="font-display text-lg font-bold text-white">💡 Socratic hints</h3>
                <button onClick={() => setHintOpen(false)} className="text-zinc-500 hover:text-white">✕</button>
              </div>

              <div className="min-h-0 flex-1 overflow-y-auto px-5 py-4">
                {!user ? (
                  <p className="py-10 text-center text-sm text-zinc-500">Sign in to get hints.</p>
                ) : (
                  <>
                    <p className="mb-4 text-xs text-zinc-500">
                      Guidance that nudges you toward the idea — it never reveals the full solution.
                    </p>
                    <div className="space-y-3">
                      {hints.map((h, i) => (
                        <div key={h.level} className="rounded-xl glass p-4">
                          <div className="mb-2 flex items-center gap-2">
                            <span className="chip !text-brand-300 text-[10px]">Level {h.level}</span>
                            <span className="text-[10px] text-zinc-600">
                              {h.source === 'groq' ? 'AI' : 'offline'}
                            </span>
                          </div>
                          <div className="text-sm leading-relaxed text-zinc-200">
                            <Typewriter
                              text={h.text}
                              animate={i === hints.length - 1}
                              render={(t) => <MarkdownLite text={t} />}
                            />
                          </div>
                        </div>
                      ))}
                    </div>

                    {hintErr && <p className="mt-3 text-xs text-accent-rose">⚠ {hintErr}</p>}

                    <div className="mt-4">
                      {nextHintLevel <= 3 ? (
                        <button onClick={fetchHint} disabled={hintBusy} className="btn-primary w-full !py-2 !text-sm">
                          {hintBusy
                            ? 'Thinking…'
                            : hints.length === 0
                              ? 'Get a hint'
                              : `Reveal level ${nextHintLevel} hint`}
                        </button>
                      ) : (
                        <p className="text-center text-xs text-zinc-500">
                          That’s all three hint levels — you’ve got this. 🚀
                        </p>
                      )}
                    </div>
                  </>
                )}
              </div>
            </motion.aside>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
