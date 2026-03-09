<template>
  <div class="qa-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <button class="back-home-btn" @click="goHome">
          <span class="back-icon">←</span>
          返回首页
        </button>
      </div>
      <h1 class="page-title">问答中心</h1>
      <div class="subtitle">核心功能模块</div>
    </div>

    <!-- 导航标签 -->
    <div class="tab-navigation">
      <div class="tab" :class="{ active: activeTab === '智能答疑' }" @click="activeTab = '智能答疑'">
        智能答疑
      </div>
      <div class="tab" :class="{ active: activeTab === '考研/考证问答' }" @click="activeTab = '考研/考证问答'">
        考研/考证问答
      </div>
    </div>

    <!-- 智能答疑内容（合并了作业答疑和专业课问答） -->
    <div v-if="activeTab === '智能答疑'" class="professional-qa">
      <div class="qa-container">
        <!-- 左侧：教材选择器 -->
        <div class="left-panel">
          <div class="panel-title">教材选择器</div>
          <!-- 搜索框 -->
          <div class="search-box">
            <input 
              type="text" 
              v-model="searchQuery"
              placeholder="搜索教材..."
              class="search-input"
            >
            <span class="search-icon">🔍</span>
          </div>
          <div class="book-selector">
            <div 
              v-for="(book, index) in books" 
              :key="index"
              class="book-item"
              :class="{ active: selectedBook === book.name }"
              @click="selectBook(book.name)"
            >
              <div class="book-icon">
                <div class="book-cover"></div>
              </div>
              <div class="book-info">
                <div class="book-name">{{ book.name }}</div>
                <div class="book-list">{{ book.author }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间：章节导航 -->
        <div class="middle-panel">
          <div class="panel-title">章节导航</div>
          <div class="chapter-nav">
            <div 
              v-for="chapter in chapters" 
              :key="chapter?.id || Math.random()"
              v-if="chapter"
              class="nav-item"
              :class="{ active: chapter.id === 2, expanded: expandedChapters.includes(chapter.id) }"
              @click="toggleChapter(chapter.id)"
            >
              <span class="nav-title">{{ chapter.title }}</span>
              <span class="nav-arrow">{{ chapter.id && expandedChapters.includes(chapter.id) ? '▼' : (chapter.children && chapter.children.length > 0 ? '>>' : '>') }}</span>
            </div>
            <!-- 子章节 -->
            <div v-for="chapter in chapters" :key="`sub-${chapter?.id || Math.random()}`" v-if="chapter && expandedChapters.includes(chapter.id) && chapter.children && chapter.children.length > 0">
              <div 
                v-for="subChapter in chapter.children" 
                :key="subChapter.id"
                class="sub-nav-item"
              >
                <span class="sub-nav-title">{{ subChapter.title }}</span>
                <span class="sub-nav-arrow">></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：答疑区域（支持文字和图片） -->
        <div class="right-panel">
          <div class="panel-title">答疑区域</div>
          
          <!-- 问题类型切换 -->
          <div class="question-type-tabs">
            <div 
              class="type-tab" 
              :class="{ active: questionType === 'text' }" 
              @click="questionType = 'text'"
            >
              文字提问
            </div>
            <div 
              class="type-tab" 
              :class="{ active: questionType === 'image' }" 
              @click="questionType = 'image'"
            >
              图片上传
            </div>
          </div>

          <!-- 文字提问区域 -->
          <div v-if="questionType === 'text'" class="question-area">
            <textarea 
              v-model="question"
              placeholder="请输入您的问题..."
              class="question-input"
              rows="3"
            ></textarea>
            <button class="submit-btn" @click="submitQuestion">
              提交问题
            </button>
          </div>

          <!-- 图片上传区域 -->
          <div v-else class="image-upload-area">
            <div class="upload-box" @click="triggerFileUpload">
              <input 
                type="file" 
                ref="fileInput" 
                style="display: none;" 
                accept="image/*"
                @change="handleFileChange"
              >
              <div class="upload-placeholder">
                <span class="upload-icon">📷</span>
                <span class="upload-text">点击上传作业/教材图片</span>
              </div>
            </div>
            <div v-if="uploadedImage" class="uploaded-preview">
              <div class="preview-container">
                <img :src="uploadedImage" alt="上传的图片" class="preview-image">
                <button class="delete-btn" @click="deleteImage">
                  ×
                </button>
              </div>
              <div class="image-question-input">
                <textarea 
                  v-model="imageQuestion"
                  placeholder="请描述您的问题..."
                  class="question-input"
                  rows="2"
                ></textarea>
              </div>
              <button class="submit-btn" @click="submitImageQuestion">
                提交问题
              </button>
            </div>
          </div>

          <!-- AI回答区域 -->
          <div v-if="showAnswer" class="answer-area">
            <div class="answer-header">
              <span class="answer-title">AI回答</span>
            </div>
            <div class="answer-content">
              <div class="ai-avatar">
                <div class="avatar-placeholder">
                  <span class="avatar-text">AI</span>
                </div>
              </div>
              <div class="answer-text">
                {{ aiAnswer }}
              </div>
            </div>
          </div>

          <!-- 历史回答 -->
          <div class="history-section">
            <div class="history-title">历史答疑记录</div>
            <div 
              v-for="(item, index) in historyQuestions" 
              :key="index"
              class="history-item"
              @click="selectHistory(item)"
            >
              <div class="history-question">{{ item.question }}</div>
              <div class="history-answer">{{ item.answer.replace(/\n/g, ' ').substring(0, 100) }}...</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部：网课推荐 -->
      <div class="course-recommendation">
        <div class="recommendation-title">网课推荐</div>
        <div class="course-grid">
          <div 
            v-for="course in courses" 
            :key="course.id"
            class="course-item" 
            @click="openCourseLink(course.videoLink)"
          >
            <div class="course-avatar">
              <div class="avatar-placeholder">
                <span class="avatar-text">讲</span>
              </div>
            </div>
            <div class="course-info">
              <div class="course-name">{{ course.name }}</div>
              <div class="course-desc">{{ course.desc }}</div>
              <div class="course-code">{{ course.code }}</div>
            </div>
            <div class="course-link-icon">🔗</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 考研/考证问答内容 -->
    <div v-else-if="activeTab === '考研/考证问答'" class="exam-qa-page">
      <div class="exam-qa-container">
        <!-- 左侧栏 -->
        <div class="exam-left-column">
          <!-- 考试科目选择 -->
          <div class="exam-panel">
            <div class="exam-panel-title">考试科目选择</div>
            <div class="exam-subject-select">
              <div class="searchable-select">
                <input 
                  type="text" 
                  v-model="subjectSearchQuery"
                  placeholder="搜索考试科目..."
                  class="select-search-input"
                >
                <div class="select-dropdown" v-show="showSubjectDropdown">
                  <div 
                    v-for="subject in filteredSubjects" 
                    :key="subject.id"
                    class="select-option"
                    :class="{ active: selectedExamSubject === subject.id }"
                    @click="selectSubject(subject)"
                  >
                    {{ subject.name }}
                  </div>
                </div>
                <div class="select-display" @click="toggleSubjectDropdown">
                  {{ getSelectedSubjectName() }}
                  <span class="select-arrow">▼</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 高频考点总结区 -->
          <div class="exam-panel">
            <div class="exam-panel-title">高频考点总结区</div>
            <div class="exam-keypoints-list">
              <div 
                v-for="(point, index) in keyPoints" 
                :key="index"
                class="exam-keypoint-item"
                :class="{ highlight: selectedKeyPoint === point }"
                @click="selectKeyPoint(point)"
              >
                <span class="keypoint-label">考点{{ index + 1 }}:</span>
                <span class="keypoint-name">{{ point.name }}</span>
              </div>
            </div>
          </div>

          <!-- 真题推荐 -->
          <div class="exam-panel">
            <div class="exam-panel-title">真题推荐</div>
            <div class="exam-papers-container">
              <div 
                v-for="(paper, index) in selectedPapers" 
                :key="index"
                class="exam-paper-item"
              >
                <div class="paper-title">{{ paper.year }}年真题</div>
                <div class="paper-content">
                  <div class="paper-image">
                    <img src="https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=100&h=100&fit=crop" alt="真题">
                  </div>
                  <div class="paper-info">
                    <div class="paper-name">{{ paper.year }}年真题推荐</div>
                    <div class="paper-tags">
                      <span class="paper-tag">{{ paper.subject }}</span>
                      <span class="paper-tag">{{ paper.difficulty }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间栏 -->
        <div class="exam-middle-column">
          <!-- 考点提问区 -->
          <div class="exam-panel">
            <div class="exam-panel-title">考点提问区</div>
            <div class="exam-question-area">
              <input 
                v-model="examQuestion"
                type="text"
                placeholder="问题问传"
                class="exam-question-input"
              >
              <button class="exam-submit-btn" @click="submitExamQuestion">
                提交问题
              </button>
            </div>
            <!-- 回答区域 -->
            <div class="exam-question-display">
              <div class="exam-question-label">回答区域</div>
              <div class="exam-answer-content">
                <div class="ai-answer-box">
                  <div class="ai-answer-header">
                    <span class="ai-avatar">AI</span>
                    <span class="ai-label">智能回答</span>
                  </div>
                  <div class="ai-answer-text" v-if="examAnswer">
                    <p>根据您的问题，我为您提供以下解答：</p>
                    <div v-html="formatAnswer(examAnswer)"></div>
                  </div>
                  <div class="ai-answer-text" v-else>
                    <p>请在上方输入问题并点击提交，AI将为您生成详细解答。</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 网课推荐 -->
          <div class="exam-panel">
            <div class="exam-panel-title">网课推荐</div>
            <div class="exam-courses-list">
              <div 
                v-for="(course, index) in examCourses" 
                :key="index"
                class="exam-course-item"
                @click="openCourseLink(course.videoLink)"
              >
                <div class="course-avatar">
                  <div class="avatar-placeholder">
                    <span class="avatar-text">讲</span>
                  </div>
                </div>
                <div class="course-info">
                  <div class="course-name">{{ course.name }}</div>
                  <div class="course-desc">{{ course.desc }}</div>
                  <div class="course-code">{{ course.code }}</div>
                </div>
                <div class="course-link-icon">🔗</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧栏 -->
        <div class="exam-right-column">
          <!-- 图片上传区域 -->
          <div class="exam-panel">
            <div class="exam-panel-title">图片上传区域</div>
            <div class="exam-upload-area" @click="triggerExamFileUpload">
              <input 
                type="file" 
                ref="examFileInput" 
                style="display: none;" 
                accept="image/*"
                @change="handleExamFileChange"
              >
              <div class="exam-upload-placeholder">
                <span class="exam-upload-text">拖拽或点击上传图片</span>
              </div>
            </div>
            <div v-if="examUploadedImage" class="exam-uploaded-preview">
              <div class="preview-container">
                <img :src="examUploadedImage" alt="上传的图片" class="exam-preview-image">
                <button class="delete-btn" @click="deleteExamImage">
                  ×
                </button>
              </div>
              <div class="exam-image-question-input">
                <textarea 
                  v-model="examImageQuestion"
                  placeholder="请描述您的问题..."
                  class="exam-question-input"
                  rows="2"
                ></textarea>
              </div>
              <button class="exam-upload-btn" @click="submitExamImageQuestion">
                提交问题
              </button>
            </div>
            <button v-else class="exam-upload-btn" @click="triggerExamFileUpload">
              图片上传
            </button>
          </div>

          <!-- 备考答疑历史记录 -->
          <div class="exam-panel">
            <div class="exam-panel-title">备考答疑历史记录</div>
            <div class="exam-history-list">
              <div 
                v-for="(record, index) in examHistory" 
                :key="index"
                class="exam-history-item"
                @click="selectExamHistory(record)"
              >
                <div class="exam-history-question">Q: {{ record.question }}</div>
                <div class="exam-history-answer">A: {{ record.answer }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 激活的标签页
const activeTab = ref('智能答疑')

// 问题类型（text: 文字提问, image: 图片上传）
const questionType = ref('text')

// 文件输入引用
const fileInput = ref(null)

// 上传的图片
const uploadedImage = ref(null)
// 图片问题描述
const imageQuestion = ref('')
// 搜索查询
const searchQuery = ref('')
// 当前选中的书籍
const selectedBook = ref(null)
// 书籍数据
const books = ref([
  { name: '高等数学', author: '同济大学数学系' },
  { name: '大学物理', author: '张三' },
  { name: '计算机基础', author: '李四' },
  { name: '英语', author: '王五' }
])
// 章节数据（模拟数据，后续由后端提供）
const chapters = ref([
  { id: 1, title: '书架导航', children: [] },
  { id: 2, title: '书架教程导航', children: [
    { id: 21, title: '教程介绍' },
    { id: 22, title: '基本操作' },
    { id: 23, title: '高级功能' }
  ]},
  { id: 3, title: '书架教程章项', children: [] },
  { id: 4, title: '书籍层级项', children: [] },
  { id: 5, title: '书架有课程', children: [] },
  { id: 6, title: '视觉层次', children: [] },
  { id: 7, title: '视觉教程项', children: [] },
  { id: 8, title: '我創信問链', children: [] }
])

// 展开的章节
const expandedChapters = ref([])

// 模拟不同书籍的章节数据
const bookChaptersMap = {
  '高等数学': [
    { id: 1, title: '第一章 函数与极限', children: [
      { id: 11, title: '1.1 函数' },
      { id: 12, title: '1.2 极限' },
      { id: 13, title: '1.3 连续函数' }
    ]},
    { id: 2, title: '第二章 导数与微分', children: [
      { id: 21, title: '2.1 导数的概念' },
      { id: 22, title: '2.2 求导法则' },
      { id: 23, title: '2.3 微分' }
    ]},
    { id: 3, title: '第三章 微分中值定理', children: [] }
  ],
  '大学物理': [
    { id: 1, title: '第一章 力学基础', children: [] },
    { id: 2, title: '第二章 热学', children: [] },
    { id: 3, title: '第三章 电磁学', children: [] }
  ],
  '计算机基础': [
    { id: 1, title: '第一章 计算机概述', children: [] },
    { id: 2, title: '第二章 操作系统', children: [] },
    { id: 3, title: '第三章 网络基础', children: [] }
  ]
}

// 返回首页
const goHome = () => {
  router.push('/')
}

// 选择书籍
const selectBook = (bookName) => {
  selectedBook.value = bookName
  // 重置展开状态
  expandedChapters.value = []
  // 根据选中的书籍更新章节数据
  if (bookChaptersMap[bookName]) {
    chapters.value = bookChaptersMap[bookName]
  } else {
    // 默认章节数据
    chapters.value = [
      { id: 1, title: '书架导航', children: [] },
      { id: 2, title: '书架教程导航', children: [] },
      { id: 3, title: '书架教程章项', children: [] },
      { id: 4, title: '书籍层级项', children: [] },
      { id: 5, title: '书架有课程', children: [] },
      { id: 6, title: '视觉层次', children: [] },
      { id: 7, title: '视觉教程项', children: [] },
      { id: 8, title: '我創信問链', children: [] }
    ]
  }
}

// 切换章节展开/折叠
const toggleChapter = (chapterId) => {
  const index = expandedChapters.value.indexOf(chapterId)
  if (index > -1) {
    // 折叠章节
    expandedChapters.value.splice(index, 1)
  } else {
    // 展开章节
    expandedChapters.value.push(chapterId)
  }
}

// 问答相关
const question = ref('')
const aiAnswer = ref('')
const showAnswer = ref(false)
const historyQuestions = ref([
  { question: '什么是函数的极限？', answer: '函数的极限是指当自变量趋近于某个值时，函数值趋近于的一个确定的数值...' },
  { question: '如何求导？', answer: '求导的基本方法包括使用导数的定义、基本导数公式和求导法则...' },
  { question: '操作系统的基本功能是什么？', answer: '操作系统的基本功能包括进程管理、内存管理、文件系统管理和设备管理...' }
])

// 提交问题
const submitQuestion = () => {
  if (!question.value.trim()) return
  
  // 模拟AI回答
  const answer = `根据${selectedBook.value || '当前教材'}的内容，关于"${question.value}"的回答如下：\n\n这是一个基于AI的回答示例。在实际应用中，这里会结合教材内容和AI模型生成详细的回答。\n\n回答会包括：\n1. 问题的核心概念\n2. 相关的知识点\n3. 详细的解释和示例\n4. 可能的应用场景`
  aiAnswer.value = answer
  
  // 添加到历史记录（包含问题和答案）
  historyQuestions.value.unshift({ question: question.value, answer: answer })
  if (historyQuestions.value.length > 5) {
    historyQuestions.value.pop()
  }
  
  // 显示回答
  showAnswer.value = true
  
  // 清空输入框
  question.value = ''
}

// 选择历史问题
const selectHistory = (item) => {
  question.value = item.question
  aiAnswer.value = item.answer
  showAnswer.value = true
}

// 课程数据（后续由后端提供）
const courses = ref([
  { 
    id: 1, 
    name: '网课推荐', 
    desc: '综合推荐 — 待', 
    code: '#36CFCC',
    videoLink: 'https://example.com/video1' // 示例链接，后续由后端提供
  },
  { 
    id: 2, 
    name: '网课推荐', 
    desc: '综合推荐 — 待', 
    code: '#36CFCC',
    videoLink: 'https://example.com/video2' // 示例链接，后续由后端提供
  }
])

// 打开课程链接
const openCourseLink = (link) => {
  if (link) {
    window.open(link, '_blank')
  }
}

// 触发文件上传
const triggerFileUpload = () => {
  fileInput.value?.click()
}

// 处理文件选择
const handleFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    // 创建图片预览
    const reader = new FileReader()
    reader.onload = (e) => {
      uploadedImage.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

// 提交图片问题
const submitImageQuestion = () => {
  if (!uploadedImage.value) return
  
  // 模拟AI回答
  const questionText = imageQuestion.value || '基于上传的图片'
  const answer = `根据上传的图片内容和您的问题"${questionText}"，AI正在分析...

这是一个基于图片和问题的AI回答示例。在实际应用中，这里会结合图片内容、用户问题和AI模型生成详细的回答。

分析结果：
1. 识别图片中的问题
2. 理解用户的具体疑问
3. 提取关键信息
4. 生成详细的解答步骤
5. 提供相关的知识点说明`
  aiAnswer.value = answer
  
  // 添加到历史记录
  historyQuestions.value.unshift({ question: `[图片提问] ${questionText}`, answer: answer })
  if (historyQuestions.value.length > 5) {
    historyQuestions.value.pop()
  }
  
  // 显示回答
  showAnswer.value = true
  
  // 清空上传的图片和问题
  uploadedImage.value = null
  imageQuestion.value = ''
}

// 删除上传的图片
const deleteImage = () => {
  uploadedImage.value = null
  imageQuestion.value = ''
}

// ==================== 考研/考证问答相关 ====================
// 考试科目
const examSubjects = ref([
  { id: 'builder', name: '一级建造师' },
  { id: 'cpa', name: '注册会计师' },
  { id: 'teacher', name: '教师资格证' },
  { id: 'postgraduate', name: '考研英语' },
  { id: 'civil', name: '公务员考试' }
])

// 选中的考试科目
const selectedExamSubject = ref('builder')

// 科目搜索
const subjectSearchQuery = ref('')

// 显示科目下拉菜单
const showSubjectDropdown = ref(false)

// 过滤后的科目
const filteredSubjects = computed(() => {
  if (!subjectSearchQuery.value) {
    return examSubjects.value
  }
  const query = subjectSearchQuery.value.toLowerCase()
  return examSubjects.value.filter(subject => 
    subject.name.toLowerCase().includes(query)
  )
})

// 选择科目
const selectSubject = (subject) => {
  selectedExamSubject.value = subject.id
  subjectSearchQuery.value = ''
  showSubjectDropdown.value = false
  // 更新高频考点
  keyPoints.value = subjectKeyPointsMap.value[subject.id] || []
  // 重置选中的考点
  selectedKeyPoint.value = null
}

// 切换科目下拉菜单
const toggleSubjectDropdown = () => {
  showSubjectDropdown.value = !showSubjectDropdown.value
}

// 获取选中的科目名称
const getSelectedSubjectName = () => {
  const subject = examSubjects.value.find(s => s.id === selectedExamSubject.value)
  return subject ? subject.name : '请选择科目'
}

// 科目对应的高频考点
const subjectKeyPointsMap = ref({
  'builder': [
    { name: '工程经济' },
    { name: '项目管理' },
    { name: '法律法规' },
    { name: '专业实务' }
  ],
  'cpa': [
    { name: '会计' },
    { name: '审计' },
    { name: '财务成本管理' },
    { name: '经济法' },
    { name: '税法' },
    { name: '公司战略与风险管理' }
  ],
  'teacher': [
    { name: '综合素质' },
    { name: '教育知识与能力' },
    { name: '学科知识与教学能力' }
  ],
  'postgraduate': [
    { name: '词汇' },
    { name: '语法' },
    { name: '阅读理解' },
    { name: '写作' }
  ],
  'civil': [
    { name: '行政职业能力测验' },
    { name: '申论' },
    { name: '专业科目' }
  ]
})

// 高频考点
const keyPoints = ref(subjectKeyPointsMap.value['builder'])

// 选中的考点
const selectedKeyPoint = ref(null)

// 考点提问
const examQuestion = ref('')

// AI回答
const examAnswer = ref('')

// 选中的真题
const selectedPapers = ref([
  { year: '2024', subject: '工程经济', difficulty: '中等难度' },
  { year: '2023', subject: '项目管理', difficulty: '较难' },
  { year: '2022', subject: '法律法规', difficulty: '简单' },
  { year: '2021', subject: '专业实务', difficulty: '中等难度' },
  { year: '2020', subject: '工程经济', difficulty: '较难' }
])

// 网课推荐数据
const examCourses = ref([
  { 
    id: 1, 
    name: '一级建造师实务精讲', 
    desc: '专业实务模块 - 考点全覆盖', 
    code: '#36CFCC',
    videoLink: 'https://example.com/video1' 
  },
  { 
    id: 2, 
    name: '注册会计师会计基础', 
    desc: '会计科目 - 基础理论讲解', 
    code: '#36CFCC',
    videoLink: 'https://example.com/video2' 
  },
  { 
    id: 3, 
    name: '教师资格证综合素质', 
    desc: '综合素质模块 - 重点解析', 
    code: '#36CFCC',
    videoLink: 'https://example.com/video3' 
  },
  { 
    id: 4, 
    name: '考研英语词汇精讲', 
    desc: '词汇模块 - 高频词汇记忆', 
    code: '#36CFCC',
    videoLink: 'https://example.com/video4' 
  },
  { 
    id: 5, 
    name: '公务员申论写作', 
    desc: '申论模块 - 写作技巧提升', 
    code: '#36CFCC',
    videoLink: 'https://example.com/video5' 
  }
])

// 备考历史记录
const examHistory = ref([
  { question: '如何高效记忆公式？', answer: '建议分模块背诵' },
  { question: '项目管理的核心流程是什么？', answer: '启动、规划、执行、监控、收尾' },
  { question: '工程经济中的净现值怎么计算？', answer: 'NPV = ∑(CI-CO)/(1+i)^t' }
])

// 考试文件上传相关
const examFileInput = ref(null)
// 考试上传的图片
const examUploadedImage = ref(null)
// 考试图片问题描述
const examImageQuestion = ref('')

// 触发考试文件上传
const triggerExamFileUpload = () => {
  examFileInput.value?.click()
}

// 处理考试文件选择
const handleExamFileChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    // 创建图片预览
    const reader = new FileReader()
    reader.onload = (e) => {
      examUploadedImage.value = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

// 提交考试图片问题
const submitExamImageQuestion = () => {
  if (!examUploadedImage.value) return
  
  // 模拟AI回答
  const questionText = examImageQuestion.value || '基于上传的图片'
  const answer = `根据上传的图片内容和您的问题"${questionText}"，AI正在分析...

这是一个基于图片和问题的AI回答示例。在实际应用中，这里会结合图片内容、用户问题和AI模型生成详细的回答。

分析结果：
1. 识别图片中的考点
2. 理解用户的具体疑问
3. 提取关键信息
4. 生成详细的解答步骤
5. 提供相关的知识点说明`
  examAnswer.value = answer
  
  // 添加到历史记录
  examHistory.value.unshift({ question: `[图片提问] ${questionText}`, answer: answer })
  if (examHistory.value.length > 5) {
    examHistory.value.pop()
  }
  
  // 清空上传的图片和问题
  examUploadedImage.value = null
  examImageQuestion.value = ''
}

// 删除考试上传的图片
const deleteExamImage = () => {
  examUploadedImage.value = null
  examImageQuestion.value = ''
}

// 提交考点问题
const submitExamQuestion = () => {
  if (!examQuestion.value.trim()) return
  
  // 模拟AI回答（实际应用中会调用后端API）
  const answer = `这是关于"${examQuestion.value}"的详细解答：

**核心知识点：**
- 知识点1：相关概念和定义
- 知识点2：重要公式和定理
- 知识点3：解题方法和技巧

**详细解析：**
这里将根据具体问题生成详细的解答内容，包括步骤分解、公式推导、实例分析等。

**答题技巧：**
1. 仔细审题，理解问题要求
2. 回忆相关知识点和公式
3. 分步骤解答，确保逻辑清晰
4. 检查答案的正确性

**相关考点：**
- 考点1：相关联的其他知识点
- 考点2：常见的考试题型
- 考点3：易错点和注意事项`
  
  // 显示AI回答
  examAnswer.value = answer
  
  // 添加到历史记录
  examHistory.value.unshift({ 
    question: examQuestion.value, 
    answer: answer 
  })
  if (examHistory.value.length > 10) {
    examHistory.value.pop()
  }
  
  // 清空输入
  examQuestion.value = ''
  
  // 实际应用中，这里会调用后端API获取真实的AI回答
  // 例如：
  // const response = await fetch('/api/ai-answer', {
  //   method: 'POST',
  //   headers: { 'Content-Type': 'application/json' },
  //   body: JSON.stringify({ question: examQuestion.value, subject: selectedExamSubject.value })
  // })
  // const data = await response.json()
  // examAnswer.value = data.answer
}

// 选择考点
const selectKeyPoint = (point) => {
  selectedKeyPoint.value = point
  examQuestion.value = '请详细讲解：' + point.name
}

// 选择历史记录
const selectExamHistory = (record) => {
  examQuestion.value = record.question
}

// 格式化回答内容
const formatAnswer = (answer) => {
  if (!answer) return ''
  
  // 简单的Markdown转HTML
  return answer
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n- (.*?)(?=\n|$)/g, '<li>$1</li>')
    .replace(/\n(\d+\. )(.*?)(?=\n|$)/g, '<li>$1$2</li>')
    .replace(/(<li>.*?<\/li>)/gs, '<ul>$1</ul>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.qa-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 64px);
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
  position: relative;
}

.header-left {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
}

.back-home-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.back-home-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(59, 130, 246, 0.4);
  background: linear-gradient(90deg, #2563eb 0%, #1d4ed8 100%);
}

.back-icon {
  font-size: 16px;
  font-weight: 600;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 16px;
  color: #6b7280;
  margin: 0;
  text-align: center;
}

/* 搜索框样式 */
.search-box {
  position: relative;
  margin-bottom: 16px;
}

.search-input {
  width: 100%;
  padding: 12px 40px 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.search-icon {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 16px;
  color: #9ca3af;
  pointer-events: none;
}

/* 导航标签 */
.tab-navigation {
  display: flex;
  background: white;
  border-radius: 12px;
  padding: 8px;
  margin-bottom: 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.tab {
  flex: 1;
  padding: 12px 0;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  color: #6b7280;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab:hover {
  color: #3b82f6;
  background-color: #eff6ff;
}

.tab.active {
  color: white;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* 专业课问答容器 */
.qa-container {
  display: grid;
  grid-template-columns: 280px 320px 1fr;
  gap: 20px;
  margin-bottom: 40px;
}

/* 面板通用样式 */
.left-panel,
.middle-panel,
.right-panel {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #3b82f6;
}

/* 左侧：教材选择器 */
.book-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.book-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.book-item:hover {
  background-color: #f3f4f6;
}

.book-item.active {
  background-color: #dbeafe;
  border-left: 4px solid #3b82f6;
  font-weight: 500;
  color: #2563eb;
}

.book-icon {
  flex-shrink: 0;
}

.book-cover {
  width: 40px;
  height: 56px;
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  border-radius: 4px;
}

.book-info {
  flex: 1;
}

.book-name {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.book-list {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 中间：章节导航 */
.chapter-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;
  gap: 8px;
}

.nav-item:hover {
  background-color: #f3f4f6;
}

.nav-item.active {
  background-color: #dbeafe;
  color: #2563eb;
  font-weight: 500;
}

.nav-title {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  margin-right: 8px;
}

.nav-arrow {
  font-size: 12px;
  opacity: 0.7;
  flex-shrink: 0;
}

/* 子章节样式 */
.sub-nav-item {
  display: flex;
  align-items: center;
  padding: 8px 16px 8px 32px;
  background: #f9fafb;
  border-radius: 0 0 8px 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e5e7eb;
  border-top: none;
  margin-bottom: 4px;
}

.sub-nav-item:hover {
  background: #f3f4f6;
}

.sub-nav-title {
  font-size: 13px;
  color: #374151;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 8px;
}

.sub-nav-arrow {
  font-size: 12px;
  opacity: 0.7;
  flex-shrink: 0;
}

/* 右侧：答疑区域 */
/* 问题类型切换标签 */
.question-type-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.type-tab {
  flex: 1;
  padding: 10px 16px;
  text-align: center;
  background: #f3f4f6;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.3s ease;
}

.type-tab:hover {
  background: #e5e7eb;
}

.type-tab.active {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

/* 提问区域 */
.question-area {
  margin-bottom: 20px;
}

/* 图片上传区域 */
.image-upload-area {
  margin-bottom: 20px;
}

.upload-box {
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f9fafb;
}

.upload-box:hover {
  border-color: #3b82f6;
  background: #f0f7ff;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  font-size: 48px;
}

.upload-text {
  font-size: 14px;
  color: #6b7280;
}

.uploaded-preview {
  margin-top: 16px;
  text-align: center;
}

.preview-container {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}

.preview-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  display: block;
}

.delete-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(255, 0, 0, 0.8);
  color: white;
  border: none;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: all 0.2s ease;
}

.delete-btn:hover {
  background: rgba(255, 0, 0, 1);
  transform: scale(1.1);
}

.exam-preview-image {
  max-width: 100%;
  max-height: 150px;
  border-radius: 6px;
  display: block;
  margin-bottom: 12px;
}

.image-question-input {
  margin-bottom: 16px;
  width: 100%;
}

.exam-uploaded-preview {
  margin-top: 16px;
  text-align: center;
}

.exam-preview-image {
  max-width: 100%;
  max-height: 150px;
  border-radius: 6px;
  margin-bottom: 12px;
}

.exam-image-question-input {
  margin-bottom: 12px;
  width: 100%;
}

.question-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  transition: all 0.3s ease;
  box-sizing: border-box;
  margin-bottom: 12px;
}

.question-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.submit-btn {
  width: 100%;
  padding: 10px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* AI回答区域 */
.answer-area {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  max-height: 300px;
  overflow-y: auto;
}

.answer-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  background: #f9fafb;
  z-index: 1;
}

.answer-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.knowledge-graph {
  font-size: 14px;
  color: #3b82f6;
  font-weight: 500;
  cursor: pointer;
}

.answer-content {
  display: flex;
  gap: 12px;
}

.ai-avatar {
  flex-shrink: 0;
}

.answer-content .avatar-placeholder {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: 600;
}

.answer-text {
  flex: 1;
  font-size: 14px;
  color: #374151;
  line-height: 1.5;
  white-space: pre-wrap;
}

/* 历史问题 */
.history-section {
  background: #f3f4f6;
  border-radius: 12px;
  padding: 16px;
  max-height: 200px;
  overflow-y: auto;
  height: 200px;
}

.history-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.history-item {
  padding: 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e5e7eb;
}

.history-item:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.history-question {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
  width: 100%;
}

.history-answer {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 底部：网课推荐 */
.course-recommendation {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.recommendation-title {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 20px;
  text-align: center;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.course-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #f9fafb;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
}

.course-item:hover {
  background: #f3f4f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.course-link-icon {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 16px;
  opacity: 0.6;
  transition: opacity 0.3s ease;
}

.course-item:hover .course-link-icon {
  opacity: 1;
}

.course-avatar {
  flex-shrink: 0;
}

.avatar-placeholder {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: 600;
}

.course-info {
  flex: 1;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.course-desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.course-code {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
}

/* 其他标签页占位 */
.tab-content {
  background: white;
  border-radius: 16px;
  padding: 100px 20px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.placeholder {
  font-size: 18px;
  color: #6b7280;
  font-weight: 500;
}

/* ==================== 考研/考证问答页面样式 ==================== */
.exam-qa-page {
  padding: 20px;
}

.exam-qa-container {
  display: grid;
  grid-template-columns: 300px 1fr 280px;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 面板通用样式 */
.exam-panel {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.exam-panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

/* 左侧栏 - 考试科目选择 */
.exam-subject-select {
  margin-bottom: 12px;
}

.subject-dropdown {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%236B7280' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

.subject-dropdown:focus {
  outline: none;
  border-color: #3b82f6;
}

.exam-subject-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.exam-subject-item {
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.exam-subject-item:hover {
  background: #f3f4f6;
}

.exam-subject-item.active {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-color: #3b82f6;
}

/* 搜索able下拉框样式 */
.searchable-select {
  position: relative;
  width: 100%;
  z-index: 100;
}

.select-search-input {
  width: 100%;
  padding: 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  margin-bottom: 8px;
}

.select-search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.select-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  max-height: 200px;
  overflow-y: auto;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  margin-top: 4px;
}

.select-option {
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  color: #374151;
}

.select-option:hover {
  background-color: #f3f4f6;
  color: #3b82f6;
}

.select-option.active {
  background-color: #3b82f6;
  color: white;
  font-weight: 500;
}

.select-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: #374151;
}

.select-display:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
}

.select-arrow {
  font-size: 12px;
  color: #6b7280;
  transition: all 0.3s ease;
}

.select-display:hover .select-arrow {
  color: #3b82f6;
  transform: rotate(180deg);
}

/* 自定义滚动条 */
.select-dropdown::-webkit-scrollbar,
.history-section::-webkit-scrollbar,
.exam-history-list::-webkit-scrollbar,
.answer-area::-webkit-scrollbar,
.exam-answer-content::-webkit-scrollbar,
.exam-papers-container::-webkit-scrollbar,
.exam-courses-list::-webkit-scrollbar {
  width: 8px;
}

.select-dropdown::-webkit-scrollbar-track,
.history-section::-webkit-scrollbar-track,
.exam-history-list::-webkit-scrollbar-track,
.answer-area::-webkit-scrollbar-track,
.exam-answer-content::-webkit-scrollbar-track,
.exam-papers-container::-webkit-scrollbar-track,
.exam-courses-list::-webkit-scrollbar-track {
  background: #f8fafc;
  border-radius: 10px;
}

.select-dropdown::-webkit-scrollbar-thumb,
.history-section::-webkit-scrollbar-thumb,
.exam-history-list::-webkit-scrollbar-thumb,
.answer-area::-webkit-scrollbar-thumb,
.exam-answer-content::-webkit-scrollbar-thumb,
.exam-papers-container::-webkit-scrollbar-thumb,
.exam-courses-list::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.select-dropdown::-webkit-scrollbar-thumb:hover,
.history-section::-webkit-scrollbar-thumb:hover,
.exam-history-list::-webkit-scrollbar-thumb:hover,
.answer-area::-webkit-scrollbar-thumb:hover,
.exam-answer-content::-webkit-scrollbar-thumb:hover,
.exam-papers-container::-webkit-scrollbar-thumb:hover,
.exam-courses-list::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
  transform: scaleX(1.1);
}

/* 隐藏滚动条但保留滚动功能 */
.select-dropdown,
.history-section,
.exam-history-list,
.answer-area,
.exam-answer-content,
.exam-papers-container,
.exam-courses-list {
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 #f8fafc;
}

/* 高频考点 */
.exam-keypoints-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.exam-keypoint-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.exam-keypoint-item:hover {
  background: #f3f4f6;
}

.exam-keypoint-item.highlight {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: 1px solid #3b82f6;
}

.exam-keypoint-item.highlight .keypoint-name {
  color: white;
  font-weight: 600;
}

.exam-keypoint-item.highlight .keypoint-label {
  color: rgba(255, 255, 255, 0.8);
}

.keypoint-label {
  color: #6b7280;
  font-size: 13px;
}

.keypoint-name {
  color: #374151;
}

/* 中间栏 - 考点提问区 */
.exam-question-area {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.exam-question-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  min-width: 400px;
}

.exam-question-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.exam-submit-btn {
  padding: 12px 24px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.exam-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.exam-question-display {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  margin-top: 16px;
}

.exam-question-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.exam-answer-content {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  height: 300px;
  overflow-y: auto;
}

.ai-answer-box {
  background: white;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.ai-answer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.ai-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.ai-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.ai-answer-text {
  font-size: 14px;
  color: #374151;
  line-height: 1.5;
}

.ai-answer-text p {
  margin-bottom: 12px;
}

.ai-answer-text ul {
  padding-left: 20px;
  margin: 0;
}

.ai-answer-text li {
  margin-bottom: 8px;
}

.ai-answer-text li:last-child {
  margin-bottom: 0;
}

/* AI真题解析区 */
.exam-paper-item {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.3s ease;
}

.exam-paper-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.paper-title {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.paper-content {
  display: flex;
  gap: 16px;
  align-items: center;
}

.paper-image {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.paper-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.paper-image:hover img {
  transform: scale(1.05);
}

.paper-info {
  flex: 1;
}

.paper-name {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.paper-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.paper-tag {
  padding: 4px 12px;
  background: white;
  border-radius: 20px;
  font-size: 12px;
  color: #6b7280;
  border: 1px solid #e5e7eb;
  transition: all 0.3s ease;
}

.paper-tag:hover {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

/* 真题推荐容器 */
.exam-papers-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 400px;
  overflow-y: auto;
}

/* 网课推荐 */
.exam-courses-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 350px;
  overflow-y: auto;
}

.exam-course-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #f9fafb;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  border: 1px solid #e5e7eb;
}

.exam-course-item:hover {
  background: #f3f4f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #d1d5db;
}

.exam-course-item .course-link-icon {
  position: absolute;
  top: 12px;
  right: 12px;
  font-size: 16px;
  opacity: 0.6;
  transition: opacity 0.3s ease;
}

.exam-course-item:hover .course-link-icon {
  opacity: 1;
}

.exam-course-item .course-avatar {
  flex-shrink: 0;
}

.exam-course-item .avatar-placeholder {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: 600;
  box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
}

.exam-course-item .course-info {
  flex: 1;
}

.exam-course-item .course-name {
  font-size: 16px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.exam-course-item .course-desc {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.exam-course-item .course-code {
  font-size: 12px;
  color: #3b82f6;
  font-weight: 500;
}

/* 右侧栏 - 图片上传 */
.exam-upload-area {
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f9fafb;
  margin-bottom: 12px;
}

.exam-upload-area:hover {
  border-color: #3b82f6;
  background: #f0f7ff;
}

.exam-upload-text {
  font-size: 14px;
  color: #6b7280;
}

.exam-upload-btn {
  width: 100%;
  padding: 12px;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.exam-upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* 备考答疑历史记录 */
.exam-history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 600px;
  overflow-y: auto;
}

.exam-history-item {
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.exam-history-item:hover {
  background: #f3f4f6;
  border-color: #e5e7eb;
}

.exam-history-question {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

.exam-history-answer {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.4;
}

/* 响应式设计 - 考研考证页面 */
@media (max-width: 1200px) {
  .exam-qa-container {
    grid-template-columns: 260px 1fr;
  }
  
  .exam-right-column {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
  }
  
  .exam-right-column .exam-panel {
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .exam-qa-container {
    grid-template-columns: 1fr;
  }
  
  .exam-right-column {
    grid-template-columns: 1fr;
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .qa-container {
    grid-template-columns: 1fr 1fr;
    grid-template-areas:
      "left middle"
      "right right";
  }
  
  .left-panel {
    grid-area: left;
  }
  
  .middle-panel {
    grid-area: middle;
  }
  
  .right-panel {
    grid-area: right;
  }
}

@media (max-width: 768px) {
  .qa-container {
    grid-template-columns: 1fr;
    grid-template-areas:
      "left"
      "middle"
      "right";
  }
  
  .tab-navigation {
    flex-direction: column;
    gap: 4px;
  }
  
  .tab {
    padding: 10px 0;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .header-left {
    position: relative;
    top: 0;
    left: 0;
    transform: none;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .subtitle {
    text-align: left;
  }
}
</style>
