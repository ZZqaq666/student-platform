<template>
  <el-card>
    <div class="graph-header">
      <h3>知识图谱可视化</h3>
      <!-- 缩放控制按钮 -->
      <div class="graph-controls">
        <el-button icon="ZoomIn" @click="zoomIn" size="small" />
        <el-button icon="ZoomOut" @click="zoomOut" size="small" />
        <el-button icon="Refresh" @click="resetGraph" size="small" />
      </div>
    </div>

    <!-- ECharts图谱展示区 -->
    <div class="graph-container" ref="graphRef"></div>

    <!-- 原文片段查看区（ -->
    <el-card style="margin-top: 20px;">
      <el-tabs v-model="activeTab" @tab-click="showFragment">
        <el-tab-pane label="知识点1" name="1">知识点1原文片段</el-tab-pane>
        <el-tab-pane label="知识点2" name="2">知识点2原文片段</el-tab-pane>
        <el-tab-pane label="知识点3" name="3">知识点3原文片段</el-tab-pane>
      </el-tabs>
      <!-- 原文片段展示 -->
      <div class="fragment-content" v-if="activeTab">
        {{ currentFragment }}
      </div>
    </el-card>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

// ECharts实例
const graphRef = ref()
let myChart = null
// 原文片段
const activeTab = ref('1')
const fragments = {
  '1': '知识点1的教材原文片段...',
  '2': '知识点2的教材原文片段...',
  '3': '知识点3的教材原文片段...'
}
const currentFragment = ref(fragments['1'])

// 初始化ECharts（草图中的图谱）
onMounted(() => {
  myChart = echarts.init(graphRef.value)
  const option = {
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: [
          { name: '知识点1', value: 10 },
          { name: '知识点2', value: 8 },
          { name: '知识点3', value: 6 }
        ],
        links: [
          { source: '知识点1', target: '知识点2' },
          { source: '知识点1', target: '知识点3' }
        ],
        force: { repulsion: 200 }
      }
    ]
  }
  myChart.setOption(option)
})

// 图谱缩放控制
const zoomIn = () => myChart.dispatchAction({ type: 'dataZoom', start: 0, end: 80 })
const zoomOut = () => myChart.dispatchAction({ type: 'dataZoom', start: 0, end: 100 })
const resetGraph = () => myChart.resize()

// 切换Tab显示对应原文
const showFragment = () => {
  currentFragment.value = fragments[activeTab.value]
}
</script>

<style scoped>
.graph-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.graph-container {
  width: 100%;
  height: 400px;
  border: 1px solid #eee;
  border-radius: 4px;
}
.fragment-content {
  padding: 10px;
  line-height: 1.8;
  color: #333;
}
</style>