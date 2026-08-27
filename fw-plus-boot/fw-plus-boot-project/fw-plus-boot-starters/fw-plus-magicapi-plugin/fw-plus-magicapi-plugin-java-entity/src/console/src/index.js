import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicJavaEntity from './service/magic-java-entity.js'
import registerJavaLanguage from './language/java.js'
import installJavaEntityIcon from './icons/java.js'
import MagicJavaEntityInfo from './components/magic-java-entity-info.vue'

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    registerJavaLanguage(opt.monaco)
    installJavaEntityIcon()

    return {
        name: 'java-entity',
        statusBarItems: [{
            icon: '#magic-java-entity-java',
            title: 'Java Entity',
            displayKey: 'java-entity',
            order: 510,
            onClick: () => window.open('https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javac.html', '_blank')
        }],
        resource: [{
            type: 'java-entity',
            icon: '#magic-java-entity-java',
            title: 'javaEntity.name',
            service: MagicJavaEntity(i18n.format)
        }],
        toolbars: [{
            type: 'java-entity',
            title: 'javaEntity.title',
            icon: '#magic-java-entity-java',
            order: 520,
            component: MagicJavaEntityInfo
        }]
    }
}
