import { installIcon } from './assets/icon.js'

installIcon()

const resolveBase = () => {
    let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
    const baseURL = import.meta.env?.MAGIC_API_BASE_URL || './'
    if (baseURL.startsWith('http')) {
        link = new URL(baseURL).origin
    } else if (baseURL.startsWith('./')) {
        link = `${window.location.protocol}//${window.location.host}`
    }
    return link
}

export default () => ({
    name: 'druid',
    statusBarItems: [{
        icon: '#magic-druid-druid',
        title: 'Druid DataSource',
        displayKey: 'druid',
        order: 1100,
        onClick: () => window.open(resolveBase() + '/druid/login.html', '_blank')
    }]
})
