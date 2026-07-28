import 'vite-plugin-svg-icons/register'
import MagicWord from './service/magic-word.js'
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicWordDesigner from './components/magic-word-canvas-designer.vue'
import MagicWordInfo from './components/magic-word-info.vue'
import MagicWordResult from './components/magic-word-result.vue'

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        resource: [{
            type: 'word',
            icon: '#magic-word-word',
            title: 'word.name',
            service: MagicWord(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
        }],
        toolbars: [{
            id: 'word-result',
            type: 'word',
            title: 'word.result',
            icon: 'run',
            order: 1300,
            component: MagicWordResult
        }, {
            id: 'word-designer',
            type: 'word',
            title: 'word.designerTab',
            icon: '#magic-word-word',
            order: 1310,
            component: MagicWordDesigner,
            fullscreen: true
        }, {
            id: 'word-info',
            type: 'word',
            title: 'word.info',
            icon: '#magic-word-word',
            order: 1320,
            component: MagicWordInfo
        }]
    }
}
