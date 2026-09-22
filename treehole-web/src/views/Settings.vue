<template>
  <div class="settings-page">
    <h2 class="title">账号设置</h2>

    <div class="tabs">
      <button
        class="tab-btn"
        :class="{ active: tab === 'profile' }"
        type="button"
        @click="tab = 'profile'"
      >个人资料</button>
      <button
        class="tab-btn"
        :class="{ active: tab === 'security' }"
        type="button"
        @click="tab = 'security'"
      >账号安全</button>
    </div>

    <div v-if="message" class="alert" :class="messageType">{{ message }}</div>

    <!-- 个人资料 -->
    <form v-if="tab === 'profile'" class="card" @submit.prevent="saveProfile">
      <label class="field">
        <span>昵称</span>
        <input v-model="form.nickname" maxlength="20" placeholder="2-20位，中文/字母/数字/下划线" />
      </label>
      <label class="field">
        <span>性别</span>
        <select v-model.number="form.gender">
          <option :value="0">保密</option>
          <option :value="1">男</option>
          <option :value="2">女</option>
        </select>
      </label>
      <label class="field">
        <span>邮箱</span>
        <input v-model="form.email" type="email" placeholder="用于接收重要通知（可选）" />
      </label>
      <label class="field">
        <span>头像链接</span>
        <input v-model="form.avatar" placeholder="https://…（暂支持外链，图片上传能力开发中）" />
      </label>
      <label class="field">
        <span>个人简介</span>
        <textarea v-model="form.bio" maxlength="200" rows="3" placeholder="介绍一下自己（200字以内）"></textarea>
      </label>
      <button class="primary" type="submit" :disabled="saving">{{ saving ? '保存中…' : '保存资料' }}</button>
    </form>

    <!-- 账号安全 -->
    <form v-else class="card" @submit.prevent="savePassword">
      <label class="field">
        <span>当前密码</span>
        <input v-model="pwd.oldPassword" type="password" autocomplete="current-password" />
      </label>
      <label class="field">
        <span>新密码</span>
        <input v-model="pwd.newPassword" type="password" autocomplete="new-password"
               placeholder="至少8位，含大小写字母、数字和特殊字符" />
      </label>
      <label class="field">
        <span>确认新密码</span>
        <input v-model="pwd.confirm" type="password" autocomplete="new-password" />
      </label>
      <p class="tip">修改成功后需要重新登录。</p>
      <button class="primary" type="submit" :disabled="saving">{{ saving ? '提交中…' : '修改密码' }}</button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo } from '../api/auth'
import { changePassword, logout } from '../api/auth'
import { updateProfile } from '../api/treehole'

const router = useRouter()

const tab = ref<'profile' | 'security'>('profile')
const saving = ref(false)
const message = ref('')
const messageType = ref<'ok' | 'err'>('ok')

const form = ref({ nickname: '', gender: 0, email: '', avatar: '', bio: '' })
const pwd = ref({ oldPassword: '', newPassword: '', confirm: '' })

function flash(text: string, type: 'ok' | 'err' = 'ok') {
  message.value = text
  messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}

async function loadProfile() {
  try {
    const res = await getUserInfo()
    const data = res.data || {}
    form.value = {
      nickname: data.nickname || '',
      gender: data.gender ?? 0,
      email: data.email || '',
      avatar: data.avatar || '',
      bio: data.bio || ''
    }
  } catch (e) {
    console.error('加载资料失败', e)
  }
}

async function saveProfile() {
  if (form.value.nickname && (form.value.nickname.length < 2 || form.value.nickname.length > 20)) {
    flash('昵称长度应为 2-20 位', 'err')
    return
  }
  saving.value = true
  try {
    await updateProfile({
      nickname: form.value.nickname || undefined,
      gender: form.value.gender,
      email: form.value.email || undefined,
      avatar: form.value.avatar || undefined,
      bio: form.value.bio || undefined
    })
    if (form.value.nickname) localStorage.setItem('th_nickname', form.value.nickname)
    window.dispatchEvent(new Event('storage'))
    flash('资料已保存')
  } catch (e: any) {
    flash(e?.response?.data?.message || '保存失败，请稍后重试', 'err')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  if (!pwd.value.oldPassword || !pwd.value.newPassword) {
    flash('请填写完整密码信息', 'err')
    return
  }
  if (pwd.value.newPassword !== pwd.value.confirm) {
    flash('两次输入的新密码不一致', 'err')
    return
  }
  saving.value = true
  try {
    await changePassword({ oldPassword: pwd.value.oldPassword, newPassword: pwd.value.newPassword })
    flash('密码修改成功，请重新登录')
    setTimeout(() => {
      logout()
      router.push('/login')
    }, 1200)
  } catch (e: any) {
    flash(e?.response?.data?.message || '修改失败，请检查当前密码', 'err')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.settings-page { max-width: 640px; margin: 0 auto; }
.title { font-size: 20px; color: #1e293b; margin: 0 0 18px; }
.tabs { display: flex; gap: 8px; margin-bottom: 18px; }
.tab-btn {
  background: #f8fafc; border: 1px solid #e2e8f0; color: #64748b;
  font-size: 13px; padding: 7px 16px; border-radius: 8px; cursor: pointer;
}
.tab-btn.active { background: #3b82f6; border-color: #3b82f6; color: #fff; }
.card {
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px;
  padding: 22px; display: flex; flex-direction: column; gap: 16px;
}
.field { display: flex; flex-direction: column; gap: 6px; }
.field > span { font-size: 13px; color: #475569; }
.field input, .field select, .field textarea {
  border: 1px solid #e2e8f0; border-radius: 8px; padding: 10px 12px;
  font-size: 14px; outline: none; font-family: inherit; transition: border-color 0.2s;
}
.field input:focus, .field select:focus, .field textarea:focus {
  border-color: #93c5fd; box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.10);
}
.field textarea { resize: vertical; }
.primary {
  background: #3b82f6; color: #fff; border: none; border-radius: 8px;
  padding: 11px 0; font-size: 14px; cursor: pointer; transition: background 0.2s;
}
.primary:hover { background: #2563eb; }
.primary:disabled { background: #93c5fd; cursor: not-allowed; }
.tip { margin: 0; color: #94a3b8; font-size: 12px; }
.alert { padding: 10px 14px; border-radius: 8px; font-size: 13px; margin-bottom: 16px; }
.alert.ok { background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
.alert.err { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
</style>
