import { Routes, Route, useLocation } from 'react-router-dom'
import { AnimatePresence } from 'framer-motion'
import Background3D from './components/Background3D.jsx'
import Navbar from './components/Navbar.jsx'
import HomePage from './pages/HomePage.jsx'
import ProblemPage from './pages/ProblemPage.jsx'
import LeaderboardPage from './pages/LeaderboardPage.jsx'
import ContestsPage from './pages/ContestsPage.jsx'
import ContestDetailPage from './pages/ContestDetailPage.jsx'
import AdminPage from './pages/AdminPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import InterviewPage from './pages/InterviewPage.jsx'
import LandingPage from './pages/LandingPage.jsx'
import { useUser } from './context/UserContext.jsx'

export default function App() {
  const location = useLocation()
  const { user, ready } = useUser()
  return (
    <div className="relative min-h-screen">
      {/* Layered ambient background */}
      <div className="aurora" />
      <div className="grid-bg" />
      <Background3D />
      <div className="noise" />

      <Navbar />

      <main className="relative z-10">
        {!ready ? (
          <div className="py-24 text-center text-sm text-zinc-500">Loading…</div>
        ) : !user ? (
          <LandingPage />
        ) : (
          <AnimatePresence mode="wait">
            <Routes location={location} key={location.pathname}>
              <Route path="/" element={<HomePage />} />
              <Route path="/problems/:slug" element={<ProblemPage />} />
              <Route path="/leaderboard" element={<LeaderboardPage />} />
              <Route path="/contests" element={<ContestsPage />} />
              <Route path="/contests/:id" element={<ContestDetailPage />} />
              <Route path="/interview" element={<InterviewPage />} />
              <Route path="/admin" element={<AdminPage />} />
              <Route path="/u/:username" element={<ProfilePage />} />
            </Routes>
          </AnimatePresence>
        )}
      </main>
    </div>
  )
}
