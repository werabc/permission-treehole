<template>
  <AuthLayout>
    <AppIcon name="tree" :size="38" />
    <h2>注册树洞</h2>
    <p class="sub">创建一个只属于你的匿名身份</p>

    <div class="field">
      <label class="dh-label">用户名</label>
      <div class="dh-input">
        <AppIcon name="user" :size="17" class="ico" />
        <input v-model.trim="form.username" placeholder="3-20 个字符" autocomplete="username" />
      </div>
      <p v-if="errors.username" class="dh-error">{{ errors.username }}</p>
    </div>

    <div class="field">
      <label class="dh-label">密码</label>
      <div class="dh-input">
        <AppIcon name="lock" :size="17" class="ico" />
        <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="至少 6 位" autocomplete="new-password" />
        <button type="button" class="pwd-toggle" :aria-label="showPwd ? '隐藏密码' : '显示密码'" @click="showPwd = !showPwd">
          <AppIcon :name="showPwd ? 'eye-off' : 'eye'" :size="16" />
        </button>
      </div>
      <p v-if="errors.password" class="dh-error">{{ errors.password }}</p>
    </div>

    <div class="field">
      <label class="dh-label">确认密码</label>
      <div class="dh-input">
        <AppIcon name="shield" :size="17" class="ico" />
        <input v-model="form.confirmPassword" :type="showPwd ? 'text' : 'password'" placeholder="再次输入密码" autocomplete="new-password" @keyup.enter="handleRegister" />
      </div>
      <p v-if="errors.confirmPassword" class="dh-error">{{ errors.confirmPassword }}</p>
    </div>

    <button class="dh-btn dh-btn--primary dh-btn--block dh-btn--lg" type="button" :disabled="loading" @click="handleRegister">
      {{ loading ? '注册中…' : '注 册' }}
    </button>

    <div class="divider">OR</div>

    <div class="foot">已有账号？<router-link to="/login">立即登录</router-link></div>
  </AuthLayout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '../components/AuthLayout.vue'
import AppIcon from '../components/AppIcon.vue'
import { register } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const showPwd = ref(false)
const form = reactive({ username: '', password: '', confirmPassword: '' })
const errors = reactive({ username: '', password: '', confirmPassword: '' })

function validate() {
  errors.username = !form.username
    ? '请输入用户名'
    : form.username.length < 3 || form.username.length > 20
      ? '用户名长度 3-20 位'
      : ''
  errors.password = !form.password ? '请输入密码' : form.password.length < 6 ? '密码至少 6 位' : ''
  errors.confirmPassword = !form.confirmPassword
    ? '请确认密码'
    : form.confirmPassword !== form.password
      ? '两次密码不一致'
      : ''
  return !errors.username && !errors.password && !errors.confirmPassword
}

async function handleRegister() {
  if (!validate()) return
  loading.value = true
  try {
    await register({ username: form.username, password: form.password })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
h2 { font-size: 30px; letter-spacing: -0.03em; font-weight: 700; margin-top: 20px; }
.sub { margin-top: 8px; color: var(--text-dim); font-size: 14px; margin-bottom: 26px; }
.field { margin-bottom: 18px; }
.ico { color: var(--text-mute); }
.pwd-toggle { color: var(--text-mute); display: grid; place-items: center; transition: color 0.28s var(--ease); }
.pwd-toggle:hover { color: var(--text); }
.divider {
  display: flex; align-items: center; gap: 14px; margin: 24px 0;
  color: var(--text-mute); font-size: 11px; font-family: var(--font-mono); letter-spacing: 0.14em;
}
.divider::before, .divider::after { content: ""; flex: 1; height: 1px; background: var(--border); }
.foot { text-align: center; font-size: 13.5px; color: var(--text-dim); }
.foot a { color: var(--accent); }
</style>
