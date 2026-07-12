import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import {
  verifyAdminKey,
  adminCreateProblem,
  adminCreateContest,
  fetchProblems,
} from '../api/client.js'
import { useUser } from '../context/UserContext.jsx'

const KEY_STORAGE = 'codearena.adminKey'
const emptyCase = () => ({ input: '', expectedOutput: '', sample: true })

function Notice({ notice }) {
  if (!notice) return null
  const ok = notice.type === 'ok'
  return (
    <div
      className={`rounded-lg border p-3 text-sm ${
        ok ? 'border-accent-emerald/30 text-accent-emerald' : 'border-accent-rose/30 text-accent-rose'
      }`}
    >
      {notice.text}
    </div>
  )
}

function KeyGate({ onUnlock }) {
  const [key, setKey] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await verifyAdminKey(key)
      localStorage.setItem(KEY_STORAGE, key)
      onUnlock(key)
    } catch {
      setError('Invalid admin key.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-sm px-6 py-24">
      <div className="border-gradient rounded-2xl glass-strong p-7">
        <h1 className="font-display text-xl font-bold text-white">Admin access</h1>
        <p className="mt-1 text-sm text-zinc-400">Enter the admin key to manage problems and contests.</p>
        <form onSubmit={submit} className="mt-5 space-y-3">
          <input
            type="password"
            value={key}
            onChange={(e) => setKey(e.target.value)}
            placeholder="Admin key"
            className="w-full rounded-lg border border-white/10 bg-ink-800 px-3 py-2.5 text-sm text-white outline-none focus:border-brand-500"
          />
          {error && <p className="text-xs text-accent-rose">{error}</p>}
          <button disabled={busy} className="btn-primary w-full justify-center">
            {busy ? 'Verifying…' : 'Unlock'}
          </button>
        </form>
      </div>
    </div>
  )
}

const inputClass =
  'w-full rounded-lg border border-white/10 bg-ink-800 px-3 py-2 text-sm text-white outline-none focus:border-brand-500'

function ProblemForm({ adminKey }) {
  const [form, setForm] = useState({
    slug: '',
    title: '',
    difficulty: 'EASY',
    tags: '',
    timeLimitMs: 2000,
    memoryLimitMb: 256,
    description: '',
  })
  const [cases, setCases] = useState([emptyCase()])
  const [notice, setNotice] = useState(null)
  const [busy, setBusy] = useState(false)

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }))
  const setCase = (i, k, v) => setCases((cs) => cs.map((c, j) => (j === i ? { ...c, [k]: v } : c)))

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setNotice(null)
    try {
      const payload = {
        ...form,
        timeLimitMs: Number(form.timeLimitMs),
        memoryLimitMb: Number(form.memoryLimitMb),
        tags: form.tags.split(',').map((t) => t.trim()).filter(Boolean),
        testcases: cases,
      }
      await adminCreateProblem(adminKey, payload)
      setNotice({ type: 'ok', text: `Problem "${form.slug}" created.` })
      setForm({ slug: '', title: '', difficulty: 'EASY', tags: '', timeLimitMs: 2000, memoryLimitMb: 256, description: '' })
      setCases([emptyCase()])
    } catch (err) {
      setNotice({ type: 'err', text: err?.response?.data?.message ?? 'Failed to create problem.' })
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-2xl glass p-6">
      <h2 className="font-display text-lg font-semibold text-white">Create problem</h2>
      <div className="grid gap-3 sm:grid-cols-2">
        <input className={inputClass} placeholder="slug (e.g. two-sum)" value={form.slug} onChange={(e) => set('slug', e.target.value)} required />
        <input className={inputClass} placeholder="Title" value={form.title} onChange={(e) => set('title', e.target.value)} required />
        <select className={inputClass} value={form.difficulty} onChange={(e) => set('difficulty', e.target.value)}>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </select>
        <input className={inputClass} placeholder="tags (comma separated)" value={form.tags} onChange={(e) => set('tags', e.target.value)} />
        <input className={inputClass} type="number" placeholder="Time limit (ms)" value={form.timeLimitMs} onChange={(e) => set('timeLimitMs', e.target.value)} />
        <input className={inputClass} type="number" placeholder="Memory (MB)" value={form.memoryLimitMb} onChange={(e) => set('memoryLimitMb', e.target.value)} />
      </div>
      <textarea
        className={`${inputClass} min-h-[120px] font-mono`}
        placeholder="Description (supports ### headings, `code`, and ``` fenced blocks)"
        value={form.description}
        onChange={(e) => set('description', e.target.value)}
        required
      />

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-semibold text-brand-400">Test cases</h3>
          <button type="button" onClick={() => setCases((cs) => [...cs, emptyCase()])} className="btn-ghost !py-1 !text-xs">
            + Add case
          </button>
        </div>
        {cases.map((c, i) => (
          <div key={i} className="rounded-lg border border-white/5 bg-white/[0.02] p-3">
            <div className="mb-2 flex items-center justify-between">
              <span className="text-xs text-zinc-500">Case {i + 1}</span>
              <div className="flex items-center gap-3">
                <label className="flex items-center gap-1.5 text-xs text-zinc-400">
                  <input type="checkbox" checked={c.sample} onChange={(e) => setCase(i, 'sample', e.target.checked)} />
                  sample (visible)
                </label>
                {cases.length > 1 && (
                  <button type="button" onClick={() => setCases((cs) => cs.filter((_, j) => j !== i))} className="text-xs text-accent-rose">
                    remove
                  </button>
                )}
              </div>
            </div>
            <div className="grid gap-2 sm:grid-cols-2">
              <textarea className={`${inputClass} font-mono`} placeholder="Input (stdin)" value={c.input} onChange={(e) => setCase(i, 'input', e.target.value)} />
              <textarea className={`${inputClass} font-mono`} placeholder="Expected output (stdout)" value={c.expectedOutput} onChange={(e) => setCase(i, 'expectedOutput', e.target.value)} />
            </div>
          </div>
        ))}
      </div>

      <Notice notice={notice} />
      <button disabled={busy} className="btn-primary">{busy ? 'Creating…' : 'Create problem'}</button>
    </form>
  )
}

