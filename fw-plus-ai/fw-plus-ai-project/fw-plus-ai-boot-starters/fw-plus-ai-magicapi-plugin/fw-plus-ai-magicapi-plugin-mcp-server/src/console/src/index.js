import MagicMcpResource from './service/magic-mcp-resource.js'
import MagicMcpPrompt from './service/magic-mcp-prompt.js'
import MagicMcpServer from './service/magic-mcp-server.js'
import localZhCN from './i18n/zh-cn.js'
import localEn from './i18n/en.js'
import MagicMcpResourceInfo from './components/magic-mcp-resource-info.vue'
import MagicMcpPromptInfo from './components/magic-mcp-prompt-info.vue'
import 'vite-plugin-svg-icons/register'

export default (opt) => {
    const i18n = opt.i18n
    i18n.add('zh-cn', localZhCN)
    i18n.add('en', localEn)
    return {
        resource: [{
            type: 'mcp-resource',
            icon: '#magic-mcp-resource',
            title: 'mcp.resource.name',
            service: MagicMcpResource(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
        }, {
            type: 'mcp-prompt',
            icon: '#magic-mcp-prompt',
            title: 'mcp.prompt.name',
            service: MagicMcpPrompt(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
        }, {
            type: 'mcp-server',
            icon: '#magic-mcp-server',
            title: 'mcp.server.name',
            service: MagicMcpServer(opt.bus, opt.constants, i18n.format, opt.Message, opt.request),
        }],
        toolbars: [{
            type: 'mcp-resource',
            title: 'mcp.resource.title',
            icon: '#magic-mcp-resource',
            order: 500,
            component: MagicMcpResourceInfo,
        }, {
            type: 'mcp-prompt',
            title: 'mcp.prompt.title',
            icon: '#magic-mcp-prompt',
            order: 510,
            component: MagicMcpPromptInfo,
        }]
    }
}
