// 图表配置模块

import * as echarts from 'echarts'
import { getNodeColor, getLinkColor, getLinkWidth, getLinkType, initNodePositionsForBook } from './index'
import { nodeHighFrequencyKnowledge } from './dataProcessor'

/**
 * 初始化ECharts图表
 * @param {HTMLElement} container - 图表容器
 * @param {Object} data - 图谱数据
 * @param {Function} handleNodeClick - 节点点击处理函数
 * @returns {Object} 图表实例
 */
export const initChart = (container, data, handleNodeClick) => {
  if (!container) {
    console.error('图表容器未找到')
    return null
  }
  
  // 初始化节点位置
  initNodePositionsForBook(data)
  
  // 初始化图表
  const chart = echarts.init(container)
  
  // 设置图表配置
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        // 只显示节点的提示，不显示连接线的提示
        if (params.dataType === 'node') {
          const nodeName = params.name
          const highFrequencyKnowledge = nodeHighFrequencyKnowledge[nodeName] || []
          let tooltipContent = `<div style="font-weight: bold; margin-bottom: 8px;">${nodeName}</div>`
          
          if (highFrequencyKnowledge.length > 0) {
            tooltipContent += '<div style="font-size: 12px; color: #666;">高频知识点：</div>'
            highFrequencyKnowledge.forEach(item => {
              tooltipContent += `<div style="font-size: 12px; margin-left: 10px; margin-top: 4px;">• ${item}</div>`
            })
          }
          
          return tooltipContent
        }
        return ''
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#6366f1',
      borderWidth: 1,
      padding: 12,
      textStyle: {
        fontSize: 14
      }
    },
    animationDuration: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: data.nodes.map(node => ({
          ...node,
          itemStyle: {
            color: getNodeColor(node.category),
            borderColor: '#fff',
            borderWidth: 2,
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          },
          label: {
            show: true,
            position: node.category === 0 ? 'inside' : 'right',
            fontSize: node.category === 0 ? 16 : node.category === 1 ? 14 : 12,
            fontWeight: node.category <= 1 ? 'bold' : 'normal',
            color: node.category === 0 ? '#fff' : '#333',
            distance: node.category > 0 ? 10 : 0
          },
          symbolSize: node.symbolSize || (node.category === 0 ? 80 : node.category === 1 ? 60 : 45),
          emphasis: {
            itemStyle: {
              shadowBlur: 20,
              shadowColor: 'rgba(99, 102, 241, 0.6)',
              borderWidth: 3
            },
            label: {
              fontSize: node.category === 0 ? 18 : node.category === 1 ? 16 : 14,
              fontWeight: 'bold'
            }
          }
        })),
        links: data.links.map(link => ({
          ...link,
          lineStyle: {
            color: getLinkColor(link.source, data.nodes),
            width: getLinkWidth(link.source, data.nodes),
            curveness: 0.2,
            type: getLinkType(link.source, data.nodes),
            opacity: 0.7
          },
          emphasis: {
            lineStyle: {
              width: 4,
              opacity: 1
            }
          }
        })),
        categories: [
          { name: '核心课程', itemStyle: { color: '#1565c0' } },
          { name: '章节模块', itemStyle: { color: '#1976d2' } },
          { name: '核心知识点', itemStyle: { color: '#1e88e5' } },
          { name: '细分知识点', itemStyle: { color: '#42a5f5' } }
        ],
        roam: true,
        draggable: true,
        dragMode: ['pan', 'node'],
        focusNodeAdjacency: true,
        cursor: 'grab',
        emphasis: {
          cursor: 'grabbing',
          focus: 'adjacency',
          lineStyle: {
            width: 4
          }
        },
        scaleLimit: {
          min: 0.1,
          max: 10
        },
        force: {
          initLayout: 'circular',
          repulsion: 3000,
          edgeLength: [150, 300],
          gravity: 0.05,
          friction: 0.9,
          layoutAnimation: true,
          edgeLengthMin: 100,
          edgeLengthMax: 400
        }
      }
    ]
  }
  
  chart.setOption(option)
  
  // 绑定点击事件
  const clickHandler = (params) => {
    if (params.dataType === 'node') {
      handleNodeClick(params.data.name)
    }
  }
  
  chart.on('click', clickHandler)
  
  // 将事件处理函数存储在图表实例上，以便后续解绑
  chart._clickHandler = clickHandler
  
  return chart
}

