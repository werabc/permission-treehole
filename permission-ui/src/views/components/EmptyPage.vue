<template>
  <div class="empty-page">
    <el-empty description="该页面暂无内容，请在左侧菜单选择子页面">
      <el-button type="primary" @click="goBack">返回首页</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
function goBack() {
  router.push('/dashboard')
}
</script>

<style scoped>
.empty-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}
</style>
