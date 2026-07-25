import MagicFreemarker from './service/magic-freemarker.js'
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicFreemarkerInfo from './components/magic-freemarker-info.vue'
import MagicFreemarkerResult from './components/magic-freemarker-result.vue'
import 'vite-plugin-svg-icons/register'

import setCss from './js/css/css.contribution'
import setJs from './js/javascript/javascript.contribution'
import setHtml from './js/html/html.contribution'

export default (opt) => {
    const i18n = opt.i18n
    // 添加i18n 国际化信息
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    setCss(opt.monaco)
    setJs(opt.monaco)
    setHtml(opt.monaco)
    return {
        statusBarItems: [{
            icon: '#magic-freemarker-ftl',
            title: 'Freemarker',
            displayKey: 'freemarker',
            order: 500,
            onClick: () => window.open('https://freemarker.apache.org/', '_blank')
        }],
        // 左侧资源
        resource: [{
            // 资源类型，和后端存储结构一致
            type: 'freemarker',
            // 展示图标
            icon: '#magic-freemarker-ftl',   // #开头表示图标在插件中
            // 展示名称
            title: 'freemarker.name',
            // 运行服务
            service: MagicFreemarker(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
        }],
        // 底部工具条
        toolbars: [{
            id: 'freemarker-result',
            type: 'freemarker',
            title: 'freemarker.result',
            icon: 'run',
            component: MagicFreemarkerResult,
            fullscreen: true
        }, {
            // 当打开的资源类型为 freemarker 时显示
            type: 'freemarker',
            // 工具条展示的标题
            title: 'freemarker.title',
            // 展示图标
            icon: '#magic-freemarker-ftl',
            // 对应的组件
            component: MagicFreemarkerInfo
        }]
    }
}
