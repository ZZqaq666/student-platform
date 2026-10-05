import MarkdownIt from 'markdown-it'
import katex from 'katex'
import 'katex/dist/katex.min.css'

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  table: true
})

md.renderer.rules.table_open = function(tokens, idx, options, env, self) {
  return '<table class="md-table">'
}

md.renderer.rules.fence = function(tokens, idx, options, env, self) {
  const token = tokens[idx]
  const code = token.content.trim()
  const lang = token.info.trim()
  
  if (lang === 'math' || lang === 'latex') {
    try {
      return katex.renderToString(code, {
        displayMode: true,
        throwOnError: false
      })
    } catch (e) {
      return `<pre><code>${code}</code></pre>`
    }
  }
  
  const defaultRender = function(tokens, idx, options, env, self) {
    return self.renderToken(tokens, idx, options)
  }
  return defaultRender(tokens, idx, options, env, self)
}

function decodeHtmlEntities(text) {
  const textarea = document.createElement('textarea')
  textarea.innerHTML = text
  return textarea.value
}

export function renderMarkdown(text) {
  if (!text) return ''
  
  let result = decodeHtmlEntities(text)
  
  result = result.replace(/\$\$(.*?)\$\$/g, (match, formula) => {
    try {
      return katex.renderToString(formula.trim(), {
        displayMode: true,
        throwOnError: false
      })
    } catch (e) {
      return match
    }
  })
  
  result = result.replace(/\$(.*?)\$/g, (match, formula) => {
    try {
      return katex.renderToString(formula.trim(), {
        displayMode: false,
        throwOnError: false
      })
    } catch (e) {
      return match
    }
  })
  
  result = md.render(result)
  
  return result
}
