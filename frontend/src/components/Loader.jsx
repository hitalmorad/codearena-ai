export default function Loader({ label = 'Judging' }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-4">
      <div className="loader-ring" />
      <div className="flex items-center gap-1 text-sm font-medium text-zinc-400">
        {label}
        <span className="inline-flex">
          <span className="animate-pulse">.</span>
          <span className="animate-pulse [animation-delay:150ms]">.</span>
          <span className="animate-pulse [animation-delay:300ms]">.</span>
        </span>
      </div>
    </div>
  )
}
