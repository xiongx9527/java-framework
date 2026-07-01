import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicSseSubscribe from './service/magic-sse-subscribe.js'
import MagicSseDataSource from './service/magic-sse-datasource.js'
import MagicSseDatasourceInfo from './components/magic-sse-datasource-info.vue'
import MagicSseSubscribeInfo from './components/magic-sse-subscribe-info.vue'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    let loadSvg = function() {
        var body = document.body
        var svgDom = document.getElementById('__svg__icons__dom__1717728641709__sse')
        if (!svgDom) {
            svgDom = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
            svgDom.style.position = 'absolute'
            svgDom.style.width = '0'
            svgDom.style.height = '0'
            svgDom.id = '__svg__icons__dom__1717728641709__sse'
            svgDom.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
            svgDom.setAttribute('xmlns:link', 'http://www.w3.org/1999/xlink')
        }
        svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 128 128" id="magic-sse-sse"><path d="M94.9 71.35c-8.08-7.63-21.82-13.99-30.91-18.2c-1.54-.71-2.93-1.35-4.11-1.92c-3.43-1.65-8.12-6.22-6.1-11.47c1.36-3.55 4.81-5.34 10.27-5.34c1.75 0 3.67.2 5.72.58c7.12 1.33 12.52 3.99 15.58 5.5c.38.19.82.21 1.22.07c.39-.15.72-.45.89-.84l7.01-15.81c.31-.69.07-1.51-.57-1.93c-4.94-3.28-17.27-8.15-30.97-8.15c-1.98 0-3.95.1-5.87.3c-10.92 1.12-21.86 4.03-27.92 17.64c-3.78 8.47-3.77 18.01.01 24.89c4.04 7.51 10.66 10.87 19.04 15.11l1.08.55c6.05 3.02 13.3 6.38 18.07 8.59c7.05 3.4 9.66 9.44 8.04 13.08c-2.61 5.87-8.38 7.16-18.36 4.33c-8.81-2.39-16.94-9.14-19.21-11.15c-.34-.3-.79-.44-1.25-.38c-.45.06-.85.32-1.1.69L24.6 104.14c-.41.63-.32 1.46.22 1.98c5.63 5.43 10.22 8.59 18.17 12.5c5.35 2.63 13.18 4.21 20.95 4.21c8.12 0 27.95-1.88 36.65-19.26c5.55-11.12 3.48-22.84-5.69-32.22z" fill="#40C0E7"></path></symbol>'
        body.insertBefore(svgDom, body.firstChild)
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadSvg)
    } else {
        loadSvg()
    }
}

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        resource: [{
            type: 'sse-subscribe',
            icon: '#magic-sse-sse',
            title: 'sse.subscribe.name',
            service: MagicSseSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
        }],
        datasources: [{
            type: 'sse-datasource',
            icon: '#magic-sse-sse',
            title: i18n.format('sse.datasource.title'),
            name: i18n.format('sse.datasource.name'),
            service: MagicSseDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicSseDatasourceInfo
        }],
        toolbars: [{
            type: 'sse-subscribe',
            title: 'sse.subscribe.title',
            icon: '#magic-sse-sse',
            component: MagicSseSubscribeInfo
        }]
    }
}