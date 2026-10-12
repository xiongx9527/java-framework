import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicRedisDataSource from './service/magic-redis-datasource.js'
import MagicRedisDatasourceInfo from './components/magic-redis-datasource-info.vue'

// Manually inject SVG icon (vite-plugin-svg-icons doesn't work in library/IIFE mode)
if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 85 85" id="magic-redis-redis"><path d="M38.5 0C59.752 5.16002e-07 77 17.248 77 38.5C77 59.752 59.7828 77 38.5 77C34.188 77 30.0297 76.2912 26.1797 74.9668H26.1182C24.0547 74.2584 22.0837 73.3962 20.2051 72.3799C20.0819 72.3183 19.9274 72.2262 19.7734 72.1338C14.9071 69.4234 10.6876 65.6964 7.42285 61.2305C5.5441 58.6433 3.9424 55.8406 2.74121 52.8223C0.985611 48.3871 1.24057e-07 43.5512 0 38.5C0 17.248 17.248 0 38.5 0ZM42.5059 18.4805C35.2685 18.4805 26.3257 21.0761 20.4453 25.1416C20.3757 29.3436 22.8815 34.8101 23.7861 34.1953C28.8661 30.7791 32.9022 28.5582 36.834 27.4307C31.0233 33.5115 17.1068 47.6202 15.4004 50.1152C15.5744 52.4043 18.601 58.5203 20.0625 58.5205C20.5149 58.5205 20.8976 58.2809 21.3496 57.8711C25.5251 53.4298 28.9701 49.4668 31.9971 45.6406C32.4496 51.2434 35.3727 58.1102 43.584 58.1104C50.9258 58.1104 58.2326 53.1217 61.5381 41.8818C61.9555 40.5154 60.147 39.4226 59.1377 40.584C55.9367 44.3422 52.4918 48.6807 45.5674 48.6807C39.4088 48.6806 37.1126 43.5564 36.9385 39.4229C38.2956 42.0875 40.9402 44.2735 45.0459 44.1709C52.9787 43.9318 58.407 37.2021 58.4072 31.0869C58.4072 23.776 52.5964 18.4806 42.5059 18.4805ZM45.5322 26.7822C49.3247 27.3631 50.7518 29.3439 50.752 31.4961C50.752 35.049 47.0292 36.7918 43.6543 36.792C41.8448 36.792 40.4526 36.3475 39.3389 35.7666C41.3919 32.8971 43.3751 29.9252 45.5322 26.7822Z"/></symbol>'
    const svgDom = document.getElementById('__svg__icons__dom__')
    if (svgDom) {
        svgDom.innerHTML += svgBody
    } else {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
        svg.style.position = 'absolute'
        svg.style.width = '0'
        svg.style.height = '0'
        svg.id = '__svg__icons__dom__'
        svg.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
        svg.setAttribute('xmlns:link', 'http://www.w3.org/1999/xlink')
        svg.innerHTML = svgBody
        document.body.insertBefore(svg, document.body.firstChild)
    }
}

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        datasources: [{
            type: 'redis-datasource',
            icon: '#magic-redis-redis',
            title: i18n.format('redis.datasource.common.title'),
            name: i18n.format('redis.datasource.common.name'),
            service: MagicRedisDataSource(opt.request, i18n.format, opt.modal),
            component: MagicRedisDatasourceInfo
        }]
    }
}