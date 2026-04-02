<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑书籍' : '新增书籍'"
    width="900px"
    destroy-on-close
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      class="book-form"
    >
      <el-tabs v-model="activeTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-row :gutter="20">
            <el-col :span="16">
              <el-form-item label="书名" prop="title">
                <el-input v-model="form.title" placeholder="请输入书名" maxlength="200" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="ISBN">
                <el-input v-model="form.isbn" placeholder="ISBN编号" maxlength="20" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="作者">
                <el-input v-model="form.author" placeholder="请输入作者" maxlength="100" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="出版社">
                <el-input v-model="form.publisher" placeholder="请输入出版社" maxlength="100" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="分类">
                <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
                  <el-option label="教材" value="教材" />
                  <el-option label="参考书" value="参考书" />
                  <el-option label="教辅" value="教辅" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="学科">
                <el-select v-model="form.subject" placeholder="选择学科" style="width: 100%">
                  <el-option label="数学" value="数学" />
                  <el-option label="英语" value="英语" />
                  <el-option label="物理" value="物理" />
                  <el-option label="化学" value="化学" />
                  <el-option label="计算机" value="计算机" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="状态">
                <el-select v-model="form.status" placeholder="选择状态" style="width: 100%">
                  <el-option label="正常" value="ACTIVE" />
                  <el-option label="停用" value="INACTIVE" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="价格">
                <el-input-number v-model="form.price" :precision="2" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="页数">
                <el-input-number v-model="form.pageCount" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="出版日期">
                <el-date-picker v-model="form.publishDate" type="date" placeholder="选择日期" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="简介">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="请输入书籍简介"
              maxlength="2000"
              show-word-limit
            />
          </el-form-item>
        </el-tab-pane>

        <!-- 图片管理 -->
        <el-tab-pane label="图片管理" name="images">
          <ImageUploader
            v-model:images="form.images"
            :book-id="bookId"
          />
        </el-tab-pane>
      </el-tabs>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createBook, updateBook, getBookDetail } from '@/api/bookAdmin'
import ImageUploader from './ImageUploader.vue'

const props = defineProps({
  visible: Boolean,
  bookId: Number
})

const emit = defineEmits(['update:visible', 'success'])

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const isEdit = computed(() => !!props.bookId)

const activeTab = ref('basic')
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  title: '',
  isbn: '',
  author: '',
  publisher: '',
  category: '',
  subject: '',
  status: 'ACTIVE',
  price: null,
  pageCount: null,
  publishDate: null,
  description: '',
  images: []
})

const rules = {
  title: [
    { required: true, message: '请输入书名', trigger: 'blur' },
    { max: 200, message: '书名长度不能超过200字符', trigger: 'blur' }
  ]
}

// 加载书籍详情
const loadBookDetail = async () => {
  if (!props.bookId) return
  try {
    const res = await getBookDetail(props.bookId)
    if (res.code === 200) {
      const data = res.data
      Object.assign(form, {
        title: data.title,
        isbn: data.isbn,
        author: data.author,
        publisher: data.publisher,
        category: data.category,
        subject: data.subject,
        status: data.status,
        price: data.price,
        pageCount: data.pageCount,
        publishDate: data.publishDate,
        description: data.description,
        images: data.images || []
      })
    }
  } catch (error) {
    ElMessage.error('加载书籍详情失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const data = { ...form }
    if (isEdit.value) {
      await updateBook(props.bookId, data)
      ElMessage.success('更新成功')
    } else {
      await createBook(data)
      ElMessage.success('创建成功')
    }
    emit('success')
    visible.value = false
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  formRef.value?.resetFields()
  activeTab.value = 'basic'
  Object.assign(form, {
    title: '',
    isbn: '',
    author: '',
    publisher: '',
    category: '',
    subject: '',
    status: 'ACTIVE',
    price: null,
    pageCount: null,
    publishDate: null,
    description: '',
    images: []
  })
}

// 监听visible变化
watch(() => props.visible, (val) => {
  if (val && props.bookId) {
    loadBookDetail()
  }
})
</script>

<style scoped>
.book-form {
  max-height: 600px;
  overflow-y: auto;
}
</style>
