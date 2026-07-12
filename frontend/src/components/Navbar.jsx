import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useUser } from '../context/UserContext.jsx'
import { ratingTier } from '../lib/rating.js'
import LoginModal from './LoginModal.jsx'

export default function Navbar() {
  const { pathname } = useLocation()
  const { user, logout, isAdmin } = useUser()
  const [loginOpen, setLoginOpen] = useState(false)

  const linkClass = (active) =>
    `relative text-sm transition-colors ${active ? 'text-white' : 'text-zinc-400 hover:text-white'}`

  return (
    <motion.header
      initial={{ y: -50, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ type: 'spring', stiffness: 90, damping: 16 }}
      className="sticky top-0 z-30"
    >
      <div className="glass-strong border-b border-white/5">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <Link to="/" className="group flex items-center gap-2.5">
            <div className="relative grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-brand-600 to-accent-cyan shadow-glow-sm">
              <span className="font-mono text-xs font-bold text-white">{'</>'}</span>
            </div>
            <div className="leading-tight">
              <div className="font-display text-base font-semibold tracking-tight text-white">
                Code<span className="gradient-text">Arena</span>
              </div>
              <div className="text-[10px] font-medium uppercase tracking-[0.25em] text-zinc-500">
                Coding&nbsp;Arena
              </div>
            </div>
          </Link>

          <nav className="hidden items-center gap-7 sm:flex">
            <Link to="/" className={linkClass(pathname === '/')}>Problems</Link>
            <Link to="/contests" className={linkClass(pathname.startsWith('/contests'))}>Contests</Link>
            <Link to="/leaderboard" className={linkClass(pathname === '/leaderboard')}>Leaderboard</Link>
            <Link to="/interview" className={linkClass(pathname === '/interview')}>Interview</Link>
            {isAdmin && <Link to="/admin" className={linkClass(pathname === '/admin')}>Admin</Link>}
          </nav>

          <div className="flex items-center gap-3">
            {user ? (
              <div className="flex items-center gap-2">
                <Link to={`/u/${user.username}`} className="flex items-center gap-2 rounded-lg border border-white/10 bg-white/[0.03] px-3 py-1.5 transition-colors hover:border-brand-500/50">
                  <span className="text-sm font-semibold text-white">{user.username}</span>
                  <span
                    className="rounded-md px-1.5 py-0.5 text-xs font-bold"
                    style={{
                      color: ratingTier(user.rating).color,
                      backgroundColor: `${ratingTier(user.rating).color}1a`,
                    }}
                    title={ratingTier(user.rating).label}
                  >
                    {user.rating}
                  </span>
                </Link>
                <button onClick={logout} className="btn-ghost !px-2.5 !py-1.5" title="Sign out">
                  ⏻
                </button>
              </div>
            ) : (
              <button onClick={() => setLoginOpen(true)} className="btn-primary !py-1.5 !text-sm">
                Sign in
              </button>
            )}
          </div>
        </div>
      </div>

      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
    </motion.header>
  )
}
