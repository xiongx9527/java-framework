const DEFAULT_PATH = '/tiny-engine/'

const normalizePath = path => {
    if (!path) {
        return DEFAULT_PATH
    }
    const prefixed = path.startsWith('/') ? path : `/${path}`
    return prefixed.endsWith('/') ? prefixed : `${prefixed}/`
}

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
    name: 'tiny-engine',
    statusBarItems: [{
        icon: 'app',
        title: 'TinyEngine',
        displayKey: 'tiny-engine',
        order: 1600,
        onClick: () => {
            const link = resolveBase()
            const path = normalizePath(pluginData.attributes?.path)
            window.open(replaceURL(link + path), '_blank')
        }
    }]
})
