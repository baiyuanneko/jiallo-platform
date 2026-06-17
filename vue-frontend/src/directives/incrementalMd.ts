import type { Directive } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

export const vIncrementalMd: Directive<HTMLElement, string> = {
  mounted(el, { value }) {
    el.style.whiteSpace = 'normal'
    if (value) el.innerHTML = renderMarkdown(value)
  },

  updated(el, { value, oldValue }) {
    if (value === oldValue) return
    const saved = saveSel(el)
    el.innerHTML = value ? renderMarkdown(value) : ''
    if (saved) requestAnimationFrame(() => restoreSel(el, saved))
  },
}

interface SavedRange { start: number; end: number }

function saveSel(el: HTMLElement): SavedRange | null {
  const s = window.getSelection()
  if (!s || s.isCollapsed || !el.contains(s.anchorNode) || !el.contains(s.focusNode)) return null
  try {
    const r = s.getRangeAt(0)
    const start = walkTo(el, r.startContainer, r.startOffset)
    const end = walkTo(el, r.endContainer, r.endOffset)
    if (start < 0 || end < 0) return null
    return { start, end }
  } catch { return null }
}

function walkTo(root: Node, target: Node, offset: number): number {
  let acc = 0
  function walk(n: Node): boolean {
    if (n === target) { acc += offset; return true }
    if (n.nodeType === 3) { acc += (n as Text).length }
    else { for (let i = 0; i < n.childNodes.length; i++) { if (walk(n.childNodes[i]!)) return true } }
    return false
  }
  walk(root)
  return acc
}

function restoreSel(el: HTMLElement, saved: SavedRange) {
  const full = el.textContent || ''
  if (saved.start > full.length || saved.end > full.length) return

  const a = findAt(el, saved.start)
  const f = findAt(el, saved.end)
  if (!a || !f) return

  try {
    const r = document.createRange()
    r.setStart(a.node, a.offset)
    r.setEnd(f.node, f.offset)
    const s = window.getSelection()
    if (s) { s.removeAllRanges(); s.addRange(r) }
  } catch { /* ignore */ }
}

function findAt(root: Node, offset: number): { node: Text; offset: number } | null {
  let acc = 0
  function walk(n: Node): { node: Text; offset: number } | null {
    if (n.nodeType === 3) {
      const len = (n as Text).length
      if (acc + len > offset) return { node: n as Text, offset: offset - acc }
      acc += len
    } else {
      for (let i = 0; i < n.childNodes.length; i++) {
        const r = walk(n.childNodes[i]!); if (r) return r
      }
    }
    return null
  }
  return walk(root)
}
