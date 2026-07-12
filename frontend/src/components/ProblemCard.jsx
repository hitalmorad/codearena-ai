import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'

const DIFFICULTY = {
  EASY: { label: 'Easy', color: '#34d399' },
  MEDIUM: { label: 'Medium', color: '#fbbf24' },
  HARD: { label: 'Hard', color: '#fb7185' },
}

export default function ProblemCard({ problem, index }) {
  const diff = DIFFICULTY[problem.difficulty] ?? DIFFICULTY.EASY
  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true }}
      transition={{ delay: index * 0.05, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ y: -5 }}
    >
      <Link
        to={`/problems/${problem.slug}`}
        className="border-gradient group relative block overflow-hidden rounded-2xl glass p-5 transition-shadow duration-300 hover:shadow-glow"
      >
        {/* hover sheen */}
        <div className="pointer-events-none absolute -inset-px opacity-0 transition-opacity duration-300 group-hover:opacity-100">
          <div className="absolute inset-0 bg-radial-fade" />
        </div>

        <div className="relative">
          <div className="mb-3 flex items-center justify-between">
            <span className="font-mono text-xs text-zinc-500">
              {String(index + 1).padStart(2, '0')}
            </span>
            <span
              className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium"
              style={{ color: diff.color, backgroundColor: `${diff.color}1a` }}
            >
              <span className="dot" style={{ backgroundColor: diff.color }} />
              {diff.label}
            </span>
          </div>

          <h3 className="font-display text-lg font-semibold text-zinc-100 transition-colors group-hover:text-white">
            {problem.title}
          </h3>

          <div className="mt-3 flex flex-wrap gap-1.5">
            {(problem.tags ?? []).map((tag) => (
              <span key={tag} className="chip text-zinc-400">
                {tag}
              </span>
            ))}
          </div>

          <div className="mt-5 flex items-center gap-1.5 text-sm font-medium text-brand-400 opacity-0 transition-all duration-300 group-hover:opacity-100">
            Solve challenge
            <span className="transition-transform duration-300 group-hover:translate-x-1">→</span>
          </div>
        </div>
      </Link>
    </motion.div>
  )
}
