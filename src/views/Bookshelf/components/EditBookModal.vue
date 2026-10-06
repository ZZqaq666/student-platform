<template>
  <!-- 编辑书籍模态框 -->
  <div class="modal-overlay" v-if="showEditBookModal" @click="showEditBookModal = false" :class="{ 'dark-mode': isDarkMode }">
    <div class="modal-content edit-book-modal" @click.stop :class="{ 'dark-mode': isDarkMode }">
      <div class="modal-header">
        <div class="modal-header-content">
          <div class="modal-icon">📚</div>
          <h2 class="modal-title">编辑书籍信息</h2>
        </div>
        <button class="modal-close" @click="showEditBookModal = false">
          <span class="close-icon">×</span>
        </button>
      </div>

      <div class="edit-book-content">
        <form class="book-form">
          <div class="form-row">
            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">📚</span>
                <label for="edit-book-title">书名</label>
              </div>
              <input
                type="text"
                id="edit-book-title"
                v-model="editBookForm.title"
                class="form-input"
                placeholder="请输入书名"
              >
            </div>

            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">👤</span>
                <label for="edit-book-author">作者</label>
              </div>
              <input
                type="text"
                id="edit-book-author"
                v-model="editBookForm.author"
                class="form-input"
                placeholder="请输入作者"
              >
            </div>
          </div>

          <div class="form-field">
            <div class="field-label">
              <span class="label-icon">📝</span>
              <label for="edit-book-description">简介</label>
            </div>
            <textarea
              id="edit-book-description"
              v-model="editBookForm.description"
              class="form-textarea"
              placeholder="请输入书籍简介"
              rows="4"
            ></textarea>
          </div>

          <div class="form-row">
            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">🎓</span>
                <label for="edit-book-major">专业</label>
              </div>
              <select
                id="edit-book-major"
                v-model="editBookForm.major"
                class="form-select"
              >
                <option value="">请选择专业</option>
                <option v-for="major in majorOptions" :key="major.value" :value="major.value">
                  {{ major.label }}
                </option>
              </select>
            </div>

            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">📅</span>
                <label for="edit-book-semester">学期</label>
              </div>
              <select
                id="edit-book-semester"
                v-model="editBookForm.semester"
                class="form-select"
              >
                <option value="">请选择学期</option>
                <option v-for="semester in semesterOptions" :key="semester.value" :value="semester.value">
                  {{ semester.label }}
                </option>
              </select>
            </div>

            <div class="form-field">
              <div class="field-label">
                <span class="label-icon">📋</span>
                <label for="edit-book-courseType">课程类型</label>
              </div>
              <select
                id="edit-book-courseType"
                v-model="editBookForm.courseType"
                class="form-select"
              >
                <option value="">请选择课程类型</option>
                <option v-for="courseType in courseTypeOptions" :key="courseType.value" :value="courseType.value">
                  {{ courseType.label }}
                </option>
              </select>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" @click="showEditBookModal = false">
              <span class="btn-icon">❌</span>
              取消
            </button>
            <button type="button" class="btn btn-primary" @click="saveBookChanges" :disabled="editLoading">
              <span v-if="editLoading" class="loading-spinner small"></span>
              <span v-else class="btn-icon">💾</span>
              {{ editLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue'

const {
  isDarkMode, showEditBookModal, editBookForm, editLoading, saveBookChanges,
  majorOptions, semesterOptions, courseTypeOptions
} = inject('bookshelfCtx')
</script>
