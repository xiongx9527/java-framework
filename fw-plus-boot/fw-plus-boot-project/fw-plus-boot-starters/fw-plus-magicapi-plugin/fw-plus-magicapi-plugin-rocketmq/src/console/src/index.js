import MagicRocketMqSubscribe from './services/MagicRocketMqSubscribe';
import MagicRocketMqDataSource from './services/MagicRocketMqDataSource';
import MagicRocketMqDataSourceInfo from './components/MagicRocketMqDataSourceInfo.vue';
import MagicRocketMqSubscribeInfo from './components/MagicRocketMqSubscribeInfo.vue';
import zhCN from './locales/zh-cn';
import en from './locales/en';

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    let loadSvg = function() {
        var body = document.body
        var svgDom = document.getElementById('__svg__icons__dom__1717728583840__')
        if (!svgDom) {
            svgDom = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
            svgDom.style.position = 'absolute'
            svgDom.style.width = '0'
            svgDom.style.height = '0'
            svgDom.id = '__svg__icons__dom__1717728583840__'
            svgDom.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
            svgDom.setAttribute('xmlns:link', 'http://www.w3.org/1999/xlink')
        }
        svgDom.innerHTML = '<symbol class="icon" viewBox="0 0 1024 1024" id="magic-rocketmq-rocketmq"><path d="M489.984 960.47c-20.267-24.918-43.35-73.9-47.701-101.163l-3.2-20.011h71.936c56.234 0 71.893 1.408 71.893 6.485 0 13.739-16.17 60.715-27.776 80.811-13.056 22.528-40.021 55.168-45.227 54.741-1.749-.17-10.709-9.514-19.925-20.864zM312.363 796.5c-49.067-117.504-48.214-265.344 2.176-415.744 30.805-91.904 103.936-222.506 172.97-308.992l23.211-29.098 16.555 19.285c29.866 34.944 102.357 145.92 128.512 196.95 51.712 100.863 80.512 195.285 88.746 290.9 7.68 90.027-6.954 189.185-37.717 255.531l-13.27 28.587-28.5-16.939-28.502-16.938H510.037c-96.64 0-128.81 1.536-136.234 6.485a889.6 889.6 0 0 1-27.606 17.28l-17.92 10.752-15.914-38.059zm-140.886 21.931c1.451-6.059 4.267-22.059 6.187-35.541 3.072-21.504 7.381-28.502 34.688-56.448l31.147-31.872.085 19.797c.085 25.088 11.819 70.4 24.747 95.659l9.941 19.456h-54.699c-54.272 0-54.698-.086-52.053-11.051zm585.643-14.72c10.624-23.979 20.736-63.19 23.723-91.99 1.237-11.69 2.602-11.007 29.866 14.849 27.307 25.813 28.928 28.757 35.584 63.744 3.84 20.181 7.04 37.248 7.04 37.973 0 .64-24.192 1.195-53.802 1.195h-53.803l11.392-25.771z"/></symbol>'
        body.insertBefore(svgDom, body.firstChild)
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadSvg)
    } else {
        loadSvg()
    }
}

export default function (opt) {
    const i18n = opt.i18n;
    i18n.add('zh-cn', zhCN);
    i18n.add('en', en);

    return {
        resource: [{
            type: 'rocketmq-subscribe',
            icon: '#magic-rocketmq-rocketmq',
            title: 'rocketmq.subscribe.name',
            service: MagicRocketMqSubscribe(opt.bus, opt.constants, i18n.format, opt.Message, opt.request)
        }],
        datasources: [{
            type: 'rocketmq-datasource',
            icon: '#magic-rocketmq-rocketmq',
            title: i18n.format('rocketmq.datasource.title'),
            name: i18n.format('rocketmq.datasource.name'),
            service: MagicRocketMqDataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicRocketMqDataSourceInfo
        }],
        toolbars: [{
            type: 'rocketmq-subscribe',
            title: 'rocketmq.subscribe.title',
            icon: '#magic-rocketmq-rocketmq',
            component: MagicRocketMqSubscribeInfo
        }]
    };
}
