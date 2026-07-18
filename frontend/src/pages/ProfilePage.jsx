import { useEffect, useMemo, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  fetchProfile,
  fetchActivity,
  fetchUserSubmissions,
  fetchUserContests,
  updateBio,
  changePassword,
} from '../api/client.js'
import { useUser } from '../context/UserContext.jsx'
import { ratingTier } from '../lib/rating.js'

const DIFF = {
  easy: { label: 'Easy', color: '#34d399' },
  medium: { label: 'Medium', color: '#fbbf24' },
  hard: { label: 'Hard', color: '#fb7185' },
}
const VERDICT_COLOR = {
  ACCEPTED: '#34d399',
  WRONG_ANSWER: '#fb7185',
  TIME_LIMIT_EXCEEDED: '#fbbf24',
  MEMORY_LIMIT_EXCEEDED: '#fbbf24',
  RUNTIME_ERROR: '#fb7185',
  COMPILATION_ERROR: '#fb7185',
  INTERNAL_ERROR: '#9ca3af',
}
const STATUS = { RUNNING: 'Live', UPCOMING: 'Upcoming', ENDED: 'Ended' }

function isoLocal(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** GitHub/LeetCode-style contribution heatmap for the past year. */
function Heatmap({ activity }) {
  const { weeks, total, months } = useMemo(() => {
    const counts = new Map((activity ?? []).map((a) => [a.date, a.count]))
    const end = new Date()
    end.setHours(0, 0, 0, 0)
    const start = new Date(end)
    start.setDate(end.getDate() - 363)
    const cells = []
    for (let i = 0; i < start.getDay(); i++) cells.push(null)
    const d = new Date(start)
    while (d <= end) {
      const key = isoLocal(d)
      cells.push({ date: key, count: counts.get(key) ?? 0 })
      d.setDate(d.getDate() + 1)
    }
    const wk = []
    for (let i = 0; i < cells.length; i += 7) wk.push(cells.slice(i, i + 7))
    const total = (activity ?? []).reduce((s, a) => s + a.count, 0)
    // month labels: first week where a new month starts
    const mLabels = []
    let lastMonth = -1
    wk.forEach((week, wi) => {
      const first = week.find((c) => c)
      if (first) {
        const mo = new Date(first.date).getMonth()
        if (mo !== lastMonth) {
          mLabels.push({ wi, label: new Date(first.date).toLocaleString('en', { month: 'short' }) })
          lastMonth = mo
        }
      }
    })
    return { weeks: wk, total, months: mLabels }
  }, [activity])

  const level = (c) => {
    if (!c) return 'rgba(255,255,255,0.05)'
    if (c >= 8) return '#22d3ee'
    if (c >= 5) return '#0ea5b7'
    if (c >= 3) return '#0d7490'
    return '#134e5a'
  }

  return (
    <div className="rounded-2xl glass p-5">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="font-display text-sm font-semibold uppercase tracking-wider text-brand-400">Activity</h2>
        <span className="text-xs text-zinc-500">{total} submissions in the past year</span>
      </div>
      <div className="overflow-x-auto">
        <div className="inline-flex flex-col gap-1">
          <div className="flex gap-[3px] pl-0 text-[9px] text-zinc-600">
            {weeks.map((_, wi) => {
              const m = months.find((x) => x.wi === wi)
              return (
                <div key={wi} className="w-[11px]">{m ? m.label : ''}</div>
              )
            })}
          </div>
          <div className="flex gap-[3px]">
            {weeks.map((week, wi) => (
              <div key={wi} className="flex flex-col gap-[3px]">
                {Array.from({ length: 7 }).map((_, di) => {
                  const cell = week[di]
                  return (
                    <div
                      key={di}
                      title={cell ? `${cell.count} on ${cell.date}` : ''}
                      className="h-[11px] w-[11px] rounded-[2px]"
                      style={{ backgroundColor: cell ? level(cell.count) : 'transparent' }}
                    />
                  )
                })}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

function SolvedRing({ solved, total }) {
  const pct = total ? solved / total : 0
  const r = 52
  const c = 2 * Math.PI * r
  return (
    <div className="relative grid h-32 w-32 place-items-center">
      <svg className="h-32 w-32 -rotate-90" viewBox="0 0 120 120">
        <circle cx="60" cy="60" r={r} fill="none" stroke="rgba(255,255,255,0.08)" strokeWidth="9" />
        <motion.circle
          cx="60" cy="60" r={r} fill="none" stroke="url(#grad)" strokeWidth="9" strokeLinecap="round"
          strokeDasharray={c}
          initial={{ strokeDashoffset: c }}
          animate={{ strokeDashoffset: c - c * pct }}
          transition={{ duration: 0.9, ease: [0.22, 1, 0.36, 1] }}
        />
        <defs>
          <linearGradient id="grad" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#22d3ee" />
            <stop offset="100%" stopColor="#8b5cf6" />
          </linearGradient>
        </defs>
      </svg>
      <div className="absolute text-center">
        <div className="font-display text-2xl font-bold text-white">{solved}</div>
        <div className="text-[11px] text-zinc-500">/ {total} solved</div>
      </div>
    </div>
  )
}

function DiffBar({ label, color, solved, total }) {
  const pct = total ? (solved / total) * 100 : 0
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-xs">
        <span style={{ color }}>{label}</span>
        <span className="text-zinc-400">{solved} / {total}</span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-white/[0.06]">
        <motion.div
          className="h-full rounded-full"
          style={{ backgroundColor: color }}
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ duration: 0.8 }}
        />
      </div>
    </div>
  )
}

function StatCard({ label, value, sub }) {
  return (
    <div className="rounded-2xl glass p-5 text-center">
      <div className="font-display text-3xl font-bold text-white">{value}</div>
      <div className="mt-1 text-sm text-zinc-400">{label}</div>
      {sub && <div className="text-xs text-zinc-600">{sub}</div>}
    </div>
  )
}

function Detail({ label, value }) {
  return (
    <div>
      <div className="text-xs text-zinc-500">{label}</div>
      <div className="mt-0.5 break-all text-sm text-zinc-200">{value}</div>
    </div>
  )
}

/** Admin profiles show platform info instead of problem-solving stats. */
function AdminPanel({ profile }) {
  const totalProblems = profile.totalEasy + profile.totalMedium + profile.totalHard
  return (
    <div className="mb-6 space-y-6">
      <div className="rounded-2xl glass p-6">
        <h2 className="mb-4 font-display text-sm font-semibold uppercase tracking-wider text-brand-400">Administrator</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <Detail label="Email" value={profile.email || '—'} />
          <Detail label="Member since" value={profile.memberSince || '—'} />
          <Detail label="Role" value="ADMIN" />
        </div>
      </div>
      <div className="grid grid-cols-3 gap-6">
        <StatCard label="Problems" value={totalProblems} />
        <StatCard label="Contests" value={profile.totalContests} />
        <StatCard label="Users" value={profile.totalUsers} />
      </div>
      <Link to="/admin" className="btn-primary inline-flex">Open admin panel →</Link>
    </div>
  )
}

function EditModal({ initialBio, onClose, onSaved }) {
  const { applyAuth } = useUser()
  const [bio, setBio] = useState(initialBio ?? '')
  const [curPw, setCurPw] = useState('')
  const [newPw, setNewPw] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [msg, setMsg] = useState('')

  async function saveBio() {
    setBusy(true); setErr(''); setMsg('')
    try {
      const u = await updateBio(bio)
      applyAuth(u)
      onSaved(bio)
      setMsg('Bio saved.')
    } catch (e) {
      setErr(e?.response?.data?.message ?? 'Could not save bio.')
    } finally {
      setBusy(false)
    }
  }

  async function savePassword() {
    if (newPw.length < 4) { setErr('New password must be at least 4 characters.'); return }
    setBusy(true); setErr(''); setMsg('')
    try {
      const u = await changePassword(curPw, newPw)
      applyAuth(u)
      setCurPw(''); setNewPw('')
      setMsg('Password changed.')
    } catch (e) {
      setErr(e?.response?.data?.message ?? 'Could not change password.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <motion.div
      className="fixed inset-0 z-50 grid place-items-center bg-black/60 p-4 backdrop-blur-sm"
      initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        className="w-full max-w-md rounded-2xl glass p-6"
        initial={{ scale: 0.95, y: 10 }} animate={{ scale: 1, y: 0 }} exit={{ scale: 0.95, y: 10 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h3 className="font-display text-lg font-bold text-white">Edit profile</h3>
          <button onClick={onClose} className="text-zinc-500 hover:text-white">✕</button>
        </div>

        {err && <p className="mb-3 rounded-lg bg-accent-rose/10 px-3 py-2 text-xs text-accent-rose">{err}</p>}
        {msg && <p className="mb-3 rounded-lg bg-accent-emerald/10 px-3 py-2 text-xs text-accent-emerald">{msg}</p>}

        <label className="mb-1 block text-xs font-semibold uppercase tracking-wider text-zinc-500">Bio</label>
        <textarea
          value={bio}
          onChange={(e) => setBio(e.target.value.slice(0, 280))}
          rows={3}
          placeholder="Tell the arena about yourself…"
          className="w-full resize-none rounded-lg border border-white/10 bg-white/[0.04] px-3 py-2 text-sm text-zinc-100 outline-none focus:border-brand-400"
        />
        <div className="mb-4 mt-1 flex items-center justify-between">
          <span className="text-[10px] text-zinc-600">{bio.length}/280</span>
          <button onClick={saveBio} disabled={busy} className="btn-primary !py-1.5 !text-xs">Save bio</button>
        </div>

        <div className="my-4 border-t border-white/5" />

        <label className="mb-1 block text-xs font-semibold uppercase tracking-wider text-zinc-500">Change password</label>
        <input
          type="password" value={curPw} onChange={(e) => setCurPw(e.target.value)}
          placeholder="Current password"
          className="mb-2 w-full rounded-lg border border-white/10 bg-white/[0.04] px-3 py-2 text-sm text-zinc-100 outline-none focus:border-brand-400"
        />
        <input
          type="password" value={newPw} onChange={(e) => setNewPw(e.target.value)}
          placeholder="New password"
          className="w-full rounded-lg border border-white/10 bg-white/[0.04] px-3 py-2 text-sm text-zinc-100 outline-none focus:border-brand-400"
        />
        <div className="mt-2 flex justify-end">
          <button onClick={savePassword} disabled={busy || !curPw || !newPw} className="btn-primary !py-1.5 !text-xs">Update password</button>
        </div>
      </motion.div>
    </motion.div>
  )
}

export default function ProfilePage() {
  const { username } = useParams()
  const { user } = useUser()
  const [profile, setProfile] = useState(null)
  const [activity, setActivity] = useState([])
  const [subs, setSubs] = useState([])
  const [contests, setContests] = useState([])
  const [status, setStatus] = useState('loading')
  const [editing, setEditing] = useState(false)

  useEffect(() => {
    setStatus('loading')
    Promise.all([
      fetchProfile(username),
      fetchActivity(username).catch(() => []),
      fetchUserSubmissions(username).catch(() => []),
      fetchUserContests(username).catch(() => []),
    ])
      .then(([p, a, s, c]) => {
        setProfile(p)
        setActivity(a)
        setSubs(s)
        setContests(c)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }, [username])

  if (status === 'loading') return <div className="py-24 text-center text-sm text-zinc-500">Loading profile…</div>
  if (status === 'error' || !profile) {
    return (
      <div className="py-24 text-center">
        <p className="text-accent-rose">⚠ User not found.</p>
        <Link to="/leaderboard" className="btn-ghost mt-6 inline-flex">← Leaderboard</Link>
      </div>
    )
  }

  const tier = ratingTier(profile.rating)
  const totalSolved = profile.solvedEasy + profile.solvedMedium + profile.solvedHard
  const totalProblems = profile.totalEasy + profile.totalMedium + profile.totalHard
  const acceptance = profile.totalSubmissions
    ? Math.round((profile.acceptedSubmissions / profile.totalSubmissions) * 100)
    : 0
  const isMe = user && user.username === profile.username
  const isAdmin = profile.role === 'ADMIN'

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="mx-auto max-w-5xl px-6 py-10">
      {/* Header */}
      <div className="mb-6 flex flex-wrap items-center gap-5 rounded-2xl glass p-6">
        <div
          className="grid h-20 w-20 place-items-center rounded-2xl text-3xl font-bold"
          style={{ color: tier.color, backgroundColor: `${tier.color}1a`, border: `1px solid ${tier.color}33` }}
        >
          {profile.username.charAt(0).toUpperCase()}
        </div>
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <h1 className="font-display text-2xl font-bold text-white">{profile.username}</h1>
            {isMe && <span className="chip !py-0 text-[10px] text-zinc-300">you</span>}
            {isAdmin && (
              <span
                className="chip !py-0 text-[10px]"
                style={{ color: '#a78bfa', backgroundColor: '#a78bfa1a', borderColor: '#a78bfa33' }}
              >
                ★ ADMIN
              </span>
            )}
          </div>
          {isAdmin ? (
            <div className="mt-1 text-sm font-medium text-brand-300">Platform Administrator</div>
          ) : (
            <>
              <div className="mt-1 text-sm" style={{ color: tier.color }}>{tier.label}</div>
              <div className="mt-1 text-xs text-zinc-500">
                Rank <span className="font-semibold text-zinc-300">#{profile.rank}</span> of {profile.totalUsers}
              </div>
            </>
          )}
          {profile.bio
            ? <p className="mt-2 max-w-xl text-sm text-zinc-400">{profile.bio}</p>
            : isMe && <p className="mt-2 text-sm italic text-zinc-600">No bio yet — add one.</p>}
        </div>
        <div className="flex flex-col items-end gap-2">
          {!isAdmin && (
            <div className="text-center">
              <div className="font-display text-3xl font-bold" style={{ color: tier.color }}>{profile.rating}</div>
              <div className="text-xs text-zinc-500">rating</div>
            </div>
          )}
          {isMe && (
            <button onClick={() => setEditing(true)} className="btn-ghost !py-1.5 !text-xs">Edit profile</button>
          )}
        </div>
      </div>

      {isAdmin && <AdminPanel profile={profile} />}

      {!isAdmin && (
      <>
      {/* Stats row */}
      <div className="mb-6 grid gap-6 lg:grid-cols-[1.3fr_1fr]">
        <div className="flex items-center gap-6 rounded-2xl glass p-6">
          <SolvedRing solved={totalSolved} total={totalProblems} />
          <div className="flex-1 space-y-3">
            <DiffBar label="Easy" color={DIFF.easy.color} solved={profile.solvedEasy} total={profile.totalEasy} />
            <DiffBar label="Medium" color={DIFF.medium.color} solved={profile.solvedMedium} total={profile.totalMedium} />
            <DiffBar label="Hard" color={DIFF.hard.color} solved={profile.solvedHard} total={profile.totalHard} />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-6">
          <StatCard label="Acceptance" value={`${acceptance}%`} sub={`${profile.acceptedSubmissions}/${profile.totalSubmissions}`} />
          <StatCard label="Submissions" value={profile.totalSubmissions} />
          <StatCard label="Accepted" value={profile.acceptedSubmissions} />
          <StatCard label="Contests" value={contests.length} />
        </div>
      </div>

      {/* Heatmap */}
      <div className="mb-6">
        <Heatmap activity={activity} />
      </div>

      {/* History */}
      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl glass p-5">
          <h2 className="mb-3 font-display text-sm font-semibold uppercase tracking-wider text-brand-400">Recent submissions</h2>
          {subs.length === 0 ? (
            <p className="py-6 text-center text-sm text-zinc-500">No submissions yet.</p>
          ) : (
            <div className="divide-y divide-white/5">
              {subs.slice(0, 15).map((s) => (
                <div key={s.id} className="flex items-center justify-between py-2.5 text-sm">
                  <Link to={`/problems/${s.problemSlug}`} className="truncate text-zinc-200 hover:text-white">{s.problemTitle}</Link>
                  <div className="flex items-center gap-3">
                    <span className="text-xs text-zinc-600">{s.language.toLowerCase()}</span>
                    <span className="text-xs font-semibold" style={{ color: VERDICT_COLOR[s.verdict] ?? '#9ca3af' }}>
                      {s.verdict === 'ACCEPTED' ? '✓' : '✕'} {s.passedTests}/{s.totalTests}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="rounded-2xl glass p-5">
          <h2 className="mb-3 font-display text-sm font-semibold uppercase tracking-wider text-brand-400">Contest history</h2>
          {contests.length === 0 ? (
            <p className="py-6 text-center text-sm text-zinc-500">No contests yet.</p>
          ) : (
            <div className="divide-y divide-white/5">
              {contests.map((c) => (
                <div key={c.contestId} className="flex items-center justify-between py-2.5 text-sm">
                  <Link to={`/contests/${c.contestId}`} className="truncate text-zinc-200 hover:text-white">
                    {c.name}<span className="ml-2 text-xs text-zinc-600">{STATUS[c.status] ?? c.status}</span>
                  </Link>
                  <div className="flex items-center gap-3 text-xs">
                    <span className="text-zinc-400">{c.solvedCount} solved</span>
                    {c.ratingDelta != null && (
                      <span className={c.ratingDelta >= 0 ? 'text-accent-emerald' : 'text-accent-rose'}>
                        {c.ratingDelta >= 0 ? '+' : ''}{c.ratingDelta}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
      </>
      )}

      <AnimatePresence>
        {editing && (
          <EditModal
            initialBio={profile.bio}
            onClose={() => setEditing(false)}
            onSaved={(bio) => setProfile((p) => ({ ...p, bio }))}
          />
        )}
      </AnimatePresence>
    </motion.div>
  )
}