/**
 * 销毁图表实例
 * @param {Object} chart - 图表实例
 */
export const destroyChart = (chart) => {
  if (chart) {
    // 解绑点击事件
    if (chart._clickHandler) {
      chart.off('click', chart._clickHandler)
    }
    // 销毁图表实例
    chart.dispose()
  }
}

/**
 * 更新图表数据
 * @param {Object} chart - 图表实例
 * @param {Object} data - 新的图谱数据
 * @param {Function} handleNodeClick - 节点点击处理函数（可选）
 */
export const updateChart = (chart, data, handleNodeClick) => {
  if (!chart) return
  
  // 初始化节点位置
  initNodePositionsForBook(data)
  
  // 直接创建新的图表配置
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        // 只显示节点的提示，不显示连接线的提示
        if (params.dataType === 'node') {
          const nodeName = params.name
          const highFrequencyKnowledge = nodeHighFrequencyKnowledge[nodeName] || []
          let tooltipContent = `<div style="font-weight: bold; margin-bottom: 8px;">${nodeName}</div>`
          
          if (highFrequencyKnowledge.length > 0) {
            tooltipContent += '<div style="font-size: 12px; color: #666;">高频知识点：</div>'
            highFrequencyKnowledge.forEach(item => {
              tooltipContent += `<div style="font-size: 12px; margin-left: 10px; margin-top: 4px;">• ${item}</div>`
            })
          }
          
          return tooltipContent
        }
        return ''
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#6366f1',
      borderWidth: 1,
      padding: 12,
      textStyle: {
        fontSize: 14
      }
    },
    animationDuration: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: data.nodes.map(node => ({
          ...node,
          itemStyle: {
            color: getNodeColor(node.category),
            borderColor: '#fff',
            borderWidth: 2,
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.2)'
          },
          label: {
            show: true,
            position: node.category === 0 ? 'inside' : 'right',
            fontSize: node.category === 0 ? 16 : node.category === 1 ? 14 : 12,
            fontWeight: node.category <= 1 ? 'bold' : 'normal',
            color: node.category === 0 ? '#fff' : '#333',
            distance: node.category > 0 ? 10 : 0
          },
          symbolSize: node.symbolSize || (node.category === 0 ? 80 : node.category === 1 ? 60 : 45),
          emphasis: {
            itemStyle: {
              shadowBlur: 20,
              shadowColor: 'rgba(99, 102, 241, 0.6)',
              borderWidth: 3
            },
            label: {
              fontSize: node.category === 0 ? 18 : node.category === 1 ? 16 : 14,
              fontWeight: 'bold'
            }
          }
        })),
        links: data.links.map(link => ({
          ...link,
          lineStyle: {
            color: getLinkColor(link.source, data.nodes),
            width: getLinkWidth(link.source, data.nodes),
            curveness: 0.2,
            type: getLinkType(link.source, data.nodes),
            opacity: 0.7
          },
          emphasis: {
            lineStyle: {
              width: 4,
              opacity: 1
            }
          }
        })),
        categories: [
          { name: '核心课程', itemStyle: { color: '#1565c0' } },
          { name: '章节模块', itemStyle: { color: '#1976d2' } },
          { name: '核心知识点', itemStyle: { color: '#1e88e5' } },
          { name: '细分知识点', itemStyle: { color: '#42a5f5' } }
        ],
        roam: true,
        draggable: true,
        dragMode: ['pan', 'node'],
        focusNodeAdjacency: true,
        cursor: 'grab',
        emphasis: {
          cursor: 'grabbing',
          focus: 'adjacency',
          lineStyle: {
            width: 4
          }
        },
        scaleLimit: {
          min: 0.1,
          max: 10
        },
        force: {
          initLayout: 'circular',
          repulsion: 3000,
          edgeLength: [150, 300],
          gravity: 0.05,
          friction: 0.9,
          layoutAnimation: true,
          edgeLengthMin: 100,
          edgeLengthMax: 400
        }
      }
    ]
  }
  
  // 使用 true 参数强制重新渲染图表
  chart.setOption(option, true)
  
  // 如果提供了点击处理函数，重新绑定点击事件
  if (handleNodeClick) {
    // 先解绑旧的事件处理函数
    if (chart._clickHandler) {
      chart.off('click', chart._clickHandler)
    }
    
    // 绑定新的点击事件
    const clickHandler = (params) => {
      if (params.dataType === 'node') {
        handleNodeClick(params.data.name)
      }
    }
    
    chart.on('click', clickHandler)
    chart._clickHandler = clickHandler
  }
}

