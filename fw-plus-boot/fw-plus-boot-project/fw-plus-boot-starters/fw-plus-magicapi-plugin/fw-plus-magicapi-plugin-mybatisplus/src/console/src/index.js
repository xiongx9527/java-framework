import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicMybatisPlusXml from './service/magic-mybatisplus-xml.js'
import installMybatisPlusXmlIcon from './icons/xml.js'
import MagicMybatisPlusXmlInfo from './components/magic-mybatisplus-xml-info.vue'

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    installMybatisPlusXmlIcon()

    return {
        name: 'mybatisplus-xml',
        resource: [{
            type: 'mybatisplus-xml',
            icon: '#magic-mybatisplus-xml',
            title: 'mybatisPlusXml.name',
            service: MagicMybatisPlusXml(i18n.format)
        }],
        toolbars: [{
            type: 'mybatisplus-xml',
            title: 'mybatisPlusXml.title',
            icon: '#magic-mybatisplus-xml',
            order: 521,
            component: MagicMybatisPlusXmlInfo
        }]
    }
}
