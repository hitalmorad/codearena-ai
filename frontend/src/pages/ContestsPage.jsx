import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { fetchContests } from '../api/client.js'

const STATUS = {
  RUNNING: { label: 'Live', color: '#34d399' },
  UPCOMING: { label: 'Upcoming', color: '#fbbf24' },
  ENDED: { label: 'Ended', color: '#9ca3af' },
}

function formatWindow(start, end) {
  const s = new Date(start)
  const e = new Date(end)
  const opts = { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }
  return `${s.toLocaleString(undefined, opts)} → ${e.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}`
}

export default function ContestsPage() {
  const [contests, setContests] = useState([])
  const [status, setStatus] = useState('loading')

  useEffect(() => {
    fetchContests()
      .then((data) => {
        setContests(data)
        setStatus('ready')
      })
      .catch(() => setStatus('error'))
  }, [])

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      className="mx-auto max-w-4xl px-6 py-10"
    >
      <div className="mb-8">
        <h1 className="font-display text-3xl font-bold tracking-tight text-white">Contests</h1>
        <p className="mt-1 text-sm text-zinc-500">Compete in timed rounds and climb the rating ladder.</p>
      </div>

      {status === 'error' && (
        <div className="rounded-2xl glass border-accent-rose/30 p-6 text-sm text-accent-rose">
          ⚠ Could not load contests.
        </div>
      )}

      {status === 'ready' && contests.length === 0 && (
        <div className="rounded-2xl glass p-8 text-center text-sm text-zinc-500">No contests scheduled yet.</div>
      )}

      <div className="space-y-4">
        {contests.map((c, i) => {
          const st = STATUS[c.status] ?? STATUS.ENDED
          return (
            <motion.div
              key={c.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
            >
              <Link
                to={`/contests/${c.id}`}
                className="border-gradient group flex items-center justify-between rounded-2xl glass p-5 transition-shadow hover:shadow-glow"
              >
                <div>
                  <div className="flex items-center gap-3">
                    <h3 className="font-display text-lg font-semibold text-white group-hover:text-white">
                      {c.name}
                    </h3>
                    <span
                      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium"
                      style={{ color: st.color, backgroundColor: `${st.color}1a` }}
                    >
                      <span className={`dot ${c.status === 'RUNNING' ? 'animate-pulse' : ''}`} style={{ backgroundColor: st.color }} />
                      {st.label}
                    </span>
                  </div>
                  <p className="mt-1.5 text-sm text-zinc-500">{formatWindow(c.startTime, c.endTime)}</p>
                </div>
                <div className="flex items-center gap-6 text-sm">
                  <div className="text-center">
                    <div className="font-semibold text-white">{c.problemCount}</div>
                    <div className="text-xs text-zinc-500">problems</div>
                  </div>
                  <div className="text-center">
                    <div className="font-semibold text-white">{c.participantCount}</div>
                    <div className="text-xs text-zinc-500">players</div>
                  </div>
                  <span className="text-brand-400 transition-transform group-hover:translate-x-1">→</span>
                </div>
              </Link>
            </motion.div>
          )
        })}
      </div>
    </motion.div>
  )
}
