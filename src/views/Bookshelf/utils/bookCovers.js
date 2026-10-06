// 书籍封面相关工具函数

// 获取默认封面 - 使用本地 SVG 数据 URI
export const getDefaultCover = (subject) => {
  const svgConfigs = {
    'MATH': { bg: '#4A90D9', text: '数学' },
    'PHYSICS': { bg: '#50C878', text: '物理' },
    'CHEMISTRY': { bg: '#FF6B6B', text: '化学' },
    'CHINESE': { bg: '#9B59B6', text: '语文' },
    'ENGLISH': { bg: '#F39C12', text: '英语' },
    'DEFAULT': { bg: '#6B7280', text: '书籍' }
  }
  const config = svgConfigs[subject] || svgConfigs['DEFAULT']
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="560" viewBox="0 0 400 560">
    <rect width="400" height="560" fill="${config.bg}"/>
    <text x="200" y="280" font-family="Arial, sans-serif" font-size="48" fill="white" text-anchor="middle" dominant-baseline="middle">${config.text}</text>
  </svg>`
  return `data:image/svg+xml,${encodeURIComponent(svg)}`
}

// 处理书籍封面URL，避免使用豆瓣图片（418错误）
export const getSafeCoverUrl = (coverUrl, subject) => {
  // 如果是Douban图片，使用默认封面（避免418错误）
  if (coverUrl && coverUrl.includes('doubanio.com')) {
    return getDefaultCover(subject)
  }
  return coverUrl || getDefaultCover(subject)
}

// 图片加载错误处理（依赖 .book-card DOM 结构，须在含该结构的组件中使用）
export const handleImageError = (event) => {
  const img = event.target
  const title = img.closest('.book-card')?.querySelector('.book-title')?.textContent || ''
  const subject = title.includes('数学') ? 'MATH' :
                  title.includes('物理') ? 'PHYSICS' :
                  title.includes('化学') ? 'CHEMISTRY' :
                  title.includes('语文') ? 'CHINESE' :
                  title.includes('英语') ? 'ENGLISH' : 'DEFAULT'
  img.src = getDefaultCover(subject)
}

// 处理图片URL，通过代理服务加载
export const getProxyImageUrl = (url) => {
  if (!url) return ''
  // 如果已经是完整的图片链接（包含http://或https://），直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 否则使用后端图片代理服务处理
  return `/api/image-proxy/proxy?url=${encodeURIComponent(url)}`
}
