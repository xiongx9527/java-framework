import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicMybatisFlexXml from './service/magic-mybatisflex-xml.js'
import installMybatisFlexXmlIcon from './icons/xml.js'
import MagicMybatisFlexXmlInfo from './components/magic-mybatisflex-xml-info.vue'

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    installMybatisFlexXmlIcon()

    return {
        name: 'mybatisflex-xml',
        resource: [{
            type: 'mybatisflex-xml',
            icon: '#magic-mybatisflex-xml',
            title: 'mybatisFlexXml.name',
            service: MagicMybatisFlexXml(i18n.format)
        }],
        toolbars: [{
            type: 'mybatisflex-xml',
            title: 'mybatisFlexXml.title',
            icon: '#magic-mybatisflex-xml',
            order: 522,
            component: MagicMybatisFlexXmlInfo
        }]
    }
}
