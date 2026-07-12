import { useEffect, useRef, useState } from 'react'
import { motion } from 'framer-motion'
import { fetchProblems, sendInterviewMessage, fetchAiStatus } from '../api/client.js'
import { useUser } from '../context/UserContext.jsx'
import Typewriter from '../components/Typewriter.jsx'

export default function InterviewPage() {
  const { user } = useUser()
  const [problems, setProblems] = useState([])
  const [slug, setSlug] = useState('')
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const [aiOn, setAiOn] = useState(null)
  const [error, setError] = useState('')
  const [animAt, setAnimAt] = useState(-1)
  const scrollRef = useRef(null)

  useEffect(() => {
    fetchProblems().then(setProblems).catch(() => {})
    fetchAiStatus().then((s) => setAiOn(s.enabled)).catch(() => setAiOn(false))
  }, [])

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, busy])

  function scrollToEnd() {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }

  async function send() {
    const text = input.trim()
    if (!text || busy) return
    setInput('')
    setError('')
    // optimistic append
    setMessages((prev) => [...prev, { role: 'user', content: text }])
    setBusy(true)
    try {
      const res = await sendInterviewMessage(slug || null, messages, text)
      setMessages(res.history)
      setAnimAt(res.history.length - 1) // typewriter-reveal the newest reply
    } catch (err) {
      setError(err?.response?.data?.message ?? 'The interviewer could not respond.')
      // roll back optimistic message so the user can retry
      setMessages((prev) => prev.filter((m, i) => !(i === prev.length - 1 && m.content === text)))
      setInput(text)
    } finally {
      setBusy(false)
    }
  }

  function reset() {
    setMessages([])
    setAnimAt(-1)
    setError('')
  }

  function onKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  if (!user) {
    return (
      <div className="py-24 text-center">
        <p className="text-sm text-zinc-400">Sign in to start a mock interview.</p>
      </div>
    )
  }

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="mx-auto max-w-3xl px-6 py-8">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-white">Mock interviewer</h1>
          <p className="text-sm text-zinc-500">
            Think aloud — get probing follow-ups on approach, complexity and edge cases.
          </p>
        </div>
        <span
          className="chip text-[10px]"
          style={{ color: aiOn ? '#34d399' : '#fbbf24' }}
          title={aiOn ? 'Live AI interviewer' : 'Offline scripted mode'}
        >
          {aiOn == null ? '…' : aiOn ? '● AI live' : '● offline mode'}
        </span>
      </div>

      <div className="mb-3 flex items-center gap-2">
        <select
          value={slug}
          onChange={(e) => setSlug(e.target.value)}
          className="rounded-lg border border-white/10 bg-ink-800 px-3 py-1.5 text-sm text-zinc-200 outline-none focus:border-brand-500"
        >
          <option value="">General (no specific problem)</option>
          {problems.map((p) => (
            <option key={p.slug} value={p.slug}>{p.title}</option>
          ))}
        </select>
        {messages.length > 0 && (
          <button onClick={reset} className="btn-ghost !py-1.5 !text-xs">Restart</button>
        )}
      </div>

      <div className="rounded-2xl glass">
        <div ref={scrollRef} className="h-[52vh] space-y-4 overflow-y-auto p-5">
          {messages.length === 0 && !busy && (
            <div className="flex h-full items-center justify-center text-center text-sm text-zinc-500">
              <span>Say “I’m ready” or describe how you’d start, and the interview begins.</span>
            </div>
          )}
          {messages.map((m, i) => (
            <div key={i} className={`flex ${m.role === 'user' ? 'justify-end' : 'justify-start'}`}>
              <div
                className={`max-w-[80%] whitespace-pre-wrap rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${
                  m.role === 'user'
                    ? 'bg-brand-600 text-white'
                    : 'border border-white/10 bg-white/[0.04] text-zinc-200'
                }`}
              >
                {m.role === 'assistant' ? (
                  <Typewriter text={m.content} animate={i === animAt} onTick={scrollToEnd} />
                ) : (
                  m.content
                )}
              </div>
            </div>
          ))}
          {busy && (
            <div className="flex justify-start">
              <div className="rounded-2xl border border-white/10 bg-white/[0.04] px-4 py-2.5 text-sm text-zinc-500">
                interviewer is typing…
              </div>
            </div>
          )}
        </div>

        {error && <p className="px-5 pb-2 text-xs text-accent-rose">⚠ {error}</p>}

        <div className="flex items-end gap-2 border-t border-white/10 p-3">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKeyDown}
            rows={1}
            placeholder="Type your answer…  (Enter to send, Shift+Enter for newline)"
            className="min-h-[42px] max-h-32 flex-1 resize-none rounded-xl border border-white/10 bg-white/[0.04] px-3 py-2.5 text-sm text-zinc-100 outline-none focus:border-brand-500"
          />
          <button onClick={send} disabled={busy || !input.trim()} className="btn-primary !py-2.5">
            Send
          </button>
        </div>
      </div>
    </motion.div>
  )
}