function ContestForm({ adminKey }) {
  const [problems, setProblems] = useState([])
  const [form, setForm] = useState({ name: '', description: '', start: '', end: '' })
  const [selected, setSelected] = useState(new Set())
  const [notice, setNotice] = useState(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    fetchProblems().then(setProblems).catch(() => {})
  }, [])

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }))
  const toggle = (slug) =>
    setSelected((s) => {
      const n = new Set(s)
      n.has(slug) ? n.delete(slug) : n.add(slug)
      return n
    })

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setNotice(null)
    try {
      const payload = {
        name: form.name,
        description: form.description,
        startTime: new Date(form.start).toISOString(),
        endTime: new Date(form.end).toISOString(),
        problemSlugs: [...selected],
      }
      await adminCreateContest(adminKey, payload)
      setNotice({ type: 'ok', text: `Contest "${form.name}" created.` })
      setForm({ name: '', description: '', start: '', end: '' })
      setSelected(new Set())
    } catch (err) {
      setNotice({ type: 'err', text: err?.response?.data?.message ?? 'Failed to create contest.' })
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-2xl glass p-6">
      <h2 className="font-display text-lg font-semibold text-white">Create contest</h2>
      <input className={inputClass} placeholder="Contest name" value={form.name} onChange={(e) => set('name', e.target.value)} required />
      <textarea className={`${inputClass} min-h-[70px]`} placeholder="Description" value={form.description} onChange={(e) => set('description', e.target.value)} />
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="text-xs text-zinc-500">
          Start
          <input className={inputClass} type="datetime-local" value={form.start} onChange={(e) => set('start', e.target.value)} required />
        </label>
        <label className="text-xs text-zinc-500">
          End
          <input className={inputClass} type="datetime-local" value={form.end} onChange={(e) => set('end', e.target.value)} required />
        </label>
      </div>
      <div>
        <h3 className="mb-2 text-sm font-semibold text-brand-400">Problems ({selected.size} selected)</h3>
        <div className="grid max-h-52 grid-cols-1 gap-1.5 overflow-y-auto sm:grid-cols-2">
          {problems.map((p) => (
            <label key={p.slug} className="flex items-center gap-2 rounded-lg border border-white/5 bg-white/[0.02] px-3 py-2 text-sm text-zinc-300">
              <input type="checkbox" checked={selected.has(p.slug)} onChange={() => toggle(p.slug)} />
              {p.title}
            </label>
          ))}
        </div>
      </div>
      <Notice notice={notice} />
      <button disabled={busy} className="btn-primary">{busy ? 'Creating…' : 'Create contest'}</button>
    </form>
  )
}

export default function AdminPage() {
  const { isAdmin } = useUser()
  const [adminKey, setAdminKey] = useState(null)
  const [checking, setChecking] = useState(true)
  const [tab, setTab] = useState('problem')

  useEffect(() => {
    const saved = localStorage.getItem(KEY_STORAGE)
    if (!saved) {
      setChecking(false)
      return
    }
    verifyAdminKey(saved)
      .then(() => setAdminKey(saved))
      .catch(() => localStorage.removeItem(KEY_STORAGE))
      .finally(() => setChecking(false))
  }, [])

  if (checking) return <div className="py-24 text-center text-sm text-zinc-500">Checking access…</div>

  const unlocked = isAdmin || !!adminKey
  if (!unlocked) return <KeyGate onUnlock={setAdminKey} />

  // Admins authorize via their session token; key users via the stored key.
  const effectiveKey = adminKey ?? ''

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mx-auto max-w-4xl px-6 py-10">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="font-display text-3xl font-bold tracking-tight text-white">Admin</h1>
          <p className="mt-1 text-sm text-zinc-500">Create practice problems and contests.</p>
        </div>
        {!isAdmin && (
          <button
            onClick={() => {
              localStorage.removeItem(KEY_STORAGE)
              setAdminKey(null)
            }}
            className="btn-ghost !py-1.5 !text-sm"
          >
            Lock
          </button>
        )}
      </div>

      <div className="mb-6 inline-flex gap-1 rounded-lg bg-white/[0.03] p-1">
        <button onClick={() => setTab('problem')} className={`rounded-md px-4 py-1.5 text-sm font-medium ${tab === 'problem' ? 'bg-white/10 text-white' : 'text-zinc-400'}`}>Problem</button>
        <button onClick={() => setTab('contest')} className={`rounded-md px-4 py-1.5 text-sm font-medium ${tab === 'contest' ? 'bg-white/10 text-white' : 'text-zinc-400'}`}>Contest</button>
      </div>

      {tab === 'problem' ? <ProblemForm adminKey={effectiveKey} /> : <ContestForm adminKey={effectiveKey} />}
    </motion.div>
  )
}
