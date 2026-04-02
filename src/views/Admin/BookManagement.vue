<template>
  <div class="book-management">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>书籍资源管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>新增书籍
          </el-button>
        </div>
      </template>

      <!-- 搜索筛选区域 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="标题/作者/ISBN" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="选择分类" clearable>
            <el-option label="教材" value="教材" />
            <el-option label="参考书" value="参考书" />
            <el-option label="教辅" value="教辅" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="searchForm.subject" placeholder="选择学科" clearable>
            <el-option label="数学" value="数学" />
            <el-option label="英语" value="英语" />
            <el-option label="物理" value="物理" />
            <el-option label="化学" value="化学" />
            <el-option label="计算机" value="计算机" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable>
            <el-option label="正常" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 批量操作区域 -->
      <div class="batch-operation" v-if="selectedRows.length > 0">
        <el-alert :title="`已选择 ${selectedRows.length} 项`" type="info" :closable="false">
          <template #default>
            <el-button type="danger" size="small" @click="handleBatchDelete">批量删除</el-button>
            <el-button type="primary" size="small" @click="handleBatchUpdateCategory">批量修改分类</el-button>
          </template>
        </el-alert>
      </div>

      <!-- 数据表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.coverImage || getPrimaryImage(row.images)"
              :preview-src-list="[row.coverImage || getPrimaryImage(row.images)]"
              fit="cover"
              style="width: 60px; height: 80px; border-radius: 4px;"
            >
              <template #error>
                <div class="image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="subject" label="学科" width="100" />
        <el-table-column prop="isbn" label="ISBN" width="150" show-overflow-tooltip />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span v-if="row.price">¥{{ row.price }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="primary" link size="small" @click="handleManageChapters(row)">章节</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 书籍编辑对话框 -->
    <BookEditDialog
      v-model:visible="editDialogVisible"
      :book-id="currentBookId"
      @success="handleEditSuccess"
    />

    <!-- 章节管理对话框 -->
    <ChapterManagementDialog
      v-model:visible="chapterDialogVisible"
      :book="currentBook"
    />

    <!-- 批量修改分类对话框 -->
    <el-dialog v-model="categoryDialogVisible" title="批量修改分类" width="400px">
      <el-form>
        <el-form-item label="新分类">
          <el-select v-model="newCategory" placeholder="选择新分类">
            <el-option label="教材" value="教材" />
            <el-option label="参考书" value="参考书" />
            <el-option label="教辅" value="教辅" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBatchUpdateCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Picture } from '@element-plus/icons-vue'
import {
  getBookList,
  deleteBook,
  batchDeleteBooks,
  batchUpdateCategory
} from '@/api/bookAdmin'
import BookEditDialog from './components/BookEditDialog.vue'
import ChapterManagementDialog from './components/ChapterManagementDialog.vue'

// 搜索表单
const searchForm = reactive({
  keyword: '',
  category: '',
  subject: '',
  status: ''
})

// 表格数据
const loading = ref(false)
const tableData = ref([])
const selectedRows = ref([])

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 对话框控制
const editDialogVisible = ref(false)
const chapterDialogVisible = ref(false)
const categoryDialogVisible = ref(false)
const currentBookId = ref(null)
const currentBook = ref(null)
const newCategory = ref('')

// 获取书籍列表
const fetchBookList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    }
    const res = await getBookList(params)
    if (res.code === 200) {
      tableData.value = res.data.list
      pagination.total = res.data.total
    }
  } catch (error) {
    ElMessage.error('获取书籍列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  fetchBookList()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.subject = ''
  searchForm.status = ''
  handleSearch()
}

// 表格选择
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 分页大小变化
const handleSizeChange = (size) => {
  pagination.pageSize = size
  fetchBookList()
}

// 页码变化
const handleCurrentChange = (page) => {
  pagination.pageNum = page
  fetchBookList()
}

// 获取主图
const getPrimaryImage = (images) => {
  if (!images || images.length === 0) return ''
  const primary = images.find(img => img.isPrimary)
  return primary ? primary.imageUrl : images[0].imageUrl
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString()
}

// 创建书籍
const handleCreate = () => {
  currentBookId.value = null
  editDialogVisible.value = true
}

// 编辑书籍
const handleEdit = (row) => {
  currentBookId.value = row.id
  editDialogVisible.value = true
}

// 管理章节
const handleManageChapters = (row) => {
  currentBook.value = row
  chapterDialogVisible.value = true
}

// 删除书籍
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除书籍 "${row.title}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteBook(row.id)
      ElMessage.success('删除成功')
      fetchBookList()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  const ids = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(
    `确定要删除选中的 ${ids.length} 本书籍吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await batchDeleteBooks(ids)
      ElMessage.success('批量删除成功')
      selectedRows.value = []
      fetchBookList()
    } catch (error) {
      ElMessage.error('批量删除失败')
    }
  })
}

// 批量修改分类
const handleBatchUpdateCategory = () => {
  newCategory.value = ''
  categoryDialogVisible.value = true
}

// 确认批量修改分类
const confirmBatchUpdateCategory = async () => {
  if (!newCategory.value) {
    ElMessage.warning('请选择新分类')
    return
  }
  const ids = selectedRows.value.map(row => row.id)
  try {
    await batchUpdateCategory({ ids, category: newCategory.value })
    ElMessage.success('批量修改分类成功')
    categoryDialogVisible.value = false
    selectedRows.value = []
    fetchBookList()
  } catch (error) {
    ElMessage.error('批量修改分类失败')
  }
}

// 编辑成功回调
const handleEditSuccess = () => {
  fetchBookList()
}

onMounted(() => {
  fetchBookList()
})
</script>

<style scoped>
.book-management {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.batch-operation {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.image-placeholder {
  width: 60px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  color: #909399;
  font-size: 20px;
  border-radius: 4px;
}
</style>
