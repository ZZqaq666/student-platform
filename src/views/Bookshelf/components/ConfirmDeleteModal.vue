<template>
  <!-- 确认删除对话框 -->
  <transition name="modal">
    <div class="modal-overlay" v-if="showDeleteConfirm" @click="showDeleteConfirm = false">
      <div class="confirm-modal" @click.stop>
        <div class="confirm-header">
          <h3 class="confirm-title">从书架移除</h3>
        </div>
        <div class="confirm-body">
          <p class="confirm-message">确定要把这本书从你的书架移除吗？（不影响其他用户的目录）</p>
        </div>
        <div class="confirm-footer">
          <button class="confirm-btn cancel" @click="showDeleteConfirm = false">取消</button>
          <button class="confirm-btn delete" @click="confirmDelete">移除</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { inject } from 'vue'

const { showDeleteConfirm, confirmDelete } = inject('bookshelfCtx')
</script>
