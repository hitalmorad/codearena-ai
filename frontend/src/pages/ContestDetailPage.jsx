import { useEffect, useMemo, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { fetchContest, fetchStandings, joinContest, subscribeStream } from '../api/client.js'
import { useUser } from '../context/UserContext.jsx'
import { ratingTier } from '../lib/rating.js'

const DIFF = {
  EASY: { label: 'Easy', color: '#34d399' },
  MEDIUM: { label: 'Medium', color: '#fbbf24' },
  HARD: { label: 'Hard', color: '#fb7185' },
}
const STATUS = {
  RUNNING: { label: 'Live', color: '#34d399' },
  UPCOMING: { label: 'Upcoming', color: '#fbbf24' },
  ENDED: { label: 'Ended', color: '#9ca3af' },
}
const MEDALS = { 1: '🥇', 2: '🥈', 3: '🥉' }

function Countdown({ end }) {
  const [now, setNow] = useState(Date.now())
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(t)
  }, [])
  const ms = Math.max(0, new Date(end).getTime() - now)
  const h = Math.floor(ms / 3_600_000)
  const m = Math.floor((ms % 3_600_000) / 60_000)
  const s = Math.floor((ms % 60_000) / 1000)
  return (
    <span className="font-mono text-sm text-accent-emerald">
      {String(h).padStart(2, '0')}:{String(m).padStart(2, '0')}:{String(s).padStart(2, '0')} left
    </span>
  )
}

export default function ContestDetailPage() {
  const { id } = useParams()
  const { user } = useUser()
  const [contest, setContest] = useState(null)
  const [standings, setStandings] = useState([])
  const [status, setStatus] = useState('loading')
  const [joining, setJoining] = useState(false)

  const load = () => fetchContest(id, user?.username)

  useEffect(() => {
    Promise.all([load(), fetchStandings(id)])
      .then(([c, s]) => {
        setContest(c)
        setStandings(s)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))

    const source = subscribeStream(`/contests/${id}/standings/stream`, setStandings)
    return () => source.close()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, user?.username])

  const mySolved = useMemo(() => {
    const row = standings.find((r) => user && r.username === user.username)
    return new Set(row?.solvedSlugs ?? [])
  }, [standings, user])

  async function onJoin() {
    if (!user) return
    setJoining(true)
    try {
      await joinContest(id, user.username)
      setContest(await load())
    } finally {
      setJoining(false)
    }
  }

  if (status === 'loading') {
    return <div className="py-24 text-center text-sm text-zinc-500">Loading contest…</div>
  }
  if (status === 'error' || !contest) {
    return (
      <div className="py-24 text-center">
        <p className="text-accent-rose">⚠ Could not load this contest.</p>
        <Link to="/contests" className="btn-ghost mt-6 inline-flex">← Back to contests</Link>
      </div>
    )
  }

  const st = STATUS[contest.status] ?? STATUS.ENDED
  const ended = contest.status === 'ENDED'

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="mx-auto max-w-5xl px-6 py-10"
    >
      <Link to="/contests" className="mb-6 inline-flex items-center gap-1.5 text-sm text-zinc-400 hover:text-white">
        ← Contests
      </Link>

      {/* Header */}
      <div className="mb-8 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="font-display text-3xl font-bold tracking-tight text-white">{contest.name}</h1>
            <span
              className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium"
              style={{ color: st.color, backgroundColor: `${st.color}1a` }}
            >
              <span className={`dot ${contest.status === 'RUNNING' ? 'animate-pulse' : ''}`} style={{ backgroundColor: st.color }} />
              {st.label}
            </span>
          </div>
          {contest.description && (
            <p className="mt-2 max-w-2xl text-sm leading-relaxed text-zinc-400">{contest.description}</p>
          )}
          <div className="mt-2">{contest.status === 'RUNNING' && <Countdown end={contest.endTime} />}</div>
        </div>

        <div>
          {!user ? (
            <span className="text-sm text-zinc-500">Sign in to join.</span>
          ) : contest.registered ? (
            <span className="chip !text-accent-emerald">✓ Registered</span>
          ) : (
            <button onClick={onJoin} disabled={joining || ended} className="btn-primary !py-1.5 !text-sm">
              {ended ? 'Contest ended' : joining ? 'Joining…' : 'Join contest'}
            </button>
          )}
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_1.1fr]">
        {/* Problems */}
        <section className="rounded-2xl glass p-5">
          <h2 className="mb-4 font-display text-sm font-semibold uppercase tracking-wider text-brand-400">
            Problems
          </h2>
          <div className="space-y-2">
            {contest.problems.map((p, i) => {
              const d = DIFF[p.difficulty] ?? DIFF.EASY
              const solved = mySolved.has(p.slug)
              return (
                <Link
                  key={p.slug}
                  to={`/problems/${p.slug}`}
                  className="flex items-center justify-between rounded-lg border border-white/5 bg-white/[0.02] px-4 py-3 transition-colors hover:border-brand-500/40"
                >
                  <span className="flex items-center gap-3">
                    <span className="font-mono text-xs text-zinc-500">{String.fromCharCode(65 + i)}</span>
                    <span className={`text-sm ${solved ? 'text-accent-emerald' : 'text-zinc-200'}`}>
                      {solved ? '✓ ' : ''}{p.title}
                    </span>
                  </span>
                  <span className="text-xs font-medium" style={{ color: d.color }}>{d.label}</span>
                </Link>
              )
            })}
          </div>
        </section>

        {/* Standings */}
        <section className="rounded-2xl glass p-5">
          <h2 className="mb-4 font-display text-sm font-semibold uppercase tracking-wider text-brand-400">
            Standings {ended ? '(final)' : '· live'}
          </h2>
          <div className="grid grid-cols-[48px_1fr_70px_80px] gap-2 border-b border-white/5 pb-2 text-xs font-medium uppercase tracking-wider text-zinc-500">
            <span>#</span>
            <span>Player</span>
            <span className="text-right">Solved</span>
            <span className="text-right">{ended ? 'Δ' : 'Pen.'}</span>
          </div>
          {standings.length === 0 && (
            <div className="py-6 text-center text-sm text-zinc-500">No submissions yet.</div>
          )}
          {standings.map((r) => {
            const tier = ratingTier(r.rating)
            const isMe = user && user.username === r.username
            return (
              <motion.div
                key={r.username}
                layout
                transition={{ type: 'spring', stiffness: 200, damping: 26 }}
                className={`grid grid-cols-[48px_1fr_70px_80px] items-center gap-2 border-b border-white/5 py-2.5 text-sm ${
                  isMe ? 'bg-brand-500/10' : ''
                }`}
              >
                <span className="font-mono text-zinc-400">{MEDALS[r.rank] ?? r.rank}</span>
                <span className="truncate font-medium" style={{ color: tier.color }}>{r.username}</span>
                <span className="text-right text-zinc-200">{r.solvedCount}</span>
                <span className="text-right text-xs">
                  {ended && r.ratingDelta != null ? (
                    <span className={r.ratingDelta >= 0 ? 'text-accent-emerald' : 'text-accent-rose'}>
                      {r.ratingDelta >= 0 ? '+' : ''}{r.ratingDelta}
                    </span>
                  ) : (
                    <span className="text-zinc-500">{r.penaltyMinutes}m</span>
                  )}
                </span>
              </motion.div>
            )
          })}
        </section>
      </div>
    </motion.div>
  )
}
