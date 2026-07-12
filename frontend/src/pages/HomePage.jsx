import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { fetchProblems } from '../api/client.js'
import ProblemCard from '../components/ProblemCard.jsx'

const STATS = [
  { value: '100%', label: 'Free & open-source' },
  { value: '6', label: 'Languages' },
  { value: '<1s', label: 'Verdict latency' },
  { value: '12', label: 'Problems' },
]

const FEATURES = [
  'Sandboxed execution',
  'Multi-language',
  'Real-time verdicts',
  'Live leaderboard',
  'Rated contests',
  'AI tutor · soon',
]

export default function HomePage() {
  const [problems, setProblems] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')

  useEffect(() => {
    fetchProblems()
      .then((data) => {
        setProblems(data)
        setStatus('ready')
      })
      .catch((err) => {
        setError(err?.message ?? 'Failed to load problems')
        setStatus('error')
      })
  }, [])

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.4 }}
      className="mx-auto max-w-7xl px-6"
    >
      {/* Hero */}
      <section className="relative flex flex-col items-center pt-24 pb-16 text-center">
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="chip mb-6 border-white/10 !text-zinc-300"
        >
          <span className="dot bg-accent-emerald animate-pulse" />
          Phase 2 · Contests &amp; live leaderboard
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
          className="font-display text-5xl font-bold leading-[1.05] tracking-tight text-white sm:text-6xl md:text-7xl"
        >
          Master code in the
          <br />
          <span className="gradient-text">competitive arena</span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.25, duration: 0.7 }}
          className="mt-7 max-w-2xl text-base leading-relaxed text-zinc-400 md:text-lg"
        >
          Write code in your browser, run it inside a locked-down sandbox, and get
          instant verdicts. An AI-powered coding platform built entirely on free,
          open-source technology.
        </motion.p>

        <motion.div
          initial={{ opacity: 0, y: 14 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4, duration: 0.6 }}
          className="mt-10 flex flex-wrap items-center justify-center gap-3"
        >
          <a href="#problems" className="btn-primary">
            Start solving
            <span>→</span>
          </a>
          <a href="#features" className="btn-ghost">
            Explore features
          </a>
        </motion.div>

        {/* Stats */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.55, duration: 0.6 }}
          className="mt-16 grid w-full max-w-3xl grid-cols-2 gap-4 sm:grid-cols-4"
        >
          {STATS.map((s) => (
            <div key={s.label} className="glass rounded-2xl px-4 py-5">
              <div className="font-display text-2xl font-bold text-white">{s.value}</div>
              <div className="mt-1 text-xs text-zinc-500">{s.label}</div>
            </div>
          ))}
        </motion.div>
      </section>

      {/* Feature marquee */}
      <div id="features" className="relative overflow-hidden rounded-2xl glass py-3.5">
        <div className="flex w-max animate-marquee gap-12 whitespace-nowrap">
          {[...FEATURES, ...FEATURES].map((f, i) => (
            <span key={i} className="flex items-center gap-2 text-sm text-zinc-400">
              <span className="h-1.5 w-1.5 rounded-full bg-brand-gradient" />
              {f}
            </span>
          ))}
        </div>
      </div>

      {/* Problems */}
      <section id="problems" className="scroll-mt-24 py-16">
        <div className="mb-8 flex items-end justify-between">
          <div>
            <h2 className="font-display text-3xl font-bold tracking-tight text-white">
              Problem set
            </h2>
            <p className="mt-1 text-sm text-zinc-500">Pick a challenge and start coding.</p>
          </div>
          <span className="chip">
            {status === 'ready' ? `${problems.length} challenges` : '—'}
          </span>
        </div>

        {status === 'loading' && (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="h-44 animate-pulse rounded-2xl glass" />
            ))}
          </div>
        )}

        {status === 'error' && (
          <div className="rounded-2xl glass border-accent-rose/30 p-6 text-sm text-accent-rose">
            ⚠ Could not reach the backend ({error}). Make sure the Spring Boot API is running on
            <code className="mx-1 font-mono text-accent-emerald">:8080</code>.
          </div>
        )}

        {status === 'ready' && (
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {problems.map((p, i) => (
              <ProblemCard key={p.id} problem={p} index={i} />
            ))}
          </div>
        )}
      </section>

      {/* Footer */}
      <footer className="border-t border-white/5 py-8 text-center text-xs text-zinc-600">
        CodeArena · Built with Spring Boot · React · Three.js — all free & open-source.
      </footer>
    </motion.div>
  )
}
