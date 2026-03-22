import MarkdownIt from 'markdown-it';
import katex from 'katex';
import 'katex/dist/katex.min.css';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js';
import 'highlight.js/styles/github.css';

// 创建markdown-it实例并配置
const md = new MarkdownIt({
    highlight: function(str, lang) {
        if (lang && hljs.getLanguage(lang)) {
            try {
                return hljs.highlight(str, { language: lang }).value;
            } catch (__) {}
        }
        return '';
    },
    breaks: false,
    html: true,
    linkify: true,
    typographer: true
});

// 自定义代码块渲染
md.renderer.rules.code_block = function(tokens, idx, options, env, self) {
    const token = tokens[idx];
    const code = token.content;
    const lang = token.info ? token.info.trim() : '';

    const language = hljs.getLanguage(lang) ? lang : 'plaintext';
    const highlightedCode = hljs.highlight(code, { language }).value;

    // 检查是否需要行号（通过lang参数中的'linenums'标记）
    const showLineNumbers = lang && lang.includes('linenums');

    if (showLineNumbers) {
        // 生成带行号的代码块
        const lines = highlightedCode.split('\n');
        const numberedLines = lines.map((line, index) => {
            return `<div class="code-line"><span class="line-number">${index + 1}</span><span class="line-content">${line}</span></div>`;
        }).join('\n');

        return `<pre class="code-with-lines"><code class="hljs language-${language}">${numberedLines}</code></pre>`;
    } else {
        // 普通代码块
        return `<pre><code class="hljs language-${language}">${highlightedCode}</code></pre>`;
    }
};

// 处理KaTeX公式的函数
function processKaTeXFormulas(text) {
    let result = '';
    let i = 0;
    const length = text.length;

    while (i < length) {
        // 查找反斜杠
        const backslashIndex = text.indexOf('\\', i);

        if (backslashIndex === -1) {
            // 没有更多反斜杠，添加剩余文本
            result += text.substring(i);
            break;
        }

        // 添加反斜杠前的文本
        result += text.substring(i, backslashIndex);

        // 检查反斜杠后的字符
        if (backslashIndex + 1 < length) {
            const nextChar = text[backslashIndex + 1];

            if (nextChar === '(') {
                // 检测行内公式  ...
                const endIndex = findMatchingDelimiter(text, backslashIndex + 2, '\\)');
                if (endIndex !== -1) {
                    const formula = text.substring(backslashIndex + 2, endIndex);
                    try {
                        const rendered = katex.renderToString(formula, {
                            displayMode: false,
                            throwOnError: false,
                            errorCallback: function(msg, err) {
                                console.warn('KaTeX渲染错误:', msg, err);
                            }
                        });
                        result += rendered;
                        i = endIndex + 2; // 跳过 \)
                        continue;
                    } catch (error) {
                        console.error('Error rendering inline math:', error);
                    }
                }
            } else if (nextChar === '[') {
                // 检测块级公式 \[ ... \]
                const endIndex = findMatchingDelimiter(text, backslashIndex + 2, '\\]');
                if (endIndex !== -1) {
                    const formula = text.substring(backslashIndex + 2, endIndex);
                    try {
                        const rendered = katex.renderToString(formula, {
                            displayMode: true,
                            throwOnError: false,
                            errorCallback: function(msg, err) {
                                console.warn('KaTeX渲染错误:', msg, err);
                            }
                        });
                        result += rendered;
                        i = endIndex + 2; // 跳过 \]
                        continue;
                    } catch (error) {
                        console.error('Error rendering block math:', error);
                    }
                }
            }
        }

        // 如果不是有效的公式，添加反斜杠并继续
        result += '\\';
        i = backslashIndex + 1;
    }

    return result;
}

// 查找匹配的分隔符
function findMatchingDelimiter(text, startIndex, delimiter) {
    let i = startIndex;
    const length = text.length;
    const delimiterLength = delimiter.length;

    while (i <= length - delimiterLength) {
        if (text.substring(i, i + delimiterLength) === delimiter) {
            return i;
        }
        i++;
    }

    return -1;
}



