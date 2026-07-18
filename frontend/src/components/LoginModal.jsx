import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { useUser } from '../context/UserContext.jsx'

export default function LoginModal({ open, onClose }) {
  const { login, register } = useUser()
  const navigate = useNavigate()
  const [mode, setMode] = useState('login') // 'login' | 'register'
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const isRegister = mode === 'register'

  async function submit(e) {
    e.preventDefault()
    const name = username.trim()
    if (!/^[A-Za-z0-9_]{2,20}$/.test(name)) {
      setError('Username: 2–20 chars (letters, numbers, underscores).')
      return
    }
    if (isRegister && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      setError('Please enter a valid email address.')
      return
    }
    if (password.length < 4) {
      setError('Password must be at least 4 characters.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await (isRegister ? register(name, email, password) : login(name, password))
      setUsername('')
      setEmail('')
      setPassword('')
      onClose()
      navigate('/') // land on home, never a stale route from a previous session
    } catch (err) {
      setError(err?.response?.data?.message ?? 'Something went wrong.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
        >
          <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />
          <motion.div
            initial={{ scale: 0.92, y: 20, opacity: 0 }}
            animate={{ scale: 1, y: 0, opacity: 1 }}
            exit={{ scale: 0.95, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 120, damping: 16 }}
            className="border-gradient relative w-full max-w-sm rounded-2xl glass-strong p-7"
          >
            <h2 className="font-display text-xl font-bold text-white">
              {isRegister ? 'Create your account' : 'Welcome back'}
            </h2>
            <p className="mt-1 text-sm text-zinc-400">
              {isRegister
                ? 'Pick a handle and password to start earning a rating.'
                : 'Sign in to track your progress and ratings.'}
            </p>
            <form onSubmit={submit} className="mt-5 space-y-3">
              <input
                autoFocus
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="username"
                className="w-full rounded-lg border border-white/10 bg-ink-800 px-3 py-2.5 text-sm text-white outline-none focus:border-brand-500"
              />
              {isRegister && (
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="email"
                  className="w-full rounded-lg border border-white/10 bg-ink-800 px-3 py-2.5 text-sm text-white outline-none focus:border-brand-500"
                />
              )}
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="password"
                className="w-full rounded-lg border border-white/10 bg-ink-800 px-3 py-2.5 text-sm text-white outline-none focus:border-brand-500"
              />
              {error && <p className="text-xs text-accent-rose">{error}</p>}
              <button type="submit" disabled={busy} className="btn-primary w-full justify-center">
                {busy ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'}
              </button>
            </form>
            <button
              onClick={() => {
                setMode(isRegister ? 'login' : 'register')
                setError('')
              }}
              className="mt-4 w-full text-center text-xs text-zinc-400 hover:text-white"
            >
              {isRegister ? 'Already have an account? Sign in' : "New here? Create an account"}
            </button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
