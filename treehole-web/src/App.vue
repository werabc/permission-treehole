<template>
  <div class="app">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <span class="logo-icon">🌳</span>
          <span class="logo-text">树洞</span>
        </router-link>
        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link to="/publish">发布</router-link>
        </nav>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <footer class="footer">
      <p>© 2026 树洞 - 匿名分享你的故事</p>
    </footer>
  </div>
</template>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  text-decoration: none;
}

.logo:hover {
  text-decoration: none;
}

.logo-icon {
  font-size: 28px;
}

.nav {
  display: flex;
  gap: 20px;
}

.nav a {
  color: #64748b;
  font-size: 15px;
  font-weight: 500;
  text-decoration: none;
  padding: 6px 16px;
  border-radius: 6px;
  transition: all 0.2s;
}

.nav a:hover,
.nav a.router-link-active {
  color: #3b82f6;
  background: #eff6ff;
  text-decoration: none;
}

.main {
  flex: 1;
  padding: 24px 20px;
}

.footer {
  text-align: center;
  padding: 24px;
  color: #94a3b8;
  font-size: 13px;
}
</style>
