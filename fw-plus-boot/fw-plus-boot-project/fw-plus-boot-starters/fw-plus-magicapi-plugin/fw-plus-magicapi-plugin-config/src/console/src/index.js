import MagicTask from './service/magic-config.js'
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicConfigInfo from './components/magic-config-info.vue'
import setYaml from './js/yaml'
import 'vite-plugin-svg-icons/register'
export default (opt) => {
    const i18n = opt.i18n
    // 添加i18n 国际化信息
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    setYaml(opt.monaco)
    return {
        // 左侧资源
        resource: [{
            // 资源类型，和后端存储结构一致
            type: 'config',
            // 展示图标
            icon: '#magic-config-config',   // #开头表示图标在插件中
            // 展示名称
            title: 'config.name',
            // 运行服务
            service: MagicTask(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
        }],
        // 底部工具条
        toolbars: [{
            // 当打开的资源类型为 config 时显示
            type: 'config',
            // 工具条展示的标题
            title: 'config.title',
            // 展示图标
            icon: 'parameter',
            // 对应的组件
            component: MagicConfigInfo,
        }]
    }
}
