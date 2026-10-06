import MagicMcpServer from './service/magic-mcp-server.js'
import MagicMcpServerInfo from './components/magic-mcp-server-info.vue'
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import 'vite-plugin-svg-icons/register'

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        // 核心控制台按 datasources（复数）消费：右栏条目 + magic-datasource-${type} 组件注册
        datasources: [{
            type: 'mcp-server',
            icon: '#magic-mcp-server',
            title: i18n.format('mcp.server.name'),
            name: i18n.format('mcp.server.name'),
            service: MagicMcpServer(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
            component: MagicMcpServerInfo
        }]
    }
}