/**
 * 切换视图模式
 * @param {Object} chart - 图表实例
 * @param {string} viewMode - 视图模式
 * @param {Object} currentGraphData - 当前图谱数据
 */
export const toggleViewMode = (chart, viewMode, currentGraphData) => {
  if (!chart) return
  
  const option = chart.getOption()
  
  // 检查option和series是否存在
  if (!option || !option.series || option.series.length === 0) {
    return
  }
  
  const series = option.series[0]
  
  if (viewMode === 'simplified') {
    // 简化视图：只显示核心课程和章节模块
    series.data = series.data.map(node => ({
      ...node,
      symbolSize: node.category <= 1 ? (node.category === 0 ? 90 : 70) : 0,
      label: {
        ...node.label,
        show: node.category <= 1,
        fontSize: node.category === 0 ? 18 : 16,
        fontWeight: 'bold'
      },
      itemStyle: {
        ...node.itemStyle,
        opacity: node.category <= 1 ? 1 : 0
      }
    }))
    
    // 隐藏与三级及以下节点相关的连接线
    series.links = series.links.filter(link => {
      const sourceNode = series.data.find(n => n.id === link.source)
      const targetNode = series.data.find(n => n.id === link.target)
      return sourceNode && targetNode && sourceNode.category <= 1 && targetNode.category <= 1
    })
    
    // 调整简化视图的力导向布局参数
    series.force = {
      ...series.force,
      repulsion: 15000,
      edgeLength: [200, 350],
      gravity: 0.01
    }
  } else {
    // 标准视图：显示所有节点
    series.data = series.data.map(node => ({
      ...node,
      symbolSize: node.category === 0 ? 80 : node.category === 1 ? 60 : node.category === 2 ? 45 : 35,
      label: {
        ...node.label,
        show: true,
        fontSize: node.category === 0 ? 16 : node.category === 1 ? 14 : 12,
        fontWeight: node.category <= 1 ? 'bold' : 'normal'
      },
      itemStyle: {
        ...node.itemStyle,
        opacity: 1
      }
    }))
    
    // 恢复所有连接线
    series.links = currentGraphData.links.map(link => ({
      ...link,
      lineStyle: {
        color: getLinkColor(link.source, currentGraphData.nodes),
        width: getLinkWidth(link.source, currentGraphData.nodes),
        curveness: 0.2,
        type: getLinkType(link.source, currentGraphData.nodes),
        opacity: 0.7
      }
    }))
    
    // 恢复标准视图的力导向布局参数
    series.force = {
      initLayout: 'circular',
      repulsion: 3000,
      edgeLength: [150, 300],
      gravity: 0.05,
      friction: 0.9,
      layoutAnimation: true,
      edgeLengthMin: 100,
      edgeLengthMax: 400
    }
  }
  
  chart.setOption(option)
}

/**
 * 高亮关联节点
 * @param {Object} chart - 图表实例
 * @param {string} nodeName - 节点名称
 * @param {Object} currentGraphData - 当前图谱数据
 */
