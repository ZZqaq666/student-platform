<template>
  <div class="wrong-book-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">我的错题本</h1>
        <p class="page-subtitle">管理和复习你的错题，提升学习效果</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showStats = true">
          <el-icon><TrendCharts /></el-icon>
          数据统计
        </el-button>
        <el-button @click="showExportDialog = true">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card">
            <div class="stat-value">{{ stats.totalWrongCount || 0 }}</div>
            <div class="stat-label">总错题数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card due">
            <div class="stat-value">{{ stats.dueTodayCount || 0 }}</div>
            <div class="stat-label">今日待复习</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card mastering">
            <div class="stat-value">{{ stats.masteringCount || 0 }}</div>
            <div class="stat-label">掌握中</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card mastered">
            <div class="stat-value">{{ stats.masteredCount || 0 }}</div>
            <div class="stat-label">已掌握</div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜索题目内容"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="科目">
          <el-select v-model="filterForm.subject" placeholder="全部科目" clearable style="width: 140px">
            <el-option
              v-for="item in subjectOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="难度">
          <el-select v-model="filterForm.difficulty" placeholder="全部难度" clearable style="width: 140px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>

        <el-form-item label="错误类型">
          <el-select v-model="filterForm.errorType" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="概念错误" value="CONCEPT" />
            <el-option label="计算错误" value="CALCULATION" />
            <el-option label="粗心大意" value="CARELESS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="掌握状态">
          <el-select v-model="filterForm.masterStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="未掌握" value="NOT_MASTERED" />
            <el-option label="掌握中" value="MASTERING" />
            <el-option label="已掌握" value="MASTERED" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级">
          <el-select v-model="filterForm.priority" placeholder="全部优先级" clearable style="width: 140px">
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="filterForm.showDueReview">仅显示待复习</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 错题列表 -->
    <el-card class="list-card">
      <el-table
        :data="wrongBookList"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" width="50" />

        <el-table-column label="题目内容" min-width="300">
          <template #default="{ row }">
            <div class="question-content">
              <span class="subject-tag">{{ row.subject || '未分类' }}</span>
              <span class="question-text">{{ row.exercise?.content || '无题目内容' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="错误类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getErrorTypeType(row.errorType)">
              {{ getErrorTypeLabel(row.errorType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="掌握状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getMasterStatusType(row.masterStatus)">
              {{ getMasterStatusLabel(row.masterStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="getPriorityType(row.priority)">
              {{ getPriorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="下次复习" width="120">
          <template #default="{ row }">
            <span :class="{ 'due-soon': row.daysUntilReview <= 0 }">
              {{ formatReviewTime(row) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleReview(row)">
              复习
            </el-button>
            <el-button type="success" size="small" @click="handleRedo(row)">
              重做
            </el-button>
            <el-button type="warning" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑错题" width="600px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="错误类型">
          <el-select v-model="editForm.errorType" style="width: 100%">
            <el-option label="概念错误" value="CONCEPT" />
            <el-option label="计算错误" value="CALCULATION" />
            <el-option label="粗心大意" value="CARELESS" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级">
          <el-radio-group v-model="editForm.priority">
            <el-radio-button label="HIGH">高</el-radio-button>
            <el-radio-button label="MEDIUM">中</el-radio-button>
            <el-radio-button label="LOW">低</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="标签">
          <el-input v-model="editForm.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>

        <el-form-item label="笔记">
          <el-input
            v-model="editForm.notes"
            type="textarea"
            :rows="4"
            placeholder="记录错误原因、解题思路..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重做对话框 -->
    <el-dialog v-model="showRedoDialog" title="重做错题" width="700px">
      <div v-if="currentWrongBook" class="redo-content">
        <div class="question-section">
          <h4>题目</h4>
          <div class="question-text">{{ currentWrongBook.exercise?.content }}</div>
        </div>

        <div class="options-section" v-if="currentWrongBook.exercise?.options">
          <h4>选项</h4>
          <el-radio-group v-model="redoAnswer">
            <el-radio
              v-for="(option, index) in parseOptions(currentWrongBook.exercise.options)"
              :key="index"
              :label="index"
            >
              {{ String.fromCharCode(65 + index) }}. {{ option }}
            </el-radio>
          </el-radio-group>
        </div>

        <div class="answer-section" v-else>
          <h4>你的答案</h4>
          <el-input v-model="redoAnswer" placeholder="请输入答案" />
        </div>

        <div class="previous-answer" v-if="currentWrongBook.userAnswer">
          <h4>上次答案</h4>
          <el-tag type="danger">{{ currentWrongBook.userAnswer }}</el-tag>
        </div>
      </div>

      <template #footer>
        <el-button @click="showRedoDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRedo">提交答案</el-button>
      </template>
    </el-dialog>

    <!-- 统计对话框 -->
    <el-dialog v-model="showStats" title="错题统计" width="800px">
      <div class="stats-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <h4>掌握状态分布</h4>
            <div class="chart-placeholder">
              <div class="stat-item">
                <span class="label">未掌握</span>
                <el-progress :percentage="getPercentage(stats.notMasteredCount)" status="exception" />
              </div>
              <div class="stat-item">
                <span class="label">掌握中</span>
                <el-progress :percentage="getPercentage(stats.masteringCount)" />
              </div>
              <div class="stat-item">
                <span class="label">已掌握</span>
                <el-progress :percentage="getPercentage(stats.masteredCount)" status="success" />
              </div>
            </div>
          </el-col>
          <el-col :span="12">
            <h4>复习计划</h4>
            <div class="review-plan">
              <div class="plan-item">
                <span class="label">今日待复习</span>
                <span class="value highlight">{{ stats.dueTodayCount || 0 }} 题</span>
              </div>
              <div class="plan-item">
                <span class="label">本周待复习</span>
                <span class="value">{{ stats.dueThisWeekCount || 0 }} 题</span>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-dialog>

    <!-- 导出对话框 -->
    <el-dialog v-model="showExportDialog" title="导出错题本" width="400px">
      <el-form label-width="100px">
        <el-form-item label="导出格式">
          <el-radio-group v-model="exportFormat">
            <el-radio label="json">JSON</el-radio>
            <el-radio label="csv">CSV</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showExportDialog = false">取消</el-button>
        <el-button type="primary" @click="exportWrongBook">导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { TrendCharts, Download } from '@element-plus/icons-vue'
import api from '@/api/api'

const router = useRouter()

// 状态
const loading = ref(false)
const wrongBookList = ref([])
const showEditDialog = ref(false)
const showRedoDialog = ref(false)
const showStats = ref(false)
const showExportDialog = ref(false)
const currentWrongBook = ref(null)
const redoAnswer = ref('')
const exportFormat = ref('json')

// 统计数据
const stats = reactive({
  totalWrongCount: 0,
  masteredCount: 0,
  masteringCount: 0,
  notMasteredCount: 0,
  dueTodayCount: 0,
  dueThisWeekCount: 0
})

// 筛选表单
const filterForm = reactive({
  keyword: '',
  subject: '',
  difficulty: '',
  errorType: '',
  masterStatus: '',
  priority: '',
  showDueReview: false
})

// 科目选项（从错题数据中动态提取）
const subjectOptions = ref([])

// 从错题列表中提取科目选项
const extractSubjectOptions = (list) => {
  const subjects = new Set()
  list.forEach(item => {
    if (item.subject) subjects.add(item.subject)
  })
  subjectOptions.value = [...subjects]
}

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// 编辑表单
const editForm = reactive({
  id: null,
  errorType: '',
  priority: '',
  tags: '',
  notes: ''
})

// 获取错题列表（后端全量返回，前端本地分页）
const fetchWrongBookList = async () => {
  loading.value = true
  try {
    const requestData = {
      ...filterForm,
      pageNum: 1,
      pageSize: 10000
    }

    const response = await api.post('/exercise/wrong-book/filter', requestData)
    if (response.code === 200) {
      const allList = response.data || []
      extractSubjectOptions(allList)

      // 本地分页
      pagination.total = allList.length
      const start = (pagination.pageNum - 1) * pagination.pageSize
      wrongBookList.value = allList.slice(start, start + pagination.pageSize)
    } else {
      ElMessage.error(response.message || '获取错题列表失败')
    }
  } catch (error) {
    console.error('获取错题列表失败:', error)
    ElMessage.error('获取错题列表失败')
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const response = await api.get('/exercise/wrong-book/stats')
    if (response.code === 200 && response.data) {
      Object.assign(stats, response.data)
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchWrongBookList()
}

// 重置筛选
const resetFilter = () => {
  Object.keys(filterForm).forEach(key => {
    filterForm[key] = key === 'showDueReview' ? false : ''
  })
  handleSearch()
}

// 分页
const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchWrongBookList()
}

const handlePageChange = (page) => {
  pagination.pageNum = page
  fetchWrongBookList()
}

// 复习
const handleReview = (row) => {
  currentWrongBook.value = row
  router.push({
    path: '/practice',
    query: {
      single: 'true',
      exerciseId: row.exercise?.id,
      fromWrongBook: 'true',
      wrongBookId: row.id
    }
  })
}

// 重做
const handleRedo = (row) => {
  currentWrongBook.value = row
  redoAnswer.value = ''
  showRedoDialog.value = true
}

// 提交重做
const submitRedo = async () => {
  if (!redoAnswer.value && redoAnswer.value !== 0) {
    ElMessage.warning('请输入答案')
    return
  }

  try {
    const isCorrect = checkAnswer(currentWrongBook.value, redoAnswer.value)

    const response = await api.post(`/exercise/wrong-book/${currentWrongBook.value.id}/redo`, null, {
      params: {
        userAnswer: redoAnswer.value,
        isCorrect: isCorrect
      }
    })

    if (response.code === 200) {
      ElMessage.success(isCorrect ? '回答正确！' : '回答错误，继续努力！')
      showRedoDialog.value = false
      fetchWrongBookList()
      fetchStats()
    } else {
      ElMessage.error(response.message || '提交失败')
    }
  } catch (error) {
    console.error('提交重做失败:', error)
    ElMessage.error('提交失败')
  }
}

// 检查答案
const checkAnswer = (wrongBook, userAnswer) => {
  const exercise = wrongBook.exercise
  if (!exercise) return false

  // 选择题 - 使用 correctAnswer 索引
  if (exercise.options && exercise.correctAnswer !== undefined) {
    return userAnswer === exercise.correctAnswer
  }

  // 其他题型 - 使用 answer 字段
  const correctAnswer = exercise.answer
  if (!correctAnswer) return false

  return userAnswer.toString().trim() === correctAnswer.toString().trim()
}

// 解析选项
const parseOptions = (optionsStr) => {
  if (!optionsStr) return []
  if (Array.isArray(optionsStr)) return optionsStr
  return optionsStr.split(',').map(s => s.trim())
}

// 编辑
const handleEdit = (row) => {
  currentWrongBook.value = row
  editForm.id = row.id
  editForm.errorType = row.errorType || 'OTHER'
  editForm.priority = row.priority || 'MEDIUM'
  editForm.tags = row.tags || ''
  editForm.notes = row.notes || ''
  showEditDialog.value = true
}

// 保存编辑
const saveEdit = async () => {
  try {
    const response = await api.put(`/exercise/wrong-book/${editForm.id}`, {
      errorType: editForm.errorType,
      priority: editForm.priority,
      tags: editForm.tags,
      notes: editForm.notes
    })
    if (response.code === 200) {
      ElMessage.success('保存成功')
      showEditDialog.value = false
      fetchWrongBookList()
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这道错题吗？', '提示', {
      type: 'warning'
    })

    const response = await api.delete(`/exercise/wrong-book/${row.id}`)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      fetchWrongBookList()
      fetchStats()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 导出
const exportWrongBook = async () => {
  try {
    const response = await api.get('/exercise/wrong-book/export', { format: exportFormat.value }, {
      responseType: 'blob'
    })

    const blob = new Blob([response], {
      type: exportFormat.value === 'json' ? 'application/json' : 'text/csv'
    })

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `错题本_${new Date().toLocaleDateString()}.${exportFormat.value}`
    link.click()
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功')
    showExportDialog.value = false
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 格式化函数
const getErrorTypeLabel = (type) => {
  const map = {
    'CONCEPT': '概念错误',
    'CALCULATION': '计算错误',
    'CARELESS': '粗心大意',
    'OTHER': '其他'
  }
  return map[type] || '其他'
}

const getErrorTypeType = (type) => {
  const map = {
    'CONCEPT': 'danger',
    'CALCULATION': 'warning',
    'CARELESS': 'info',
    'OTHER': ''
  }
  return map[type] || ''
}

const getMasterStatusLabel = (status) => {
  const map = {
    'NOT_MASTERED': '未掌握',
    'MASTERING': '掌握中',
    'MASTERED': '已掌握'
  }
  return map[status] || '未掌握'
}

const getMasterStatusType = (status) => {
  const map = {
    'NOT_MASTERED': 'danger',
    'MASTERING': 'warning',
    'MASTERED': 'success'
  }
  return map[status] || ''
}

const getPriorityLabel = (priority) => {
  const map = {
    'HIGH': '高',
    'MEDIUM': '中',
    'LOW': '低'
  }
  return map[priority] || '中'
}

const getPriorityType = (priority) => {
  const map = {
    'HIGH': 'danger',
    'MEDIUM': 'warning',
    'LOW': 'info'
  }
  return map[priority] || ''
}

const formatReviewTime = (row) => {
  if (!row.nextReviewAt) {
    return row.masterStatus === 'MASTERED' ? '已掌握' : '未设置'
  }

  const days = row.daysUntilReview
  if (days === null || days === undefined) {
    return '未知'
  }

  if (days < 0) {
    return `逾期 ${Math.abs(days)} 天`
  } else if (days === 0) {
    return '今天'
  } else {
    return `${days} 天后`
  }
}

const getPercentage = (count) => {
  if (!stats.totalWrongCount || stats.totalWrongCount === 0) return 0
  return Math.round((count / stats.totalWrongCount) * 100)
}

// 初始化
onMounted(() => {
  fetchWrongBookList()
  fetchStats()
})
</script>

<style scoped>
.wrong-book-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 12px;
}

.stats-cards {
  margin-bottom: 24px;
}

.stat-card {
  text-align: center;
  padding: 20px;
}

.stat-card .stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #6366f1;
  margin-bottom: 8px;
}

.stat-card.due .stat-value {
  color: #ef4444;
}

.stat-card.mastering .stat-value {
  color: #f59e0b;
}

.stat-card.mastered .stat-value {
  color: #10b981;
}

.stat-card .stat-label {
  font-size: 14px;
  color: #6b7280;
}

.filter-card {
  margin-bottom: 24px;
}

.list-card {
  margin-bottom: 24px;
}

.question-content {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.subject-tag {
  background: #e0e7ff;
  color: #4f46e5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
}

.question-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.due-soon {
  color: #ef4444;
  font-weight: 600;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.redo-content {
  padding: 20px;
}

.question-section,
.options-section,
.answer-section,
.previous-answer {
  margin-bottom: 24px;
}

.question-section h4,
.options-section h4,
.answer-section h4,
.previous-answer h4 {
  margin-bottom: 12px;
  color: #374151;
}

.question-text {
  font-size: 16px;
  line-height: 1.6;
  color: #1f2937;
}

.stats-content {
  padding: 20px;
}

.stats-content h4 {
  margin-bottom: 16px;
  color: #374151;
}

.stat-item {
  margin-bottom: 16px;
}

.stat-item .label {
  display: inline-block;
  width: 80px;
  color: #6b7280;
}

.review-plan {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.plan-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #e5e7eb;
}

.plan-item:last-child {
  border-bottom: none;
}

.plan-item .value {
  font-weight: 600;
  color: #374151;
}

.plan-item .value.highlight {
  color: #ef4444;
  font-size: 18px;
}
</style>
