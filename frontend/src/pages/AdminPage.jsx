import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import {
  adminCreateProblem,
  adminUpdateProblem,
  adminDeleteProblem,
  adminFetchProblem,
  adminCreateContest,
  fetchProblems,
} from '../api/client.js'
import { useUser } from '../context/UserContext.jsx'

const emptyCase = () => ({ input: '', expectedOutput: '', sample: true })

// Downloadable template + shape expected when an admin uploads a test-case file.
const TESTCASE_TEMPLATE = {
  testcases: [
    { input: '2 3', expectedOutput: '5', sample: true },
    { input: '10 20', expectedOutput: '30', sample: true },
    { input: '-5 5', expectedOutput: '0', sample: false },
    { input: '1000000000 1000000000', expectedOutput: '2000000000', sample: false },
  ],
}

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

const inputClass =
  'w-full rounded-lg border border-white/10 bg-ink-800 px-3 py-2 text-sm text-white outline-none focus:border-brand-500'

function ProblemForm() {
  const emptyForm = { slug: '', title: '', difficulty: 'EASY', tags: '', timeLimitMs: 2000, memoryLimitMb: 256, description: '' }
  const [form, setForm] = useState(emptyForm)
  const [cases, setCases] = useState([emptyCase()])
  const [notice, setNotice] = useState(null)
  const [busy, setBusy] = useState(false)
  const [editSlug, setEditSlug] = useState('')
  const [problems, setProblems] = useState([])

  const refreshProblems = () => fetchProblems().then(setProblems).catch(() => {})
  useEffect(() => { refreshProblems() }, [])

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }))
  const setCase = (i, k, v) => setCases((cs) => cs.map((c, j) => (j === i ? { ...c, [k]: v } : c)))

  function resetToCreate() {
    setEditSlug('')
    setForm(emptyForm)
    setCases([emptyCase()])
    setNotice(null)
  }

  async function onSelectProblem(slug) {
    if (!slug) { resetToCreate(); return }
    setBusy(true)
    setNotice(null)
    try {
      const p = await adminFetchProblem(slug)
      setEditSlug(slug)
      setForm({
        slug: p.slug,
        title: p.title,
        difficulty: p.difficulty,
        tags: (p.tags ?? []).join(', '),
        timeLimitMs: p.timeLimitMs,
        memoryLimitMb: p.memoryLimitMb,
        description: p.description,
      })
      setCases(
        p.testcases?.length
          ? p.testcases.map((c) => ({ input: c.input, expectedOutput: c.expectedOutput, sample: !!c.sample }))
          : [emptyCase()],
      )
    } catch (err) {
      setNotice({ type: 'err', text: err?.response?.data?.message ?? 'Failed to load problem.' })
    } finally {
      setBusy(false)
    }
  }

  async function onDelete() {
    if (!editSlug) return
    if (!window.confirm(`Delete problem "${editSlug}"? This also removes its submissions.`)) return
    setBusy(true)
    setNotice(null)
    try {
      await adminDeleteProblem(editSlug)
      resetToCreate()
      setNotice({ type: 'ok', text: 'Problem deleted.' })
      refreshProblems()
    } catch (err) {
      setNotice({ type: 'err', text: err?.response?.data?.message ?? 'Failed to delete problem.' })
    } finally {
      setBusy(false)
    }
  }

  function downloadTemplate() {
    const blob = new Blob([JSON.stringify(TESTCASE_TEMPLATE, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'testcases-template.json'
    a.click()
    URL.revokeObjectURL(url)
  }

  async function onUploadJson(e) {
    const file = e.target.files?.[0]
    e.target.value = '' // let the same file be re-selected later
    if (!file) return
    try {
      const parsed = JSON.parse(await file.text())
      const arr = Array.isArray(parsed) ? parsed : parsed?.testcases
      if (!Array.isArray(arr) || arr.length === 0) {
        throw new Error('expected a "testcases" array')
      }
      const mapped = arr.map((c, i) => {
        if (c?.input == null || c?.expectedOutput == null) {
          throw new Error(`case ${i + 1} is missing "input" or "expectedOutput"`)
        }
        return { input: String(c.input), expectedOutput: String(c.expectedOutput), sample: !!c.sample }
      })
      setCases(mapped)
      const samples = mapped.filter((c) => c.sample).length
      setNotice({
        type: 'ok',
        text: `Loaded ${mapped.length} test case(s): ${samples} sample, ${mapped.length - samples} hidden.`,
      })
    } catch (err) {
      setNotice({ type: 'err', text: `Could not read JSON — ${err.message}.` })
    }
  }

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
      if (editSlug) {
        await adminUpdateProblem(editSlug, payload)
        setEditSlug(form.slug) // slug may have been renamed
        setNotice({ type: 'ok', text: `Problem "${form.slug}" saved.` })
      } else {
        await adminCreateProblem(payload)
        resetToCreate()
        setNotice({ type: 'ok', text: `Problem "${payload.slug}" created.` })
      }
      refreshProblems()
    } catch (err) {
      setNotice({
        type: 'err',
        text: err?.response?.data?.message ?? (editSlug ? 'Failed to save problem.' : 'Failed to create problem.'),
      })
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4 rounded-2xl glass p-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-display text-lg font-semibold text-white">
          {editSlug ? 'Edit problem' : 'Create problem'}
        </h2>
        <select
          className={`${inputClass} max-w-xs`}
          value={editSlug}
          onChange={(e) => onSelectProblem(e.target.value)}
        >
          <option value="">+ New problem</option>
          {problems.map((p) => (
            <option key={p.slug} value={p.slug}>Edit: {p.title}</option>
          ))}
        </select>
      </div>
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
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-sm font-semibold text-brand-400">Test cases</h3>
          <div className="flex items-center gap-2">
            <button type="button" onClick={downloadTemplate} className="btn-ghost !py-1 !text-xs">
              ⤓ Template
            </button>
            <label className="btn-ghost cursor-pointer !py-1 !text-xs">
              ↑ Upload JSON
              <input type="file" accept=".json,application/json" onChange={onUploadJson} className="hidden" />
            </label>
            <button type="button" onClick={() => setCases((cs) => [...cs, emptyCase()])} className="btn-ghost !py-1 !text-xs">
              + Add case
            </button>
          </div>
        </div>
        <p className="text-xs text-zinc-500">
          Add a few <span className="text-zinc-300">sample</span> cases (shown to users) plus any number of
          hidden cases. Bulk-import them by uploading a JSON file — download the template to see the format.
          Uploading replaces the cases below.
        </p>
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
      <div className="flex flex-wrap items-center gap-3">
        <button disabled={busy} className="btn-primary">
          {busy ? 'Saving…' : editSlug ? 'Save changes' : 'Create problem'}
        </button>
        {editSlug && (
          <>
            <button type="button" onClick={resetToCreate} disabled={busy} className="btn-ghost">Cancel</button>
            <button type="button" onClick={onDelete} disabled={busy} className="ml-auto text-sm text-accent-rose hover:underline">
              Delete problem
            </button>
          </>
        )}
      </div>
    </form>
  )
}

function ContestForm() {
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
      await adminCreateContest(payload)
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
  const { user, isAdmin, ready } = useUser()
  const [tab, setTab] = useState('problem')

  if (!ready) return <div className="py-24 text-center text-sm text-zinc-500">Checking access…</div>

  if (!isAdmin) {
    return (
      <div className="mx-auto max-w-sm px-6 py-24 text-center">
        <h1 className="font-display text-xl font-bold text-white">Admin only</h1>
        <p className="mt-2 text-sm text-zinc-400">
          {user
            ? 'Your account does not have admin access.'
            : 'Sign in with an admin account to manage problems and contests.'}
        </p>
      </div>
    )
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mx-auto max-w-4xl px-6 py-10">
      <div className="mb-6">
        <h1 className="font-display text-3xl font-bold tracking-tight text-white">Admin</h1>
        <p className="mt-1 text-sm text-zinc-500">Create practice problems and contests.</p>
      </div>

      <div className="mb-6 inline-flex gap-1 rounded-lg bg-white/[0.03] p-1">
        <button onClick={() => setTab('problem')} className={`rounded-md px-4 py-1.5 text-sm font-medium ${tab === 'problem' ? 'bg-white/10 text-white' : 'text-zinc-400'}`}>Problem</button>
        <button onClick={() => setTab('contest')} className={`rounded-md px-4 py-1.5 text-sm font-medium ${tab === 'contest' ? 'bg-white/10 text-white' : 'text-zinc-400'}`}>Contest</button>
      </div>

      {tab === 'problem' ? <ProblemForm /> : <ContestForm />}
    </motion.div>
  )
}
