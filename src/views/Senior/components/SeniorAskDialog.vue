<script setup>
import { computed, nextTick, reactive, ref, shallowRef, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  submitting: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'submit'])

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value) => {
    emit('update:modelValue', value)
  }
})

const formRef = ref(null)
const tagInputRef = ref(null)

const form = reactive({
  title: '',
  category: '',
  content: '',
  tags: []
})

const rules = Object.freeze({
  title: [
    { required: true, message: '请输入问题标题', trigger: 'blur' },
    { min: 6, max: 50, message: '标题长度需在 6-50 字之间', trigger: 'blur' }
  ],
  category: [{ required: true, message: '请选择问题分类', trigger: 'change' }],
  content: [
    { required: true, message: '请填写问题详情', trigger: 'blur' },
    { min: 12, max: 800, message: '问题详情需在 12-800 字之间', trigger: 'blur' }
  ]
})

const showTagInput = shallowRef(false)
const tagDraft = shallowRef('')
const fileList = ref([])

function resetForm() {
  form.title = ''
  form.category = ''
  form.content = ''
  form.tags = []
  showTagInput.value = false
  tagDraft.value = ''
  fileList.value = []
  formRef.value?.clearValidate?.()
}

function closeDialog() {
  dialogVisible.value = false
}

function enableTagInput() {
  showTagInput.value = true
  nextTick(() => {
    tagInputRef.value?.focus?.()
  })
}

function appendTag() {
  const value = tagDraft.value.trim()
  if (value && !form.tags.includes(value)) {
    form.tags = [...form.tags, value]
  }

  tagDraft.value = ''
  showTagInput.value = false
}

function removeTag(tag) {
  form.tags = form.tags.filter((item) => item !== tag)
}

function handleUploadChange(uploadFile, uploadFiles) {
  fileList.value = uploadFiles
}

function handleUploadRemove(uploadFile, uploadFiles) {
  fileList.value = uploadFiles
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      resolve(typeof reader.result === 'string' ? reader.result : '')
    }
    reader.onerror = () => {
      reject(new Error('图片读取失败'))
    }
    reader.readAsDataURL(file)
  })
}

async function buildImagePayload() {
  const timestamp = Date.now()
  const tasks = fileList.value.map(async (item, index) => {
    const rawFile = item?.raw
    if (rawFile instanceof Blob) {
      const url = await readFileAsDataUrl(rawFile)
      if (!url) {
        return null
      }

      return {
        id: `img-${timestamp}-${index}`,
        name: item.name || `图片${index + 1}`,
        url
      }
    }

    const url = typeof item?.url === 'string' ? item.url.trim() : ''
    if (!url) {
      return null
    }

    return {
      id: `img-${timestamp}-${index}`,
      name: item.name || `图片${index + 1}`,
      url
    }
  })

  const resolved = await Promise.all(tasks)
  return resolved.filter(Boolean)
}

async function submit() {
  const validator = formRef.value
  if (!validator) {
    return
  }

  await validator.validate()
  const images = await buildImagePayload()

  emit('submit', {
    title: form.title,
    category: form.category,
    content: form.content,
    tags: form.tags,
    images
  })
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible && !props.submitting) {
      resetForm()
    }
  }
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    width="680px"
    class="ask-dialog"
    title="发布求助"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="问题标题" prop="title">
        <el-input v-model="form.title" maxlength="50" show-word-limit placeholder="请用一句话概述你的问题" />
      </el-form-item>

      <el-form-item label="问题分类" prop="category">
        <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
          <el-option label="数学" value="math" />
          <el-option label="物理" value="physics" />
          <el-option label="计算机" value="cs" />
          <el-option label="英语" value="english" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>

      <el-form-item label="标签（可选）">
        <div class="tag-editor">
          <el-tag
            v-for="tag in form.tags"
            :key="tag"
            closable
            class="tag-item"
            @close="removeTag(tag)"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="showTagInput"
            ref="tagInputRef"
            v-model="tagDraft"
            class="tag-input"
            size="small"
            @blur="appendTag"
            @keyup.enter="appendTag"
          />
          <el-button v-else size="small" @click="enableTagInput">+ 添加标签</el-button>
        </div>
      </el-form-item>

      <el-form-item label="问题详情" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="6"
          maxlength="800"
          show-word-limit
          placeholder="请详细说明背景、已尝试方案和卡点"
        />
      </el-form-item>

      <el-form-item label="补充图片（可选，最多 3 张）">
        <el-upload
          class="upload-block"
          action="#"
          list-type="picture-card"
          :auto-upload="false"
          :file-list="fileList"
          :limit="3"
          @change="handleUploadChange"
          @remove="handleUploadRemove"
        >
          <span>上传</span>
        </el-upload>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="footer-actions">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">发布问题</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.tag-editor {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.tag-item {
  margin: 0;
}

.tag-input {
  width: 110px;
}

.upload-block :deep(.el-upload--picture-card) {
  width: 90px;
  height: 90px;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .ask-dialog :deep(.el-dialog) {
    width: 95% !important;
    margin-top: 6vh;
  }
}
</style>
