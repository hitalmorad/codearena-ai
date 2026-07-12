import { useEffect, useRef, useState } from 'react'

/**
 * Reveals `text` progressively for a typewriter effect. When `animate` is false
 * the full text is shown immediately. Pass a `render` function to wrap the
 * partial text (e.g. markdown); otherwise plain text is rendered. `onTick`
 * fires on each reveal step (useful for keeping a scroll view pinned).
 */
export default function Typewriter({ text = '', animate = true, speed = 16, render, onTick }) {
  const [shown, setShown] = useState(animate ? '' : text)
  const tickRef = useRef(onTick)
  tickRef.current = onTick

  useEffect(() => {
    if (!animate) {
      setShown(text)
      return
    }
    let i = 0
    setShown('')
    const chunk = Math.max(1, Math.ceil(text.length / 300))
    const id = setInterval(() => {
      i += chunk
      setShown(text.slice(0, i))
      tickRef.current?.()
      if (i >= text.length) clearInterval(id)
    }, speed)
    return () => clearInterval(id)
  }, [text, animate, speed])

  return render ? render(shown) : <>{shown}</>
}
