<template>
  <el-dialog
    v-model="visible"
    :title="`章节管理 - ${book?.title || ''}`"
    width="900px"
    destroy-on-close
  >
    <div class="chapter-management">
      <!-- 章节列表 -->
      <div class="chapter-list">
        <el-empty v-if="chapters.length === 0" description="暂无章节" />

        <draggable
          v-else
          v-model="chapters"
          item-key="id"
          handle=".drag-handle"
          class="chapter-items"
          @end="handleSortEnd"
        >
          <template #item="{ element, index }">
            <div class="chapter-item" :class="{ 'is-editing': editingId === element.id }">
              <div class="chapter-header">
                <el-icon class="drag-handle"><Rank /></el-icon>
                <span class="chapter-index">第 {{ index + 1 }} 章</span>
                <span class="chapter-title">{{ element.title }}</span>
                <div class="chapter-actions">
                  <el-button type="primary" link size="small" @click="handleEdit(element)">
                    编辑
                  </el-button>
                  <el-button type="danger" link size="small" @click="handleDelete(element)">
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </template>
        </draggable>
      </div>

      <!-- 添加/编辑章节表单 -->
      <el-divider />

      <el-form :model="form" label-width="80px">
        <el-form-item label="章节标题">
          <el-input v-model="form.title" placeholder="请输入章节标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="章节内容">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入章节内容（支持HTML富文本）"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">
            {{ isEditing ? '更新章节' : '添加章节' }}
          </el-button>
          <el-button v-if="isEditing" @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import {
  getChapterList,
  createChapter,
  updateChapter,
  deleteChapter,
  updateChapterSort
} from '@/api/bookAdmin'

const props = defineProps({
  visible: Boolean,
  book: Object
})

const emit = defineEmits(['update:visible'])

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const chapters = ref([])
const editingId = ref(null)
const isEditing = computed(() => !!editingId.value)

const form = reactive({
  title: '',
  content: ''
})

// 加载章节列表
const loadChapters = async () => {
  if (!props.book?.id) return
  try {
    const res = await getChapterList(props.book.id)
    if (res.code === 200) {
      chapters.value = res.data || []
    }
  } catch (error) {
    ElMessage.error('加载章节列表失败')
  }
}

// 保存章节
const handleSave = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入章节标题')
    return
  }

  try {
    if (isEditing.value) {
      await updateChapter(props.book.id, editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await createChapter(props.book.id, form)
      ElMessage.success('添加成功')
    }
    resetForm()
    loadChapters()
  } catch (error) {
    ElMessage.error(isEditing.value ? '更新失败' : '添加失败')
  }
}

// 编辑章节
const handleEdit = (chapter) => {
  editingId.value = chapter.id
  form.title = chapter.title
  form.content = chapter.content
}

// 删除章节
const handleDelete = (chapter) => {
  ElMessageBox.confirm(
    `确定要删除章节 "${chapter.title}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteChapter(props.book.id, chapter.id)
      ElMessage.success('删除成功')
      loadChapters()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 取消编辑
const handleCancel = () => {
  resetForm()
}

// 重置表单
const resetForm = () => {
  editingId.value = null
  form.title = ''
  form.content = ''
}

// 排序结束
const handleSortEnd = async () => {
  try {
    const sortedChapters = chapters.value.map((chapter, index) => ({
      id: chapter.id,
      title: chapter.title,
      sortOrder: index
    }))
    await updateChapterSort(props.book.id, sortedChapters)
    ElMessage.success('排序已更新')
  } catch (error) {
    ElMessage.error('排序更新失败')
    loadChapters() // 重新加载
  }
}

// 监听visible变化
watch(() => props.visible, (val) => {
  if (val && props.book?.id) {
    loadChapters()
    resetForm()
  }
})
</script>

<style scoped>
.chapter-management {
  max-height: 600px;
  overflow-y: auto;
}

.chapter-list {
  margin-bottom: 20px;
}

.chapter-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chapter-item {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background: #fff;
  transition: all 0.3s;
}

.chapter-item.is-editing {
  border-color: #409eff;
  background: #f0f9ff;
}

.chapter-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.drag-handle {
  cursor: move;
  color: #909399;
}

.chapter-index {
  font-weight: bold;
  color: #606266;
  min-width: 60px;
}

.chapter-title {
  flex: 1;
  color: #303133;
}

.chapter-actions {
  display: flex;
  gap: 8px;
}
</style>
