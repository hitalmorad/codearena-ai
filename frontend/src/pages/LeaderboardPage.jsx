import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { fetchLeaderboard, subscribeStream } from '../api/client.js'
import { useUser } from '../context/UserContext.jsx'
import { ratingTier } from '../lib/rating.js'

const MEDALS = { 1: '🥇', 2: '🥈', 3: '🥉' }

export default function LeaderboardPage() {
  const { user } = useUser()
  const [rows, setRows] = useState([])
  const [status, setStatus] = useState('loading')
  const [live, setLive] = useState(false)

  useEffect(() => {
    fetchLeaderboard()
      .then((data) => {
        setRows(data)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))

    const source = subscribeStream('/leaderboard/stream', setRows)
    source.onopen = () => setLive(true)
    source.onerror = () => setLive(false)
    return () => source.close()
  }, [])

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="mx-auto max-w-4xl px-6 py-10"
    >
      <div className="mb-8 flex items-end justify-between">
        <div>
          <h1 className="font-display text-3xl font-bold tracking-tight text-white">Leaderboard</h1>
          <p className="mt-1 text-sm text-zinc-500">Global ranking by rating. Updates in real time.</p>
        </div>
        <span className="chip">
          <span className={`dot ${live ? 'bg-accent-emerald animate-pulse' : 'bg-zinc-600'}`} />
          {live ? 'Live' : 'Offline'}
        </span>
      </div>

      {status === 'error' && (
        <div className="rounded-2xl glass border-accent-rose/30 p-6 text-sm text-accent-rose">
          ⚠ Could not load the leaderboard. Is the backend running on <code className="text-accent-emerald">:8080</code>?
        </div>
      )}

      {status !== 'error' && (
        <div className="overflow-hidden rounded-2xl glass">
          <div className="grid grid-cols-[64px_1fr_110px_110px] gap-2 border-b border-white/5 px-5 py-3 text-xs font-medium uppercase tracking-wider text-zinc-500">
            <span>Rank</span>
            <span>Competitor</span>
            <span className="text-right">Rating</span>
            <span className="text-right">Solved</span>
          </div>

          {status === 'loading' && (
            <div className="p-6 text-center text-sm text-zinc-500">Loading…</div>
          )}

          {status === 'ready' && rows.length === 0 && (
            <div className="p-8 text-center text-sm text-zinc-500">
              No competitors yet — solve a problem to get on the board!
            </div>
          )}

          {rows.map((r) => {
            const tier = ratingTier(r.rating)
            const isMe = user && user.username === r.username
            return (
              <motion.div
                key={r.username}
                layout
                transition={{ type: 'spring', stiffness: 200, damping: 26 }}
                className={`grid grid-cols-[64px_1fr_110px_110px] items-center gap-2 border-b border-white/5 px-5 py-3.5 text-sm ${
                  isMe ? 'bg-brand-500/10' : ''
                }`}
              >
                <span className="font-mono text-zinc-400">
                  {MEDALS[r.rank] ?? `#${r.rank}`}
                </span>
                <span className="flex items-center gap-2 font-medium" style={{ color: tier.color }}>
                  {r.username}
                  {isMe && <span className="chip !py-0 text-[10px] text-zinc-300">you</span>}
                </span>
                <span className="text-right font-semibold" style={{ color: tier.color }}>
                  {r.rating}
                </span>
                <span className="text-right text-zinc-300">{r.problemsSolved}</span>
              </motion.div>
            )
          })}
        </div>
      )}
    </motion.div>
  )
}
