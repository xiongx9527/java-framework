import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicMqttClientSubscribe from './service/magic-mqttclient-subscribe.js'
import MagicMqttClientDataSource from './service/magic-mqttclient-datasource.js'
import MagicMqttClientDatasourceInfo from './components/magic-mqttclient-datasource-info.vue'
import MagicMqttClientSubscribeInfo from './components/magic-mqttclient-subscribe-info.vue'

// Manually inject SVG icon
if (typeof window !== 'undefined') {
    const svgBody = '<symbol class="icon" viewBox="0 0 24 24" id="magic-mqttclient-mqttclient"><path d="M11.987 1.913c-1.9 0-3.693.321-5.298.883C2.756 4.268 0 7.826 0 12c0 4.147 2.756 7.706 6.689 9.204 1.632.562 3.425.883 5.325.883a16.74 16.74 0 0 0 5.27-.856C21.217 19.759 24 16.174 24 12.027V12c0-4.174-2.783-7.732-6.716-9.204a16.295 16.295 0 0 0-5.297-.883zM10.89 5.257h2.167v3.827c1.525-.402 2.702-1.766 2.782-3.399l2.168.027c.16 2.73-2.22 4.95-4.897 5.378v1.793c2.676.428 4.736 2.675 4.924 5.404l-2.167.028c-.08-1.633-1.258-2.997-2.783-3.425v3.853h-2.167V14.89a3.775 3.775 0 0 0-2.81 3.425l-2.167-.028a5.868 5.868 0 0 1 4.923-5.404v-1.766C8.187 10.716 6.1 8.468 5.94 5.74l2.167-.027A3.711 3.711 0 0 0 10.89 9.11Z"/></symbol>'
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
            type: 'mqttclient-subscribe',
            icon: '#magic-mqttclient-mqttclient',
            title: 'mqttclient.subscribe.name',
            service: MagicMqttClientSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
        }],
        datasources: [{
            type: 'mqttclient-datasource',
            icon: '#magic-mqttclient-mqttclient',
            title: i18n.format('mqttclient.datasource.title'),
            name: i18n.format('mqttclient.datasource.name'),
            service: MagicMqttClientDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicMqttClientDatasourceInfo
        }],
        toolbars: [{
            type: 'mqttclient-subscribe',
            title: 'mqttclient.subscribe.title',
            icon: 'parameter',
            component: MagicMqttClientSubscribeInfo
        }]
    }
}
