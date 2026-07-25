import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicS3DataSource from './service/magic-s3-datasource.js'
import MagicS3DatasourceInfo from './components/magic-s3-datasource-info.vue'
import MagicS3Browser from './components/magic-s3-browser.vue'
import { setS3BrowserModal, setS3BrowserRequest } from './service/magic-s3-browser.js'

// 手动注入 SVG icon（vite-plugin-svg-icons 在 IIFE 库模式下无法工作）
if (typeof window !== 'undefined') {
    const svgBody = '<symbol viewBox="0 0 183 135" id="magic-s3-s3"><path d="M179.102 19.3359L135.547 66.4062C140.234 65.3646 144.596 64.8438 148.633 64.8438C157.487 64.8438 164.974 67.1875 171.094 71.875C178.906 77.9948 182.812 86.3281 182.812 96.875C182.812 104.688 180.339 111.914 175.391 118.555C167.057 129.362 154.818 134.766 138.672 134.766C130.599 134.766 123.438 133.398 117.188 130.664C106.901 126.107 101.758 119.271 101.758 110.156C101.758 109.115 101.888 108.073 102.148 107.031C103.32 102.344 106.38 100 111.328 100C113.932 100 116.081 100.586 117.773 101.758C119.987 103.451 121.419 107.487 122.07 113.867C122.461 116.862 122.786 119.01 123.047 120.312C124.609 126.693 128.971 129.883 136.133 129.883C154.883 129.753 164.388 119.661 164.648 99.6094C164.648 94.0104 163.737 88.9974 161.914 84.5703C158.008 75.4557 151.497 70.8984 142.383 70.8984C137.956 70.8984 133.398 71.875 128.711 73.8281L124.219 70.1172L159.18 29.6875H128.32C125.456 29.6875 123.307 29.7526 121.875 29.8828C117.448 30.5339 114.518 33.138 113.086 37.6953L109.57 48.2422H104.883L105.273 15.2344H179.102V19.3359ZM41.9922 0C53.5807 0 64.1927 2.53906 73.8281 7.61719C73.8281 6.3151 73.9583 5.20833 74.2188 4.29688C74.6094 2.08333 76.4974 0.976562 79.8828 0.976562V35.3516H73.6328C73.763 27.9297 71.875 21.6797 67.9688 16.6016C62.3698 9.17969 54.1667 5.46875 43.3594 5.46875C39.1927 5.46875 35.3516 6.1849 31.8359 7.61719C23.763 10.8724 19.7266 16.6667 19.7266 25C19.7266 28.125 20.4427 31.1198 21.875 33.9844C24.8698 39.3229 32.0964 44.5312 43.5547 49.6094C56.3151 55.3385 63.737 58.8542 65.8203 60.1562C78.3203 67.3177 84.5703 76.6276 84.5703 88.0859C84.5703 96.1589 81.6406 103.125 75.7812 108.984C67.4479 117.057 55.7292 121.094 40.625 121.094C27.6042 121.094 16.0807 117.773 6.05469 111.133C6.05469 113.737 5.92448 115.625 5.66406 116.797C5.01302 118.62 3.125 119.531 0 119.531V78.9062H6.25C6.38021 80.2083 6.51042 81.5755 6.64062 83.0078C9.63542 104.753 20.5729 115.625 39.4531 115.625C44.2708 115.625 48.6979 114.779 52.7344 113.086C61.3281 109.18 65.625 102.669 65.625 93.5547C65.625 90.1693 64.8438 87.0443 63.2812 84.1797C60.1562 78.7109 52.7995 73.3724 41.2109 68.1641C28.5807 62.4349 21.1589 58.9193 18.9453 57.6172C6.83594 50.7161 0.78125 41.7969 0.78125 30.8594C0.78125 23.4375 3.45052 17.0573 8.78906 11.7188C16.7318 3.90625 27.7995 0 41.9922 0Z"/></symbol>'
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
    setS3BrowserRequest(opt.request)
    setS3BrowserModal(opt.modal)
    return {
        name: 's3',
        datasources: [{
            type: 's3-datasource',
            icon: '#magic-s3-s3',
            title: i18n.format('s3.datasource.title'),
            name: i18n.format('s3.datasource.name'),
            service: MagicS3DataSource(opt.request, i18n.format, opt.modal, opt.JavaClass),
            component: MagicS3DatasourceInfo
        }],
        toolbars: [{
            id: 's3-browser',
            title: i18n.format('s3.browser.title'),
            icon: '#magic-s3-s3',
            component: MagicS3Browser,
            adminOnly: true,
            allowClose: true,
            fullscreen: true
        }]
    }
}
