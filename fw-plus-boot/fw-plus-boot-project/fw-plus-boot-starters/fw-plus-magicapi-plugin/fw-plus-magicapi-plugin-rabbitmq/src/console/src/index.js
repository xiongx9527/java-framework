import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicRabbitMqSubscribe from './service/magic-rabbitmq-subscribe.js'
import MagicRabbitMqDataSource from './service/magic-rabbitmq-datasource.js'
import MagicRabbitMqDatasourceInfo from './components/magic-rabbitmq-datasource-info.vue'
import MagicRabbitMqSubscribeInfo from './components/magic-rabbitmq-subscribe-info.vue'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    const svgBody = '<symbol class="icon" viewBox="0 0 1024 1024" id="magic-rabbitmq-rabbitmq"><path d="M824.363 439.147H597.312a20.736 20.736 0 0 1-20.48-20.587V180.928a31.488 31.488 0 0 0-9.173-22.379 31.232 31.232 0 0 0-22.272-9.216h-81.259a31.232 31.232 0 0 0-22.272 9.216 31.467 31.467 0 0 0-9.152 22.379v235.968a22.4 22.4 0 0 1-22.336 22.25l-74.24.62a22.187 22.187 0 0 1-22.123-22.251l.619-236.587a31.488 31.488 0 0 0-9.173-22.379 31.253 31.253 0 0 0-22.294-9.216h-81.066a31.147 31.147 0 0 0-22.251 9.216 31.488 31.488 0 0 0-9.173 22.379v665.45c0 15.809 12.821 28.289 28.117 28.289h626.41c15.723 0 28.14-12.886 28.14-28.288V467.2c-1.025-15.808-13.249-28.053-28.971-28.053zM718.08 689.237a36.951 36.951 0 0 1-10.73 26.219 36.544 36.544 0 0 1-26.091 10.795h-64.107a36.565 36.565 0 0 1-26.09-10.795 36.95 36.95 0 0 1-10.731-26.219v-64.661a36.95 36.95 0 0 1 10.73-26.219 36.48 36.48 0 0 1 26.091-10.773h64.107a36.65 36.65 0 0 1 36.821 36.992v64.661zm0 0"/></symbol>'
    const svgDom = document.getElementById('__svg__icons__dom__')
    if (svgDom) {
        svgDom.innerHTML += svgBody
    } else {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
        svg.style.position = 'absolute'
        svg.style.width = '0'
        svg.style.height = '0'
        svg.id = '__svg__icons__dom__'
        svg.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
        svg.setAttribute('xmlns:link', 'http://www.w3.org/1999/xlink')
        svg.innerHTML = svgBody
        document.body.insertBefore(svg, document.body.firstChild)
    }
}

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        resource: [{
            type: 'rabbitmq-subscribe',
            icon: '#magic-rabbitmq-rabbitmq',
            title: 'rabbitmq.subscribe.name',
            service: MagicRabbitMqSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
        }],
        datasources: [{
            type: 'rabbitmq-datasource',
            icon: '#magic-rabbitmq-rabbitmq',
            title: i18n.format('rabbitmq.datasource.title'),
            name: i18n.format('rabbitmq.datasource.name'),
            service: MagicRabbitMqDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicRabbitMqDatasourceInfo
        }],
        toolbars: [{
            type: 'rabbitmq-subscribe',
            title: 'rabbitmq.subscribe.title',
            icon: '#magic-rabbitmq-rabbitmq',
            component: MagicRabbitMqSubscribeInfo
        }]
    }
}