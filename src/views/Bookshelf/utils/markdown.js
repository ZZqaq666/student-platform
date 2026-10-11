import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import cpp from 'highlight.js/lib/languages/cpp'
import xml from 'highlight.js/lib/languages/xml'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import sql from 'highlight.js/lib/languages/sql'
import plain from 'highlight.js/lib/languages/plaintext'
import DOMPurify from 'dompurify'

// 按需注册常用语言，避免整包体积过大
const LANGS = { javascript, python, java, cpp, xml, json, bash, sql, plaintext: plain }
Object.entries(LANGS).forEach(([name, def]) => hljs.registerLanguage(name, def))

// 笔记预览专用 markdown-it 实例：
// - html:false 关闭内嵌 HTML，用户粘贴的脚本一律转义显示
// - 代码块走 highlight.js 高亮，class 与 bookshelf.css 中 .note-preview 样式对齐
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
})

md.renderer.rules.fence = (tokens, idx) => {
  const token = tokens[idx]
  const code = token.content
  const lang = (token.info || '').trim().split(/\s+/)[0]
  let highlighted
  if (lang && hljs.getLanguage(lang)) {
    try {
      highlighted = hljs.highlight(code, { language: lang, ignoreIllegals: true }).value
    } catch (e) {
      highlighted = md.utils.escapeHtml(code)
    }
  } else {
    highlighted = md.utils.escapeHtml(code)
  }
  return `<pre class="code-block"><code class="hljs">${highlighted}</code></pre>`
}

md.renderer.rules.code_inline = (tokens, idx) =>
  `<code class="inline-code">${md.utils.escapeHtml(tokens[idx].content)}</code>`

// Markdown 渲染与预览工具

// 获取笔记预览文本
export const getNotePreview = (content) => {
  if (!content) return ''
  // 移除 Markdown 标记，获取纯文本预览
  const plainText = content
    .replace(/#{1,6}\s/g, '')
    .replace(/\*\*/g, '')
    .replace(/\*/g, '')
    .replace(/`{3}[\s\S]*?`{3}/g, '[代码块]')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/>\s/g, '')
    .replace(/-\s/g, '')
    .replace(/\n+/g, ' ')
    .trim()
  return plainText.length > 60 ? plainText.substring(0, 60) + '...' : plainText
}

// Markdown 预览渲染：markdown-it 解析 + DOMPurify 消毒
export const renderMarkdownPreview = (content) => {
  if (!content) return ''
  return DOMPurify.sanitize(md.render(content))
}
