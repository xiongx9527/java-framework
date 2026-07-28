import MagicSatoken from './service/magic-satoken.js'
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicSatokenInfo from './components/magic-satoken-info.vue'
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
            icon: '#magic-satoken-satoken',
            title: 'Satoken',
            displayKey: 'satoken',
            order: 1400,
            onClick: () => window.open('https://sa-token.cc/', '_blank')
        }],
        // 左侧资源
        resource: [{
            // 资源类型，和后端存储结构一致
            type: 'satoken',
            // 展示图标
            icon: '#magic-satoken-satoken',   // #开头表示图标在插件中
            // 展示名称
            title: 'satoken.name',
            // 运行服务
            service: MagicSatoken(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
        }],
        // 底部工具条
        toolbars: [{
            // 当打开的资源类型为 freemarker 时显示
            type: 'satoken',
            // 工具条展示的标题
            title: 'satoken.title',
            // 展示图标
            icon: 'satoken',
            // 对应的组件
            order: 1100,
            component: MagicSatokenInfo,
        }]
    }
}
