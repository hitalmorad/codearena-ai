import { useState } from 'react'
import { motion } from 'framer-motion'
import LoginModal from '../components/LoginModal.jsx'

const FEATURES = [
  'Solve in 6 languages',
  'Rated contests',
  'Live leaderboard',
  'AI Socratic tutor',
  'Mock interviewer',
]

export default function LandingPage() {
  // Auto-open sign-in so logged-out visitors (and users who just signed out)
  // are prompted to authenticate before accessing anything.
  const [open, setOpen] = useState(true)

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="mx-auto flex min-h-[calc(100vh-64px)] max-w-3xl flex-col items-center justify-center px-6 text-center"
    >
      <div className="chip mb-6 border-white/10 !text-zinc-300">
        <span className="dot bg-accent-emerald animate-pulse" />
        Competitive coding arena
      </div>

      <h1 className="font-display text-5xl font-bold leading-[1.05] tracking-tight text-white sm:text-6xl">
        Master code in the
        <br />
        <span className="gradient-text">competitive arena</span>
      </h1>

      <p className="mt-5 max-w-xl text-zinc-400">
        Solve challenges across six languages, compete in rated contests, and learn with an
        AI tutor. Sign in to start — everything here is members-only.
      </p>

      <div className="mt-6 flex flex-wrap justify-center gap-2">
        {FEATURES.map((f) => (
          <span key={f} className="chip text-zinc-400">{f}</span>
        ))}
      </div>

      <button onClick={() => setOpen(true)} className="btn-primary mt-8 !px-6 !py-2.5">
        Sign in to start
      </button>

      <LoginModal open={open} onClose={() => setOpen(false)} />
    </motion.div>
  )
}
