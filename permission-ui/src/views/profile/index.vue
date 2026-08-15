<template>
  <div class="profile-page">
    <el-row :gutter="24">
      <!-- 左侧：用户卡片 -->
      <el-col :span="8">
        <el-card shadow="never" class="user-card">
          <div class="user-avatar">
            <el-avatar :size="80" :src="userStore.userInfo?.avatar">
              <el-icon :size="40"><UserFilled /></el-icon>
            </el-avatar>
            <h3 class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h3>
            <p class="user-dept">{{ userStore.userInfo?.deptName || '未分配部门' }}</p>
          </div>
          <el-divider />
          <div class="user-meta">
            <div class="meta-item">
              <el-icon><User /></el-icon>
              <span>用户名：{{ userStore.userInfo?.username }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Message /></el-icon>
              <span>邮箱：{{ profileForm.email || '未设置' }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Iphone /></el-icon>
              <span>手机：{{ profileForm.phone || '未设置' }}</span>
            </div>
            <div class="meta-item">
              <el-icon><Male v-if="profileForm.sex === 1" /><Female v-else-if="profileForm.sex === 2" /><QuestionFilled v-else /></el-icon>
              <span>性别：{{ sexLabel }}</span>
            </div>
          </div>
          <el-divider />
          <div class="user-roles">
            <el-tag v-for="role in userStore.roles" :key="role" type="primary" effect="plain" style="margin: 4px">
              {{ role }}
            </el-tag>
            <el-empty v-if="userStore.roles.length === 0" description="暂无角色" :image-size="40" />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：编辑表单 -->
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>编辑个人信息</span>
            </div>
          </template>
          <el-form ref="formRef" :model="profileForm" :rules="formRules" label-width="100px" style="max-width: 500px">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="请输入手机号" maxlength="20" />
            </el-form-item>
            <el-form-item label="性别" prop="sex">
              <el-radio-group v-model="profileForm.sex">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
                <el-radio :value="0">保密</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">保存修改</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" style="margin-top: 20px">
          <template #header>
            <div class="card-header">
              <span>修改密码</span>
            </div>
          </template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" style="max-width: 500px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少8位，含大小写字母、数字和特殊字符" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdSubmitting" @click="handleChangePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { UserFilled, User, Message, Iphone, Male, Female, QuestionFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateProfile } from '@/api/profile'
import { updatePassword } from '@/api/user'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const pwdFormRef = ref<FormInstance>()
const submitting = ref(false)
const pwdSubmitting = ref(false)

const profileForm = reactive({
  nickname: '',
  email: '',
  phone: '',
  sex: 0,
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const sexLabel = computed(() => {
  const map: Record<number, string> = { 0: '保密', 1: '男', 2: '女' }
  return map[profileForm.sex] || '保密'
})

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  nickname: [{ max: 50, message: '昵称长度不能超过50', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ max: 20, message: '手机号长度不能超过20', trigger: 'blur' }],
}

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

onMounted(() => {
  const info = userStore.userInfo
  if (info) {
    profileForm.nickname = info.nickname || ''
    profileForm.email = (info as any).email || ''
    profileForm.phone = (info as any).phone || ''
    profileForm.sex = (info as any).sex ?? 0
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateProfile(profileForm)
    ElMessage.success('个人信息修改成功')
    await userStore.fetchUserInfo()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  const info = userStore.userInfo
  if (info) {
    profileForm.nickname = info.nickname || ''
    profileForm.email = (info as any).email || ''
    profileForm.phone = (info as any).phone || ''
    profileForm.sex = (info as any).sex ?? 0
  }
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  pwdSubmitting.value = true
  try {
    await updatePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功，请重新登录')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    setTimeout(() => userStore.logoutAction(), 1500)
  } finally {
    pwdSubmitting.value = false
  }
}
</script>

<style scoped>
.profile-page {
  padding: 0;
}

.user-card {
  text-align: center;
}

.user-avatar {
  padding: 20px 0;
}

.user-name {
  margin: 12px 0 4px;
  font-size: 18px;
}

.user-dept {
  color: #909399;
  font-size: 13px;
  margin: 0;
}

.user-meta {
  text-align: left;
  padding: 0 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  color: #606266;
  font-size: 14px;
}

.user-roles {
  text-align: left;
  padding: 0 12px;
}

.card-header {
  font-weight: 600;
  font-size: 15px;
}
</style>