// 主函数：处理AI返回的文本
export function processAiResponse(text) {
    // 1. 安全清理
    const cleanText = DOMPurify.sanitize(text);

    // 2. 清理多余的空行，只保留单个换行
    const textWithCleanLines = cleanText.replace(/\n{2,}/g, '\n');

    // 3. 处理KaTeX公式（在Markdown解析之前）
    const textWithMath = processKaTeXFormulas(textWithCleanLines);

    // 4. Markdown解析
    const html = md.render(textWithMath);

    // 5. 清理HTML中多余的空白和空行
    const finalHtml = html
        .replace(/\n{2,}/g, '\n')
        .replace(/>\s+</g, '><')
        .replace(/\s+/g, ' ');

    return finalHtml;
}

// 智能内容解析器
export class ContentParser {
    static parseMixedContent(text) {
        const blocks = [];
        let currentBlock = { type: 'text', content: '' };

        // 分割文本为不同类型的块
        const lines = text.split('\n');

        lines.forEach(line => {
            // 检测代码块
            if (line.trim().startsWith('```')) {
                if (currentBlock.type === 'code') {
                    blocks.push({ ...currentBlock });
                    currentBlock = { type: 'text', content: '' };
                } else {
                    if (currentBlock.content) blocks.push({ ...currentBlock });
                    currentBlock = {
                        type: 'code',
                        content: '',
                        language: line.replace('```', '').trim() || 'plaintext'
                    };
                }
            }
            // 普通文本
            else {
                if (currentBlock.type !== 'text') {
                    blocks.push({ ...currentBlock });
                    currentBlock = { type: 'text', content: line };
                } else {
                    currentBlock.content += (currentBlock.content ? '\n' : '') + line;
                }
            }
        });

        if (currentBlock.content) {
            blocks.push(currentBlock);
        }

        return blocks;
    }
}

// 辅助函数：仅清理文本
export function sanitizeText(text) {
    return DOMPurify.sanitize(text);
}

// 辅助函数：仅解析Markdown
export function parseMarkdown(text) {
    return md.render(text);
}



// 将Markdown格式的文本转换为纯文本
export function markdownToPlainText(markdown) {
    let plainText = markdown;

    // 1. 处理代码块 (```...```)
    plainText = plainText.replace(/```[\s\S]*?```/g, (match) => {
        // 提取代码内容，移除语言标记
        const codeContent = match.replace(/^```[\s\S]*?\n|```$/g, '');
        return codeContent;
    });

    // 2. 处理行内代码 (`...`)
    plainText = plainText.replace(/`([^`]+)`/g, '$1');

    // 3. 处理链接 ([text](url))
    plainText = plainText.replace(/\[([^\]]+)\]\([^)]+\)/g, '$1');

    // 4. 处理图片 (![alt](url))
    plainText = plainText.replace(/!\[([^\]]*)\]\([^)]+\)/g, '$1');

    // 5. 处理粗体 (**...** 或 __...__)
    plainText = plainText.replace(/\*\*([^*]+)\*\*/g, '$1');
    plainText = plainText.replace(/__([^_]+)__/g, '$1');

    // 6. 处理斜体 (*...* 或 _..._)
    plainText = plainText.replace(/\*([^*]+)\*/g, '$1');
    plainText = plainText.replace(/_([^_]+)_/g, '$1');

    // 7. 处理标题 (#)
    plainText = plainText.replace(/^#{1,6}\s+(.*)$/gm, '$1');

    // 8. 处理列表
    // 无序列表 (*, -, +)
    plainText = plainText.replace(/^\s*[*+-]\s+(.*)$/gm, '$1');
    // 有序列表 (1., 2., etc.)
    plainText = plainText.replace(/^\s*\d+\.\s+(.*)$/gm, '$1');

    // 9. 处理引用 (>)
    plainText = plainText.replace(/^\s*>\s*(.*)$/gm, '$1');

    // 10. 处理水平线 (---, ***)
    plainText = plainText.replace(/^[-*]{3,}$/gm, '');



    // 12. 清理多余的空行，将多个连续换行压缩为一个
    plainText = plainText.replace(/\n{2,}/g, '\n');

    // 13. 清理行尾空格
    plainText = plainText.replace(/\s+$/gm, '');

    // 14. 清理开头和结尾的空行
    plainText = plainText.trim();

    return plainText;
}
