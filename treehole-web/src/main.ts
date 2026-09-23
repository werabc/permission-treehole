import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// Element Plus 深色变量（配合 html.dark 生效）
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './assets/main.css'
import { reveal } from './directives/reveal'

const app = createApp(App)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.directive('reveal', reveal)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
