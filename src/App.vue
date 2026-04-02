<script setup>
import { onMounted } from 'vue'

onMounted(() => {
  // 页面刷新时打印用户ID
  const userId = localStorage.getItem('userId')
  if (userId) {
    console.log('Current User ID:', userId)
  }
  // 打印localStorage中的所有数据，以便调试
  console.log('LocalStorage data:', {
    userId: localStorage.getItem('userId'),
    token: localStorage.getItem('token'),
    userName: localStorage.getItem('userName')
  })
})
</script>

<template>
  <router-view />
</template>

<style scoped>
</style>
