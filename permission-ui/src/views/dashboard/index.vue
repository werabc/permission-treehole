<template>
  <div class="dashboard">
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #667eea, #764ba2)">
          <div class="stat-icon"><el-icon :size="32"><UserFilled /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">当前用户</div>
            <div class="stat-value">{{ userStore.userInfo?.nickname }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #f093fb, #f5576c)">
          <div class="stat-icon"><el-icon :size="32"><Key /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">拥有角色</div>
            <div class="stat-value">{{ userStore.roles.length }} 个</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #4facfe, #00f2fe)">
          <div class="stat-icon"><el-icon :size="32"><Lock /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">权限标识</div>
            <div class="stat-value">{{ userStore.permissions.length }} 个</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #43e97b, #38f9d7)">
          <div class="stat-icon"><el-icon :size="32"><OfficeBuilding /></el-icon></div>
          <div class="stat-content">
            <div class="stat-label">所属部门</div>
            <div class="stat-value">{{ userStore.userInfo?.deptName || '未分配' }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>角色信息</span>
            </div>
          </template>
          <el-tag v-for="role in userStore.roles" :key="role" type="success" style="margin: 4px" size="large">
            {{ role }}
          </el-tag>
          <el-empty v-if="userStore.roles.length === 0" description="暂无角色" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>系统信息</span>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="系统名称">企业级权限管理系统</el-descriptions-item>
            <el-descriptions-item label="后端框架">Spring Boot 3.2 + JDK 17</el-descriptions-item>
            <el-descriptions-item label="前端框架">Vue 3 + Element Plus</el-descriptions-item>
            <el-descriptions-item label="安全框架">Spring Security 6 + JWT</el-descriptions-item>
            <el-descriptions-item label="数据库">MySQL 8.0 + Redis</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { UserFilled, Key, Lock, OfficeBuilding } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.stats-row {
  margin-bottom: 0;
}

.stat-card {
  border-radius: 12px;
  padding: 24px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  opacity: 0.8;
}

.stat-label {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
}

.card-header {
  font-weight: 600;
  font-size: 15px;
}
</style>
