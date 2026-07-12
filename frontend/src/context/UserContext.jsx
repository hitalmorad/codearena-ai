import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { authLogin, authRegister, authMe, TOKEN_KEY } from '../api/client.js'

const UserContext = createContext(null)

export function UserProvider({ children }) {
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false)

  // Restore session from the stored token.
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setReady(true)
      return
    }
    authMe()
      .then((u) => {
        // /auth/me returns a fresh (sliding) token — persist it.
        if (u?.token) localStorage.setItem(TOKEN_KEY, u.token)
        setUser(u)
      })
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setReady(true))
  }, [])

  // Sign out automatically when a request reports the token is no longer valid.
  useEffect(() => {
    const onUnauthorized = () => setUser(null)
    window.addEventListener('codearena:unauthorized', onUnauthorized)
    return () => window.removeEventListener('codearena:unauthorized', onUnauthorized)
  }, [])

  const login = useCallback(async (username, password) => {
    const u = await authLogin(username.trim(), password)
    localStorage.setItem(TOKEN_KEY, u.token)
    setUser(u)
    return u
  }, [])

  const register = useCallback(async (username, password) => {
    const u = await authRegister(username.trim(), password)
    localStorage.setItem(TOKEN_KEY, u.token)
    setUser(u)
    return u
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  // Persist an updated auth payload (e.g. after a bio edit or password change).
  const applyAuth = useCallback((u) => {
    if (u?.token) localStorage.setItem(TOKEN_KEY, u.token)
    setUser(u)
  }, [])

  const refresh = useCallback(async () => {
    try {
      setUser(await authMe())
    } catch {
      /* keep current */
    }
  }, [])

  const isAdmin = user?.role === 'ADMIN'

  return (
    <UserContext.Provider value={{ user, ready, isAdmin, login, register, logout, applyAuth, refresh }}>
      {children}
    </UserContext.Provider>
  )
}

export function useUser() {
  const ctx = useContext(UserContext)
  if (!ctx) throw new Error('useUser must be used within UserProvider')
  return ctx
}
