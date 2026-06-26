<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo" :class="{ collapsed: isCollapse }">
        <el-icon :size="24" color="#fff"><Lock /></el-icon>
        <span v-show="!isCollapse" class="logo-text">权限管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#001529"
        text-color="#ffffffb3"
        active-text-color="#fff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-menu-item index="/novel">
          <el-icon><Notebook /></el-icon>
          <template #title>小说广场</template>
        </el-menu-item>
        <el-menu-item index="/bookshelf">
          <el-icon><Star /></el-icon>
          <template #title>我的书架</template>
        </el-menu-item>
        <el-menu-item index="/author">
          <el-icon><Edit /></el-icon>
          <template #title>作者中心</template>
        </el-menu-item>

        <template v-for="menu in menuList" :key="menu.id">
          <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.path || String(menu.id)">
            <template #title>
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <span>{{ menu.menuName }}</span>
            </template>
            <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.path">
              <template #title>{{ child.menuName }}</template>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="menu.path">
            <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
            <template #title>{{ menu.menuName }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" :size="20" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Key /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- 修改密码对话框 -->
  <el-dialog v-model="passwordDialogVisible" title="修改密码" width="450px" :close-on-click-modal="false">
    <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <div class="password-input-wrapper">
          <el-input v-model="passwordForm.newPassword" type="password" show-password @input="onPasswordInput" />
          <div v-if="passwordStrength" class="password-strength">
            <div class="strength-bar">
              <div :class="['strength-fill', passwordStrength]" :style="{ width: strengthPercent + '%' }"></div>
            </div>
            <span class="strength-text">{{ strengthLabel }}</span>
          </div>
        </div>
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="handleUpdatePassword">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, Fold, Expand, UserFilled, ArrowDown, User, Key, SwitchButton, HomeFilled, Notebook, Star, Edit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { updatePassword } from '@/api/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const permissionStore = usePermissionStore()
const isCollapse = ref(false)
const activeMenu = computed(() => route.path)

const menuList = computed(() => {
  return permissionStore.menus.filter((m: any) => m.menuType !== 'BUTTON')
})

onMounted(async () => {
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
      await permissionStore.generateRoutes()
    } catch {
      router.push('/login')
    }
  } else {
    await permissionStore.generateRoutes()
  }
})

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logoutAction()
  } else if (command === 'password') {
    passwordDialogVisible.value = true
  } else if (command === 'profile') {
    ElMessage.info('个人信息')
  }
}

// Password change
const passwordDialogVisible = ref(false)
const passwordFormRef = ref()
const passwordLoading = ref(false)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const passwordStrength = ref('')
const strengthPercent = ref(0)
const strengthLabel = ref('')

function onPasswordInput(val: string) {
  if (!val) {
    passwordStrength.value = ''
    strengthPercent.value = 0
    strengthLabel.value = ''
    return
  }
  let score = 0
  if (val.length >= 8) score++
  if (/[a-z]/.test(val)) score++
  if (/[A-Z]/.test(val)) score++
  if (/\d/.test(val)) score++
  if (/[~!@#$%^&*()_+\-=\[\]{}|;:',.<>?/]/.test(val)) score++

  if (score <= 2) {
    passwordStrength.value = 'weak'
    strengthPercent.value = 33
    strengthLabel.value = '弱'
  } else if (score <= 3) {
    passwordStrength.value = 'medium'
    strengthPercent.value = 66
    strengthLabel.value = '中'
  } else {
    passwordStrength.value = 'strong'
    strengthPercent.value = 100
    strengthLabel.value = '强'
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (value && !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+\-=\[\]{}|;:',.<>?/]).{8,}$/.test(value)) {
          callback(new Error('密码至少8位，含大小写字母、数字和特殊字符'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function handleUpdatePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await updatePassword(passwordForm.value.oldPassword, passwordForm.value.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    userStore.logoutAction()
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
  background: #f0f2f5;
}

.layout-aside {
  background-color: #001529;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
}

.logo.collapsed {
  justify-content: center;
  padding: 0;
}

.layout-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  cursor: pointer;
  color: #333;
}

.collapse-btn:hover {
  color: #667eea;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #333;
}

.layout-main {
  padding: 20px;
  background: #f0f2f5;
  overflow-y: auto;
  height: calc(100vh - 60px);
}

.el-menu {
  border-right: none;
}

.password-input-wrapper {
  width: 100%;
}

.password-strength {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background: #ebeef5;
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s, background 0.3s;
}

.strength-fill.weak {
  background: #f56c6c;
}

.strength-fill.medium {
  background: #e6a23c;
}

.strength-fill.strong {
  background: #67c23a;
}

.strength-text {
  font-size: 12px;
  color: #909399;
  min-width: 24px;
}
</style>
