import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicDbBrowser from './components/magic-db-browser.vue'
import { setDbBrowserRequest } from './service/magic-db-browser.js'

if (typeof window !== 'undefined') {
    const body = '<symbol id="magic-db-browser" viewBox="0 0 24 24"><path d="M3 5c0-2 4-3 9-3s9 1 9 3v14c0 2-4 3-9 3s-9-1-9-3V5zm9-1C7.6 4 5 4.9 5 5s2.6 1 7 1 7-.9 7-1-2.6-1-7-1zm7 4.2C17.3 8.8 14.7 9 12 9S6.7 8.8 5 8.2V11c0 .1 2.6 1 7 1s7-.9 7-1V8.2zm0 5C17.3 13.8 14.7 14 12 14s-5.3-.2-7-.8V16c0 .1 2.6 1 7 1s7-.9 7-1v-2.8z"/></symbol>'
    const root = document.getElementById('__svg__icons__dom__')
    if (root) root.innerHTML += body
    else {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
        svg.style.cssText = 'position:absolute;width:0;height:0'
        svg.id = '__svg__icons__dom__'
        svg.innerHTML = body
        document.body.insertBefore(svg, document.body.firstChild)
    }
}

export default opt => {
    opt.i18n.add('zh-cn', localZhCN)
    opt.i18n.add('en', localEn)
    setDbBrowserRequest(opt.request)
    return {
        // 顶层 name 必须与后端 Plugin name("db") 一致：magic-toolbar 用
        // PLUGINS_DATA.find(item => item.name === plugin.name) 反查 adminEnabled，
        // 缺失会导致 adminOnly 的 db-browser 入口永远被过滤掉。
        name: 'db',
        toolbars: [{
            id: 'db-browser',
            title: opt.i18n.format('db.title'),
            icon: '#magic-db-browser',
            order: 200,
            component: MagicDbBrowser,
            adminOnly: true,
            allowClose: true,
            fullscreen: true
        }]
    }
}
