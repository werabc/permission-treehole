<template>
  <div class="dh-narrow settings">
    <div class="page-hd" v-reveal>
      <h2 class="dh-page-title">账号设置</h2>
      <p class="dh-page-sub">管理你的公开资料与账号安全。</p>
      <div class="dh-tabs head-tabs">
        <button type="button" :class="{ 'is-on': tab === 'profile' }" @click="tab = 'profile'">个人资料</button>
        <button type="button" :class="{ 'is-on': tab === 'security' }" @click="tab = 'security'">账号安全</button>
      </div>
    </div>

    <!-- ============ 个人资料 ============ -->
    <form v-if="tab === 'profile'" class="dh-card pane" v-reveal @submit.prevent="saveProfile">
      <div class="field">
        <label class="dh-label">昵称</label>
        <div class="dh-input">
          <AppIcon name="user" :size="17" class="ico" />
          <input v-model="form.nickname" maxlength="20" placeholder="2-20 位，中文/字母/数字/下划线" />
        </div>
      </div>

      <div class="field">
        <label class="dh-label">性别</label>
        <div class="dh-seg">
          <button type="button" :class="{ 'is-on': form.gender === 0 }" @click="form.gender = 0">保密</button>
          <button type="button" :class="{ 'is-on': form.gender === 1 }" @click="form.gender = 1">男</button>
          <button type="button" :class="{ 'is-on': form.gender === 2 }" @click="form.gender = 2">女</button>
        </div>
      </div>

      <div class="field">
        <label class="dh-label">邮箱</label>
        <div class="dh-input">
          <AppIcon name="mail" :size="17" class="ico" />
          <input v-model="form.email" type="email" placeholder="用于接收重要通知（可选）" />
        </div>
      </div>

      <div class="field">
        <label class="dh-label">头像链接</label>
        <div class="dh-input">
          <AppIcon name="image" :size="17" class="ico" />
          <input v-model="form.avatar" placeholder="https://…（暂支持外链）" />
        </div>
      </div>

      <div class="field">
        <label class="dh-label">个人简介</label>
        <textarea v-model="form.bio" class="ta" maxlength="200" rows="3" placeholder="介绍一下自己（200 字以内）" />
      </div>

      <div class="pane__acts">
        <button class="dh-btn dh-btn--primary dh-btn--lg" type="submit" :disabled="saving">
          <AppIcon name="check" :size="16" /> {{ saving ? '保存中…' : '保存资料' }}
        </button>
      </div>
    </form>

    <!-- ============ 账号安全 ============ -->
    <form v-else class="dh-card pane" v-reveal @submit.prevent="savePassword">
      <div class="field">
        <label class="dh-label">当前密码</label>
        <div class="dh-input">
          <AppIcon name="lock" :size="17" class="ico" />
          <input v-model="pwd.oldPassword" type="password" autocomplete="current-password" placeholder="请输入当前密码" />
        </div>
      </div>

      <div class="field">
        <label class="dh-label">新密码</label>
        <div class="dh-input">
          <AppIcon name="shield" :size="17" class="ico" />
          <input v-model="pwd.newPassword" type="password" autocomplete="new-password" placeholder="至少 8 位，含大小写字母、数字与特殊字符" />
        </div>
      </div>

      <div class="field">
        <label class="dh-label">确认新密码</label>
        <div class="dh-input">
          <AppIcon name="shield" :size="17" class="ico" />
          <input v-model="pwd.confirm" type="password" autocomplete="new-password" placeholder="再次输入新密码" />
        </div>
      </div>

      <p class="tip">修改成功后，所有已登录设备都需要重新登录。</p>

      <div class="pane__acts">
        <button class="dh-btn dh-btn--primary dh-btn--lg" type="submit" :disabled="saving">
          <AppIcon name="lock" :size="16" /> {{ saving ? '提交中…' : '修改密码' }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppIcon from '../components/AppIcon.vue'
import { getUserInfo, changePassword, logout } from '../api/auth'
import { updateProfile } from '../api/treehole'

const router = useRouter()

const tab = ref<'profile' | 'security'>('profile')
const saving = ref(false)

const form = ref({ nickname: '', gender: 0, email: '', avatar: '', bio: '' })
const pwd = ref({ oldPassword: '', newPassword: '', confirm: '' })

async function loadProfile() {
  try {
    const res = await getUserInfo()
    const data = res.data || {}
    form.value = {
      nickname: data.nickname || '',
      gender: data.gender ?? 0,
      email: data.email || '',
      avatar: data.avatar || '',
      bio: data.bio || '',
    }
  } catch {
    ElMessage.error('加载资料失败')
  }
}

async function saveProfile() {
  const name = form.value.nickname
  if (name && (name.length < 2 || name.length > 20)) {
    ElMessage.warning('昵称长度应为 2-20 位')
    return
  }
  saving.value = true
  try {
    await updateProfile({
      nickname: name || undefined,
      gender: form.value.gender,
      email: form.value.email || undefined,
      avatar: form.value.avatar || undefined,
      bio: form.value.bio || undefined,
    })
    if (name) localStorage.setItem('th_nickname', name)
    window.dispatchEvent(new Event('storage'))
    ElMessage.success('资料已保存')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  if (!pwd.value.oldPassword || !pwd.value.newPassword) {
    ElMessage.warning('请填写完整的密码信息')
    return
  }
  if (pwd.value.newPassword !== pwd.value.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await changePassword({ oldPassword: pwd.value.oldPassword, newPassword: pwd.value.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    setTimeout(() => {
      logout()
      router.push('/login')
    }, 1200)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '修改失败，请检查当前密码')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.settings { padding-bottom: 80px; }
.page-hd { padding: 44px 0 8px; }
.head-tabs { margin-top: 22px; }

.pane { margin-top: 22px; padding: 28px 30px; border-radius: var(--r-xl); }
.field { margin-bottom: 22px; }
.ico { color: var(--text-mute); }

.ta {
  width: 100%; padding: 14px 16px; font-size: 15px; line-height: 1.75; resize: vertical;
  border: 1px solid var(--border); border-radius: var(--r-md); background: var(--surface);
  transition: border-color 0.35s var(--ease), box-shadow 0.35s var(--ease);
}
.ta:focus { border-color: var(--accent-line); box-shadow: 0 0 0 4px var(--accent-soft); }
.ta::placeholder { color: var(--text-mute); }

.tip { font-size: 12.5px; color: var(--text-mute); margin-bottom: 4px; }
.pane__acts { display: flex; justify-content: flex-end; margin-top: 26px; }

@media (max-width: 720px) {
  .page-hd { padding-top: 32px; }
  .pane { padding: 22px; }
  .pane__acts .dh-btn { width: 100%; }
}
</style>
