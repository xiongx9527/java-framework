import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicKafkaSubscribe from './service/magic-kafka-subscribe.js'
import MagicKafkaDataSource from './service/magic-kafka-datasource.js'
import MagicKafkaDatasourceInfo from './components/magic-kafka-datasource-info.vue'
import MagicKafkaSubscribeInfo from './components/magic-kafka-subscribe-info.vue'
import MagicKafka from './components/magic-kafka.vue'
import { setMagicKafkaRuntime } from './service/magic-kafka-runtime.js'

// Manually inject SVG icon (vite-plugin-svg-icons doesn't work in library/IIFE mode)
if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 1024 1024" id="magic-kafka-kafka"><path d="M725.333 597.333c-38.4 0-72.533 17.067-96 44.8L524.8 582.4c6.4-19.2 10.667-38.4 10.667-57.6 0-23.467-4.267-44.8-12.8-66.133l115.2-66.134c23.466 21.334 53.333 36.267 89.6 36.267 70.4 0 128-57.6 128-128s-57.6-128-128-128-128 57.6-128 128c0 12.8 2.133 25.6 6.4 38.4l-115.2 66.133a170.24 170.24 0 0 0-104.534-57.6v-115.2c36.267-12.8 64-49.066 64-89.6C448 85.333 405.333 42.667 352 42.667s-96 42.666-96 96c0 42.666 27.733 76.8 64 89.6v115.2c-85.333 14.933-149.333 89.6-149.333 179.2s64 164.266 149.333 179.2v93.866c-36.267 12.8-64 49.067-64 89.6 0 53.334 42.667 96 96 96s96-42.666 96-96c0-42.666-27.733-76.8-64-89.6v-93.866c44.8-8.534 81.067-32 108.8-64l106.667 61.866c-2.134 8.534-2.134 17.067-2.134 25.6 0 70.4 57.6 128 128 128s128-57.6 128-128-57.6-128-128-128z"/></symbol>'
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
    setMagicKafkaRuntime(opt)
    return {
        name: 'kafka',
        statusBarItems: [{
            icon: '#magic-kafka-kafka',
            title: 'Kafka',
            displayKey: 'kafka',
            onClick: () => window.open('https://kafka.apache.org/', '_blank')
        }],
        resource: [{
            type: 'kafka-subscribe',
            icon: '#magic-kafka-kafka',
            title: 'kafka.subscribe.name',
            service: MagicKafkaSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
        }],
        datasources: [{
            type: 'kafka-datasource',
            icon: '#magic-kafka-kafka',
            title: i18n.format('kafka.datasource.title'),
            name: i18n.format('kafka.datasource.name'),
            service: MagicKafkaDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicKafkaDatasourceInfo
        }],
        toolbars: [{
            type: 'kafka-subscribe',
            title: 'kafka.subscribe.title',
            icon: '#magic-kafka-kafka',
            component: MagicKafkaSubscribeInfo
        }, {
            id: 'kafka',
            title: i18n.format('kafka.title'),
            icon: '#magic-kafka-kafka',
            component: MagicKafka,
            adminOnly: true,
            allowClose: true,
            fullscreen: true
        }]
    }
}
