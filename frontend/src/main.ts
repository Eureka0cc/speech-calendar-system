import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'element-plus/theme-chalk/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'

const app = createApp(App)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
