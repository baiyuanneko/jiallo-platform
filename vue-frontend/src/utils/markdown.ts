// Markdown 渲染工具 — XSS 安全配置

import MarkdownIt from 'markdown-it'
import type { Options } from 'markdown-it'
import type Token from 'markdown-it/lib/token.mjs'
import type Renderer from 'markdown-it/lib/renderer.mjs'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'

// 按需注册常用语言（避免全量引入增大包体积）
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import xml from 'highlight.js/lib/languages/xml'
import sql from 'highlight.js/lib/languages/sql'
import markdown from 'highlight.js/lib/languages/markdown'
import yaml from 'highlight.js/lib/languages/yaml'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import cpp from 'highlight.js/lib/languages/cpp'
import shell from 'highlight.js/lib/languages/shell'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('py', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('css', css)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('md', markdown)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('yml', yaml)
hljs.registerLanguage('go', go)
hljs.registerLanguage('rust', rust)
hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('c', cpp)
hljs.registerLanguage('shell', shell)
hljs.registerLanguage('sh', shell)

function highlightCode(str: string, lang: string): string {
  const language = lang?.trim()
  let codeHtml: string
  if (language && hljs.getLanguage(language)) {
    try {
      codeHtml = hljs.highlight(str, { language, ignoreIllegals: true }).value
    } catch {
      codeHtml = str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    }
  } else {
    codeHtml = str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  }
  const langAttr = language ? ` data-lang="${language}"` : ''
  return `<pre class="hljs"${langAttr}><div class="code-toolbar"><button class="copy-btn" type="button" title="复制代码"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg><span>复制</span></button></div><code>${codeHtml}</code></pre>`
}

const md = new MarkdownIt({
  html: false,
  xhtmlOut: false,
  breaks: true,
  linkify: true,
  typographer: false,
  langPrefix: 'language-',
  highlight: highlightCode,
})

const defaultLinkOpenRenderer =
  md.renderer.rules.link_open ||
  function (tokens: Token[], idx: number, options: Options, _env: unknown, self: Renderer) {
    return self.renderToken(tokens, idx, options)
  }

md.renderer.rules.link_open = function (
  tokens: Token[],
  idx: number,
  options: Options,
  env: unknown,
  self: Renderer,
) {
  const token = tokens[idx]!
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer')
  return defaultLinkOpenRenderer(tokens, idx, options, env, self)
}

export function renderMarkdown(text: string): string {
  const rawHtml = md.render(text)
  return DOMPurify.sanitize(rawHtml, {
    FORBID_TAGS: ['style', 'script', 'textarea', 'form', 'iframe', 'object', 'embed'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover'],
    ADD_ATTR: ['target'],
  })
}
