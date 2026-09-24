<template>
  <div class="auth">
    <div class="auth__art">
      <img src="../assets/auth-aurora-tree.jpg" alt="" aria-hidden="true" />
      <div class="auth__quote">
        <p>“愿你被这世界温柔以待，也愿你成为别人的光。”</p>
        <span>Deep Hollow · 树洞</span>
      </div>
    </div>
    <div class="auth__panel">
      <div class="auth__box">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 登录 / 注册共用的整屏分屏布局：左侧情绪画面，右侧表单 */
</script>

<style scoped>
.auth { display: grid; grid-template-columns: 1.05fr 0.95fr; min-height: 100dvh; }
.auth__art { position: relative; overflow: hidden; }
.auth__art img {
  position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover;
  opacity: 0.82; animation: auth-kb 34s ease-in-out infinite alternate;
}
.auth__art::after {
  content: ""; position: absolute; inset: 0;
  background: linear-gradient(90deg, transparent 40%, var(--bg) 100%);
}
.auth__quote { position: absolute; left: 56px; bottom: 64px; z-index: 2; max-width: 24ch; }
/* 浅色主题下引语是白字压浅雾，对比度不足：给文字后面加一道局部暗角 */
.auth__quote::before {
  content: ""; position: absolute; inset: -18px -22px -14px -22px; z-index: -1;
  background: radial-gradient(ellipse at 30% 70%, rgba(6, 6, 13, 0.42), transparent 72%);
  filter: blur(6px);
}
html[data-theme="dark"] .auth__quote::before { background: radial-gradient(ellipse at 30% 70%, rgba(6, 6, 13, 0.3), transparent 72%); }
.auth__quote p {
  font-family: var(--font-display); font-style: italic; font-size: 26px; line-height: 1.5;
  color: #fff; text-shadow: 0 2px 30px rgba(0, 0, 0, 0.6);
}
.auth__quote span {
  display: block; margin-top: 14px; font-family: var(--font-mono); font-size: 11px;
  letter-spacing: 0.18em; color: rgba(255, 255, 255, 0.65); text-transform: uppercase;
}
.auth__panel { display: grid; place-items: center; padding: 60px 24px; }
.auth__box { width: 100%; max-width: 372px; }

@keyframes auth-kb { from { transform: scale(1); } to { transform: scale(1.08) translate3d(-1%, -1%, 0); } }

@media (max-width: 980px) {
  .auth { grid-template-columns: 1fr; min-height: 100dvh; }
  .auth__art { min-height: 240px; }
  .auth__art::after { background: linear-gradient(180deg, transparent 40%, var(--bg) 100%); }
  .auth__quote { left: 28px; bottom: 24px; }
  .auth__quote p { font-size: 19px; }
  .auth__panel { padding: 40px 20px 64px; }
}
</style>
