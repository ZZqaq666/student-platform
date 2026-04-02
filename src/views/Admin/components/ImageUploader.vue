<template>
  <div class="image-uploader">
    <el-upload
      ref="uploadRef"
      class="upload-area"
      drag
      action="#"
      :auto-upload="false"
      :on-change="handleFileChange"
      :show-file-list="false"
      :multiple="true"
      accept=".jpg,.jpeg,.png,.gif,.webp"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        拖拽文件到此处或 <em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 jpg/png/gif/webp 格式，单张不超过 10MB
        </div>
      </template>
    </el-upload>

    <!-- 图片列表 -->
    <div class="image-list" v-if="localImages.length > 0">
      <draggable
        v-model="localImages"
        item-key="id"
        class="image-grid"
        @end="handleSortEnd"
      >
        <template #item="{ element, index }">
          <div class="image-item" :class="{ 'is-primary': element.isPrimary }">
            <el-image
              :src="element.imageUrl"
              fit="cover"
              class="image-preview"
              :preview-src-list="previewList"
            />
            <div class="image-actions">
              <el-button
                v-if="!element.isPrimary"
                type="primary"
                link
                size="small"
                @click="handleSetPrimary(index)"
              >
                设为主图
              </el-button>
              <el-tag v-else type="success" size="small">主图</el-tag>
              <el-button type="danger" link size="small" @click="handleDelete(index)">
                删除
              </el-button>
            </div>
            <div class="image-sort">{{ index + 1 }}</div>
          </div>
        </template>
      </draggable>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import { getUploadSignatureWithFilename, uploadToOss } from '@/api/oss'

const props = defineProps({
  images: {
    type: Array,
    default: () => []
  },
  bookId: Number
})

const emit = defineEmits(['update:images'])

const uploadRef = ref(null)
const uploading = ref(false)
const localImages = ref([...props.images])

// 预览列表
const previewList = computed(() => {
  return localImages.value.map(img => img.imageUrl)
})

// 监听props变化
watch(() => props.images, (newVal) => {
  localImages.value = [...newVal]
}, { deep: true })

// 监听localImages变化，同步到父组件
watch(localImages, (newVal) => {
  emit('update:images', [...newVal])
}, { deep: true })

// 文件选择变化
const handleFileChange = async (file) => {
  // 验证文件类型
  const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.raw.type)) {
    ElMessage.error('不支持的文件格式')
    return
  }

  // 验证文件大小 (10MB)
  const maxSize = 10 * 1024 * 1024
  if (file.raw.size > maxSize) {
    ElMessage.error('文件大小不能超过10MB')
    return
  }

  try {
    uploading.value = true

    // 获取上传签名
    const signRes = await getUploadSignatureWithFilename(file.raw.name, 'cover')
    if (signRes.code !== 200) {
      throw new Error('获取上传签名失败')
    }

    // 上传到OSS
    const uploadRes = await uploadToOss(signRes.data, file.raw)
    if (uploadRes.code !== 200) {
      throw new Error('上传失败')
    }

    // 添加到图片列表
    const newImage = {
      imageUrl: uploadRes.data.url,
      imageType: 'COVER',
      fileSize: file.raw.size,
      mimeType: file.raw.type,
      isPrimary: localImages.value.length === 0, // 第一张设为主图
      sortOrder: localImages.value.length
    }

    localImages.value.push(newImage)
    ElMessage.success('上传成功')
  } catch (error) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
    // 清空上传列表
    uploadRef.value?.clearFiles()
  }
}

// 设置主图
const handleSetPrimary = (index) => {
  localImages.value.forEach((img, i) => {
    img.isPrimary = i === index
  })
  ElMessage.success('已设置为主图')
}

// 删除图片
const handleDelete = (index) => {
  const deleted = localImages.value.splice(index, 1)[0]

  // 如果删除的是主图，将第一张设为主图
  if (deleted.isPrimary && localImages.value.length > 0) {
    localImages.value[0].isPrimary = true
  }

  // 重新排序
  localImages.value.forEach((img, i) => {
    img.sortOrder = i
  })
}

// 排序结束
const handleSortEnd = () => {
  localImages.value.forEach((img, i) => {
    img.sortOrder = i
  })
}
</script>

<style scoped>
.image-uploader {
  padding: 20px;
}

.upload-area {
  margin-bottom: 20px;
}

.image-list {
  margin-top: 20px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

.image-item {
  position: relative;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  cursor: move;
}

.image-item.is-primary {
  border-color: #67c23a;
  box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2);
}

.image-preview {
  width: 100%;
  height: 150px;
  display: block;
}

.image-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.image-actions .el-button {
  color: #fff;
}

.image-sort {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 24px;
  height: 24px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

:deep(.el-upload-dragger) {
  width: 100%;
}
</style>
