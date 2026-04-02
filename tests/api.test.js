import { apiService } from '../src/api/api.js'

const BASE_URL = 'http://localhost:8080/api'

const testResults = {
  passed: 0,
  failed: 0,
  errors: []
}

function log(message, type = 'info') {
  const timestamp = new Date().toISOString()
  const prefix = type === 'success' ? '✓' : type === 'error' ? '✗' : '→'
  console.log(`[${timestamp}] ${prefix} ${message}`)
}

function assert(condition, message) {
  if (condition) {
    testResults.passed++
    log(message, 'success')
    return true
  } else {
    testResults.failed++
    testResults.errors.push(message)
    log(message, 'error')
    return false
  }
}

async function testSaveProcessedText() {
  log('测试保存处理后的文本接口...')
  
  try {
    const data = {
      question: '什么是微积分？',
      processedText: '微积分是数学的一个分支，主要研究函数的极限、微分、积分以及无穷级数等概念。',
      subject: '数学'
    }
    
    const response = await apiService.qa.saveProcessedText(data)
    
    assert(response !== undefined, '保存处理后的文本 - 响应存在')
    assert(response.code === 200 || response.code === undefined, '保存处理后的文本 - 状态码正确')
    
    log('保存处理后的文本测试完成')
    return true
  } catch (error) {
    log(`保存处理后的文本测试失败: ${error.message}`, 'error')
    testResults.errors.push(`保存处理后的文本: ${error.message}`)
    return false
  }
}

async function testGetRecommendedCourses() {
  log('测试获取推荐课程接口...')
  
  try {
    const response = await apiService.qa.getRecommendedCourses(5)
    
    assert(response !== undefined, '获取推荐课程 - 响应存在')
    assert(Array.isArray(response) || (response.data && Array.isArray(response.data)), '获取推荐课程 - 返回数组格式')
    
    if (Array.isArray(response) && response.length > 0) {
      const course = response[0]
      assert(course.id !== undefined, '推荐课程包含 id 字段')
      assert(course.name !== undefined, '推荐课程包含 name 字段')
      assert(course.videoLink !== undefined, '推荐课程包含 videoLink 字段')
    }
    
    log('获取推荐课程测试完成')
    return true
  } catch (error) {
    log(`获取推荐课程测试失败: ${error.message}`, 'error')
    testResults.errors.push(`获取推荐课程: ${error.message}`)
    return false
  }
}

async function testGetKnowledgeGraph() {
  log('测试获取知识图谱接口...')
  
  try {
    const bookId = 1
    const response = await apiService.knowledgeGraph.getKnowledgeGraphByBook(bookId)
    
    assert(response !== undefined, '获取知识图谱 - 响应存在')
    assert(response.nodes !== undefined, '知识图谱包含 nodes 字段')
    assert(response.relations !== undefined, '知识图谱包含 relations 字段')
    
    if (response.nodes && response.nodes.length > 0) {
      const node = response.nodes[0]
      assert(node.id !== undefined, '知识节点包含 id 字段')
      assert(node.name !== undefined, '知识节点包含 name 字段')
      assert(node.category !== undefined, '知识节点包含 category 字段')
      assert(node.symbolSize !== undefined, '知识节点包含 symbolSize 字段')
    }
    
    if (response.relations && response.relations.length > 0) {
      const relation = response.relations[0]
      assert(relation.sourceNodeId !== undefined || relation.source !== undefined, '知识关系包含 source 字段')
      assert(relation.targetNodeId !== undefined || relation.target !== undefined, '知识关系包含 target 字段')
    }
    
    log('获取知识图谱测试完成')
    return true
  } catch (error) {
    log(`获取知识图谱测试失败: ${error.message}`, 'error')
    testResults.errors.push(`获取知识图谱: ${error.message}`)
    return false
  }
}

async function testGetHistory() {
  log('测试获取历史记录接口...')
  
  try {
    const response = await apiService.qa.getHistory()
    
    assert(response !== undefined, '获取历史记录 - 响应存在')
    assert(Array.isArray(response) || (response.data && Array.isArray(response.data)), '获取历史记录 - 返回数组格式')
    
    log('获取历史记录测试完成')
    return true
  } catch (error) {
    log(`获取历史记录测试失败: ${error.message}`, 'error')
    testResults.errors.push(`获取历史记录: ${error.message}`)
    return false
  }
}

async function testGetCourses() {
  log('测试获取课程列表接口...')
  
  try {
    const response = await apiService.qa.getCourses()
    
    assert(response !== undefined, '获取课程列表 - 响应存在')
    assert(Array.isArray(response) || (response.data && Array.isArray(response.data)), '获取课程列表 - 返回数组格式')
    
    log('获取课程列表测试完成')
    return true
  } catch (error) {
    log(`获取课程列表测试失败: ${error.message}`, 'error')
    testResults.errors.push(`获取课程列表: ${error.message}`)
    return false
  }
}

async function runAllTests() {
  console.log('\n========================================')
  console.log('开始执行 API 集成测试')
  console.log('========================================\n')
  
  const tests = [
    { name: '保存处理后的文本', fn: testSaveProcessedText },
    { name: '获取推荐课程', fn: testGetRecommendedCourses },
    { name: '获取知识图谱', fn: testGetKnowledgeGraph },
    { name: '获取历史记录', fn: testGetHistory },
    { name: '获取课程列表', fn: testGetCourses }
  ]
  
  for (const test of tests) {
    console.log(`\n----- ${test.name} -----`)
    await test.fn()
  }
  
  console.log('\n========================================')
  console.log('测试结果汇总')
  console.log('========================================')
  console.log(`通过: ${testResults.passed}`)
  console.log(`失败: ${testResults.failed}`)
  console.log(`总计: ${testResults.passed + testResults.failed}`)
  
  if (testResults.errors.length > 0) {
    console.log('\n失败的测试:')
    testResults.errors.forEach((error, index) => {
      console.log(`${index + 1}. ${error}`)
    })
  }
  
  console.log('\n')
  
  return testResults.failed === 0
}

runAllTests().then(success => {
  process.exit(success ? 0 : 1)
})
