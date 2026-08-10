import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicSpringBean from './service/magic-spring-bean.js'
import registerJavaLanguage from './language/java.js'
import installSpringBeanIcon from './icons/java.js'
import MagicSpringBeanInfo from './components/magic-spring-bean-info.vue'

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    registerJavaLanguage(opt.monaco)
    installSpringBeanIcon()

    return {
        name: 'spring-bean',
        statusBarItems: [{
            icon: '#magic-spring-bean-java',
            title: 'Spring Bean',
            displayKey: 'spring-bean',
            order: 520,
            onClick: () => window.open('https://docs.spring.io/spring-framework/docs/5.3.x/reference/html/core.html#beans', '_blank')
        }],
        resource: [{
            type: 'spring-bean',
            icon: '#magic-spring-bean-java',
            title: 'springBean.name',
            service: MagicSpringBean(i18n.format)
        }],
        toolbars: [{
            type: 'spring-bean',
            title: 'springBean.title',
            icon: '#magic-spring-bean-java',
            order: 530,
            component: MagicSpringBeanInfo
        }]
    }
}
