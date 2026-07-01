import * as Vue from 'vue'
import App from './App.vue'
import './assets/index.css'
import MagicEditor from './magic-editor.js'
import 'vite-plugin-svg-icons/register'
window.Vue = Vue
const app = Vue.createApp(App);
app.use(MagicEditor)
app.mount('#app')