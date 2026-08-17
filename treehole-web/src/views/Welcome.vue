<template>
  <Transition name="welcome">
    <div v-if="visible" class="welcome-overlay" @click="dismiss">
      <div class="welcome-content">
        <div class="welcome-tree">🌳</div>
        <h1 class="welcome-title">欢迎来到树洞</h1>
        <p class="welcome-subtitle">匿名分享你的故事，安全保密</p>
        <div class="welcome-features">
          <div class="feature">
            <span class="feature-icon">🌙</span>
            <span>匿名发布</span>
          </div>
          <div class="feature">
            <span class="feature-icon">💬</span>
            <span>自由评论</span>
          </div>
          <div class="feature">
            <span class="feature-icon">🔒</span>
            <span>安全保密</span>
          </div>
        </div>
        <button class="welcome-btn" @click.stop="dismiss">开始探索</button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const visible = ref(false)
const route = useRoute()

function dismiss() {
  visible.value = false
  localStorage.setItem('th_welcomed', '1')
}

onMounted(() => {
  // 不在登录/注册页面显示欢迎动画
  const path = route.path
  if (path === '/login' || path === '/register') return
  if (!localStorage.getItem('th_welcomed')) {
    visible.value = true
  }
})
</script>

<style scoped>
.welcome-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.welcome-content {
  text-align: center;
  color: #fff;
  animation: fadeInUp 0.8s ease;
}

.welcome-tree {
  font-size: 80px;
  margin-bottom: 20px;
  animation: sway 3s ease-in-out infinite;
}

.welcome-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #a78bfa, #60a5fa, #34d399);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-subtitle {
  font-size: 16px;
  color: #94a3b8;
  margin-bottom: 32px;
}

.welcome-features {
  display: flex;
  gap: 32px;
  justify-content: center;
  margin-bottom: 40px;
}

.feature {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.feature-icon {
  font-size: 32px;
}

.feature span:last-child {
  font-size: 14px;
  color: #cbd5e1;
}

.welcome-btn {
  padding: 14px 48px;
  border-radius: 30px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.welcome-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.welcome-enter-active {
  transition: opacity 0.5s ease;
}

.welcome-leave-active {
  transition: opacity 0.6s ease;
}

.welcome-enter-from,
.welcome-leave-to {
  opacity: 0;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes sway {
  0%, 100% { transform: rotate(-3deg); }
  50% { transform: rotate(3deg); }
}
</style>
