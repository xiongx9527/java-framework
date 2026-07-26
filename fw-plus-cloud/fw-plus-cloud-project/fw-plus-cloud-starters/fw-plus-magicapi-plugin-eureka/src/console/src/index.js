import { installIcon } from './assets/icon.js'

installIcon()

const replaceURL = url => url.replace(/:?\/+/g, value => value.indexOf(':') > -1 ? value : '/')

const resolveBase = () => {
    let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
    const baseURL = typeof __MAGIC_API_BASE_URL__ === 'undefined' ? './' : __MAGIC_API_BASE_URL__
    if (baseURL.startsWith('http')) {
        link = new URL(baseURL).origin
    } else if (baseURL.startsWith('./')) {
        link = `${window.location.protocol}//${window.location.host}`
    }
    return link
}

export default ({ pluginData = {} } = {}) => ({
    name: 'eureka',
    statusBarItems: [{
        icon: '#magic-eureka-eureka',
        title: 'Eureka Dashboard',
        displayKey: 'eureka',
        order: 1600,
        onClick: () => {
            const link = resolveBase()
            const path = pluginData.attributes?.path || '/'
            window.open(path ? replaceURL(link + '/' + path) : replaceURL(link), '_blank')
        }
    }]
})