export const highlightRelatedNodes = (chart, nodeName, currentGraphData) => {
  if (!chart) return
  
  // 从当前图谱数据中动态获取关联节点
  const targetNode = currentGraphData.nodes.find(n => n.name === nodeName)
  if (!targetNode) return
  
  // 找到所有直接关联的节点
  const relatedNodeIds = new Set()
  currentGraphData.links.forEach(link => {
    if (link.source == targetNode.id) {
      relatedNodeIds.add(link.target)
    } else if (link.target == targetNode.id) {
      relatedNodeIds.add(link.source)
    }
  })
  
  // 获取关联节点的名称
  const relatedNames = currentGraphData.nodes
    .filter(n => relatedNodeIds.has(n.id))
    .map(n => n.name)
  
  const option = chart.getOption()
  
  // 检查option和series是否存在
  if (!option || !option.series || option.series.length === 0) {
    return
  }
  
  const series = option.series[0]
  
  // 更新节点样式
  series.data = series.data.map(node => {
    const isRelated = relatedNames.includes(node.name) || node.name === nodeName
    const isSelected = node.name === nodeName
    return {
      ...node,
      itemStyle: {
        color: isRelated ? getNodeColor(node.category) : getNodeColor(node.category),
        opacity: isRelated ? 1 : 0.85,
        borderColor: isSelected ? '#ff6b6b' : 'transparent',
        borderWidth: isSelected ? 3 : 0
      },
      label: {
        ...node.label,
        opacity: 1,
        color: isRelated ? '#fff' : '#333'
      }
    }
  })
  
  // 更新连接线样式
  series.links = series.links.map(link => {
    const sourceNode = series.data.find(n => n.id === link.source)
    const targetNode = series.data.find(n => n.id === link.target)
    const isRelated = sourceNode?.itemStyle?.opacity === 1 && targetNode?.itemStyle?.opacity === 1
    
    return {
      ...link,
      lineStyle: {
        ...link.lineStyle,
        opacity: isRelated ? 1 : 0.3
      }
    }
  })
  
  chart.setOption(option)
}

/**
 * 清除高亮
 * @param {Object} chart - 图表实例
 * @param {Object} currentGraphData - 当前图谱数据
 */
export const clearHighlights = (chart, currentGraphData) => {
  if (!chart) return
  
  const option = chart.getOption()
  
  // 检查option和series是否存在
  if (!option || !option.series || option.series.length === 0) {
    return
  }
  
  const series = option.series[0]
  
  // 恢复所有节点
  series.data = series.data.map(node => ({
    ...node,
    itemStyle: {
      color: getNodeColor(node.category),
      opacity: 1
    },
    label: {
      ...node.label,
      opacity: 1
    }
  }))
  
  // 恢复所有连接线
  series.links = currentGraphData.links.map(link => ({
    ...link,
    lineStyle: {
      color: getLinkColor(link.source, currentGraphData.nodes),
      width: getLinkWidth(link.source, currentGraphData.nodes),
      curveness: 0.2,
      type: getLinkType(link.source, currentGraphData.nodes),
      opacity: 0.7
    }
  }))
  
  chart.setOption(option)
}

/**
 * 高亮搜索结果
 * @param {Object} chart - 图表实例
 * @param {Array} searchResults - 搜索结果列表
 */
export const highlightSearchResults = (chart, searchResults) => {
  if (!chart || searchResults.length === 0) return
  
  const resultNames = searchResults.map(r => r.name)
  
  const option = chart.getOption()
  const series = option.series[0]
  
  series.data = series.data.map(node => {
    const isMatch = resultNames.includes(node.name)
    return {
      ...node,
      itemStyle: {
        color: isMatch ? '#ff6b6b' : getNodeColor(node.category),
        opacity: isMatch ? 1 : 0.3
      },
      label: {
        ...node.label,
        opacity: isMatch ? 1 : 0.3
      }
    }
  })
  
  chart.setOption(option)
}
