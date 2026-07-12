import axios from 'axios'

export const TOKEN_KEY = 'codearena.token'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Attach the session token (if any) to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['X-Auth-Token'] = token
  }
  return config
})

// On an expired/invalid token, drop it and let the app sign the user out.
// (Skip login/register — a 401 there just means wrong credentials.)
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url = err?.config?.url ?? ''
    const isAuthAttempt = url.includes('/auth/login') || url.includes('/auth/register')
    if (err?.response?.status === 401 && !isAuthAttempt) {
      localStorage.removeItem(TOKEN_KEY)
      window.dispatchEvent(new Event('codearena:unauthorized'))
    }
    return Promise.reject(err)
  },
)

// ---------- Auth ----------
export async function authRegister(username, password) {
  const { data } = await api.post('/auth/register', { username, password })
  return data
}

export async function authLogin(username, password) {
  const { data } = await api.post('/auth/login', { username, password })
  return data
}

export async function authMe() {
  const { data } = await api.get('/auth/me')
  return data
}

export async function updateBio(bio) {
  const { data } = await api.put('/auth/me/bio', { bio })
  return data
}

export async function changePassword(currentPassword, newPassword) {
  const { data } = await api.put('/auth/me/password', { currentPassword, newPassword })
  return data
}

// ---------- AI ----------
export async function fetchAiStatus() {
  const { data } = await api.get('/ai/status')
  return data
}

export async function getHint(slug, level, language, sourceCode) {
  const { data } = await api.post('/ai/hint', { slug, level, language, sourceCode })
  return data
}

export async function sendInterviewMessage(slug, history, message) {
  const { data } = await api.post('/ai/interview', { slug, history, message })
  return data
}

// ---------- Problems ----------
export async function fetchProblems() {
  const { data } = await api.get('/problems')
  return data
}

export async function fetchProblem(slug) {
  const { data } = await api.get(`/problems/${slug}`)
  return data
}

export async function submitSolution(slug, language, sourceCode, username) {
  const { data } = await api.post(`/problems/${slug}/submit`, { language, sourceCode, username })
  return data
}

export async function runSolution(slug, language, sourceCode, username) {
  const { data } = await api.post(`/problems/${slug}/run`, { language, sourceCode, username })
  return data
}

export async function fetchSubmissions(slug) {
  const { data } = await api.get(`/problems/${slug}/submissions`)
  return data
}

// ---------- Users / leaderboard ----------
export async function registerUser(username) {
  const { data } = await api.post('/users/register', { username })
  return data
}

export async function fetchUser(username) {
  const { data } = await api.get(`/users/${username}`)
  return data
}

export async function fetchProfile(username) {
  const { data } = await api.get(`/users/${username}/profile`)
  return data
}

export async function fetchActivity(username) {
  const { data } = await api.get(`/users/${username}/activity`)
  return data
}

export async function fetchUserSubmissions(username) {
  const { data } = await api.get(`/users/${username}/submissions`)
  return data
}

export async function fetchUserContests(username) {
  const { data } = await api.get(`/users/${username}/contests`)
  return data
}

export async function fetchLeaderboard() {
  const { data } = await api.get('/leaderboard')
  return data
}

// ---------- Contests ----------
export async function fetchContests() {
  const { data } = await api.get('/contests')
  return data
}

export async function fetchContest(id, username) {
  const { data } = await api.get(`/contests/${id}`, { params: username ? { username } : {} })
  return data
}

export async function joinContest(id, username) {
  await api.post(`/contests/${id}/register`, { username })
}

export async function fetchStandings(id) {
  const { data } = await api.get(`/contests/${id}/standings`)
  return data
}

/**
 * Subscribe to a Server-Sent Events stream (via the Vite proxy). Returns the
 * EventSource so the caller can close it. `onData` receives parsed JSON from
 * "update" events.
 */
export function subscribeStream(path, onData) {
  const source = new EventSource(`/api${path}`)
  source.addEventListener('update', (e) => {
    try {
      onData(JSON.parse(e.data))
    } catch {
      /* ignore malformed frames */
    }
  })
  return source
}

// ---------- Admin (X-Admin-Key header, or the global auth token for admins) ----------
function adminCfg(key) {
  return key ? { headers: { 'X-Admin-Key': key } } : {}
}

export async function verifyAdminKey(key) {
  const { data } = await api.get('/admin/verify', adminCfg(key))
  return data
}

export async function adminCreateProblem(key, payload) {
  const { data } = await api.post('/admin/problems', payload, adminCfg(key))
  return data
}

export async function adminUpdateProblem(key, slug, payload) {
  const { data } = await api.put(`/admin/problems/${slug}`, payload, adminCfg(key))
  return data
}

export async function adminDeleteProblem(key, slug) {
  const { data } = await api.delete(`/admin/problems/${slug}`, adminCfg(key))
  return data
}

export async function adminCreateContest(key, payload) {
  const { data } = await api.post('/admin/contests', payload, adminCfg(key))
  return data
}

export default api
