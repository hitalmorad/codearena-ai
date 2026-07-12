// Codeforces-style rating tiers.
const TIERS = [
  { min: 2200, label: 'Master', color: '#fb923c' },
  { min: 1900, label: 'Candidate Master', color: '#c084fc' },
  { min: 1600, label: 'Expert', color: '#60a5fa' },
  { min: 1400, label: 'Specialist', color: '#22d3ee' },
  { min: 1200, label: 'Pupil', color: '#34d399' },
  { min: 0, label: 'Newbie', color: '#9ca3af' },
]

export function ratingTier(rating) {
  return TIERS.find((t) => rating >= t.min) ?? TIERS[TIERS.length - 1]
}
