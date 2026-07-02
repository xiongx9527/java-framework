import MagicVue3 from './service/magic-vue3.js'
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicVue3Info from './components/magic-vue3-info.vue'
import 'vite-plugin-svg-icons/register'
import setCss from './js/css/css.contribution'
import setJs from './js/javascript/javascript.contribution'
import setHtml from './js/html/html.contribution'
export default (opt) => {
    const i18n = opt.i18n
    // 添加i18n 国际化信息
    i18n.add('zh-cn', localZhCN) ;
    i18n.add('en', localEn) ;
    setCss(opt.monaco) ;
    setJs(opt.monaco) ;
    setHtml(opt.monaco) ;
    return {
        // 左侧资源
        resource: [{
            // 资源类型，和后端存储结构一致
            type: 'vue3',
            // 展示图标
            icon: '#magic-vue3-vue',   // #开头表示图标在插件中
            // 展示名称
            title: 'vue.name',
            // 运行服务
            service: MagicVue3(opt.bus, opt.constants, i18n.format, opt.Message, opt.request, opt),
        }],
        // 底部工具条
        toolbars: [{
            // 当打开的资源类型为 vue3 时显示
            type: 'vue3',
            // 工具条展示的标题
            title: 'vue.title',
            // 展示图标
            icon: 'parameter',
            // 对应的组件
            component: MagicVue3Info,
        }]
    }
}
