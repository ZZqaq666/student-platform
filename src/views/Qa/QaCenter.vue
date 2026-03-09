<template>
  <el-container>
    <!-- 左侧固定侧边栏（专业课/作业/考研） -->
    <el-aside width="200px" style="border-right: 1px solid #eee; height: 80vh;">
      <el-menu :default-active="activeSubMenu" class="qa-menu" router>
        <el-menu-item index="/qa">专业课问答</el-menu-item>
        <el-menu-item index="/qa/homework">作业答疑</el-menu-item>
        <el-menu-item index="/qa/exam">考研/考证问答</el-menu-item>
      </el-menu>
    </el-aside>

    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
const activeSubMenu = route.path 
</script>

<style scoped>
.qa-menu {
  border-right: none;
}
</style>