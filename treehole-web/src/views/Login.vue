<template>
  <AuthLayout>
    <AppIcon name="tree" :size="38" />
    <h2>登录树洞</h2>
    <p class="sub">登录后即可匿名发布与回响</p>

    <div class="field">
      <label class="dh-label">用户名</label>
      <div class="dh-input">
        <AppIcon name="user" :size="17" class="ico" />
        <input v-model.trim="form.username" placeholder="请输入用户名" autocomplete="username" @keyup.enter="handleLogin" />
      </div>
      <p v-if="errors.username" class="dh-error">{{ errors.username }}</p>
    </div>

    <div class="field">
      <label class="dh-label">密码</label>
      <div class="dh-input">
        <AppIcon name="lock" :size="17" class="ico" />
        <input
          v-model="form.password"
          :type="showPwd ? 'text' : 'password'"
          placeholder="请输入密码"
          autocomplete="current-password"
          @keyup.enter="handleLogin"
        />
        <button type="button" class="pwd-toggle" :aria-label="showPwd ? '隐藏密码' : '显示密码'" @click="showPwd = !showPwd">
          <AppIcon :name="showPwd ? 'eye-off' : 'eye'" :size="16" />
        </button>
      </div>
      <p v-if="errors.password" class="dh-error">{{ errors.password }}</p>
    </div>

    <button class="dh-btn dh-btn--primary dh-btn--block dh-btn--lg" type="button" :disabled="loading" @click="handleLogin">
      {{ loading ? '登录中…' : '登 录' }}
    </button>

    <div class="divider">OR</div>

    <div class="foot">还没有账号？<router-link to="/register">立即注册</router-link></div>
    <p class="tiny">我们不会记录你的真实身份<br />请勿在帖子里留下真实姓名与联系方式</p>
  </AuthLayout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthLayout from '../components/AuthLayout.vue'
import AppIcon from '../components/AppIcon.vue'
import { login } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const showPwd = ref(false)
const form = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '' })

function validate() {
  errors.username = form.username ? '' : '请输入用户名'
  errors.password = form.password ? '' : '请输入密码'
  return !errors.username && !errors.password
}

async function handleLogin() {
  if (!validate()) return
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    localStorage.setItem('th_token', res.data.token)
    localStorage.setItem('th_nickname', res.data.nickname || form.username)
    window.dispatchEvent(new Event('storage'))
    ElMessage.success('欢迎回来')
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '登录失败')
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
.tiny { margin-top: 24px; text-align: center; font-size: 11.5px; color: var(--text-mute); line-height: 1.7; }
</style>
