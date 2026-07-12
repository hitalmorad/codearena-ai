const VERDICT_META = {
  ACCEPTED: { label: 'Accepted', color: '#34d399', bg: 'rgba(52, 211, 153, 0.12)', icon: '✓' },
  WRONG_ANSWER: { label: 'Wrong Answer', color: '#fb7185', bg: 'rgba(251, 113, 133, 0.12)', icon: '✕' },
  TIME_LIMIT_EXCEEDED: { label: 'Time Limit Exceeded', color: '#fbbf24', bg: 'rgba(251, 191, 36, 0.12)', icon: '⏱' },
  MEMORY_LIMIT_EXCEEDED: { label: 'Memory Limit Exceeded', color: '#fbbf24', bg: 'rgba(251, 191, 36, 0.12)', icon: '▦' },
  RUNTIME_ERROR: { label: 'Runtime Error', color: '#fb7185', bg: 'rgba(251, 113, 133, 0.12)', icon: '!' },
  COMPILATION_ERROR: { label: 'Compilation Error', color: '#fb7185', bg: 'rgba(251, 113, 133, 0.12)', icon: '!' },
  INTERNAL_ERROR: { label: 'Judge Error', color: '#a1a1aa', bg: 'rgba(161, 161, 170, 0.12)', icon: '?' },
  PENDING: { label: 'Pending', color: '#22d3ee', bg: 'rgba(34, 211, 238, 0.12)', icon: '•' },
}

export default function VerdictBadge({ verdict }) {
  const meta = VERDICT_META[verdict] ?? VERDICT_META.INTERNAL_ERROR
  return (
    <span
      className="inline-flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-semibold"
      style={{ color: meta.color, backgroundColor: meta.bg, border: `1px solid ${meta.color}33` }}
    >
      <span
        className="grid h-5 w-5 place-items-center rounded-md text-xs"
        style={{ backgroundColor: `${meta.color}22` }}
      >
        {meta.icon}
      </span>
      {meta.label}
    </span>
  )
}
