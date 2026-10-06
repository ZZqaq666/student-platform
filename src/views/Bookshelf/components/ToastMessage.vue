<template>
  <!-- 成功提示 -->
  <transition name="toast">
    <div class="toast" v-if="showSuccessToast">
      <div class="toast-icon" :class="{ 'error': toastType === 'error' }">
        {{ toastType === 'success' ? '✓' : '×' }}
      </div>
      <div class="toast-message">{{ successMessage }}</div>
    </div>
  </transition>
</template>

<script setup>
import { inject } from 'vue'

const { showSuccessToast, successMessage, toastType } = inject('bookshelfCtx')
</script>
