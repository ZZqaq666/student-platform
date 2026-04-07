// 工具函数模块

/**
 * 获取节点颜色
 * @param {number} category - 节点类别
 * @returns {string} 颜色值
 */
export const getNodeColor = (category) => {
  const colors = ['#1565c0', '#1976d2', '#1e88e5', '#42a5f5']
  return colors[category] || '#64b5f6'
}

/**
 * 获取连接线颜色
 * @param {number} source - 源节点ID
 * @param {Array} nodes - 节点数据数组
 * @returns {string} 颜色值
 */
export const getLinkColor = (source, nodes) => {
  const node = nodes.find(n => n.id == source)
  if (!node) return '#ccc'
  if (node.category === 0) return '#1e88e5'
  if (node.category === 1) return '#42a5f5'
  return '#90caf9'
}

/**
 * 获取连接线宽度
 * @param {number} source - 源节点ID
 * @param {Array} nodes - 节点数据数组
 * @returns {number} 宽度值
 */
export const getLinkWidth = (source, nodes) => {
  const node = nodes.find(n => n.id == source)
  if (!node) return 1
  if (node.category === 0) return 3
  if (node.category === 1) return 2
  return 1
}

/**
 * 获取连接线类型
 * @param {number} source - 源节点ID
 * @param {Array} nodes - 节点数据数组
 * @returns {string} 线类型
 */
export const getLinkType = (source, nodes) => {
  const node = nodes.find(n => n.id == source)
  if (!node) return 'solid'
  if (node.category === 0) return 'solid'
  if (node.category === 1) return 'dashed'
  return 'dotted'
}

/**
 * 为书籍初始化节点位置
 * @param {Object} data - 图谱数据
 */
export const initNodePositionsForBook = (data) => {
  const centerX = 0
  const centerY = 0
  const level1Radius = 200
  const level2Radius = 350
  const level3Radius = 500
  
  // 第一层：中心节点
  const centerNode = data.nodes.find(n => n.category === 0)
  if (centerNode) {
    centerNode.x = centerX
    centerNode.y = centerY
    centerNode.fixed = true
  }
  
  // 第二层：主要模块
  const level1Nodes = data.nodes.filter(n => n.category === 1)
  level1Nodes.forEach((node, index) => {
    const angle = (index * 360 / level1Nodes.length - 90) * Math.PI / 180
    node.x = centerX + level1Radius * Math.cos(angle)
    node.y = centerY + level1Radius * Math.sin(angle)
  })
  
  // 第三层和第四层根据连接关系分布
  const level2Nodes = data.nodes.filter(n => n.category === 2)
  const level2Groups = {}
  level2Nodes.forEach(node => {
    const parentLink = data.links.find(l => l.target == node.id)
    if (parentLink) {
      if (!level2Groups[parentLink.source]) {
        level2Groups[parentLink.source] = []
      }
      level2Groups[parentLink.source].push(node)
    }
  })
  
  Object.keys(level2Groups).forEach((parentId) => {
    const parentNode = data.nodes.find(n => n.id == parentId)
    const nodes = level2Groups[parentId]
    if (parentNode) {
      const baseAngle = Math.atan2(parentNode.y - centerY, parentNode.x - centerX)
      
      nodes.forEach((node, index) => {
        const offset = (index - (nodes.length - 1) / 2) * 0.5
        const angle = baseAngle + offset
        node.x = centerX + level2Radius * Math.cos(angle)
        node.y = centerY + level2Radius * Math.sin(angle)
      })
    }
  })
  
  // 第四层：细分知识点
  const level3Nodes = data.nodes.filter(n => n.category === 3)
  const level3Groups = {}
  level3Nodes.forEach(node => {
    const parentLink = data.links.find(l => l.target == node.id)
    if (parentLink) {
      if (!level3Groups[parentLink.source]) {
        level3Groups[parentLink.source] = []
      }
      level3Groups[parentLink.source].push(node)
    }
  })
  
  Object.keys(level3Groups).forEach((parentId) => {
    const parentNode = data.nodes.find(n => n.id == parentId)
    const nodes = level3Groups[parentId]
    if (parentNode) {
      const baseAngle = Math.atan2(parentNode.y - centerY, parentNode.x - centerX)
      
      nodes.forEach((node, index) => {
        const offset = (index - (nodes.length - 1) / 2) * 0.4
        const angle = baseAngle + offset
        node.x = centerX + level3Radius * Math.cos(angle)
        node.y = centerY + level3Radius * Math.sin(angle)
      })
    }
  })
}

/**
 * 获取模块名称
 * @param {number} category - 节点类别
 * @returns {string} 模块名称
 */
export const getModuleName = (category) => {
  const modules = ['核心课程', '章节模块', '核心知识点', '细分知识点']
  return modules[category] || '其他'
}

/**
 * 获取层级名称
 * @param {number} category - 节点类别
 * @returns {string} 层级名称
 */
export const getLevelName = (category) => {
  const levels = ['核心', '一级', '二级', '三级']
  return levels[category] || '其他'
}

/**
 * 获取难度颜色
 * @param {string} difficulty - 难度等级
 * @returns {string} 颜色值
 */
export const getDifficultyColor = (difficulty) => {
  const colorMap = {
    '基础巩固': '#10b981',
    '进阶提升': '#f59e0b',
    '真题冲刺': '#ef4444',
    '竞赛拓展': '#8b5cf6'
  }
  return colorMap[difficulty] || '#6b7280'
}
