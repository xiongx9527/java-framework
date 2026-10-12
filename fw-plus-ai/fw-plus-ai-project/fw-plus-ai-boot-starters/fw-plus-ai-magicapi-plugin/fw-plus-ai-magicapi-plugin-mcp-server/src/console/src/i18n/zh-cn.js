export default {
    mcp: {
        server: {
            title: 'MCP 服务实例',
            name: 'MCP 服务实例',
            form: {
                key: 'Key',
                keyHint: '实例唯一标识，如 report',
                name: '名称',
                nameHint: '展示名称，留空取 Key',
                transport: '传输协议',
                endpoint: '服务端点',
                endpointHint: '如 /mcp/report',
                enabled: '是否启用',
                auth: '认证方式',
                authNone: '不配置（拒绝访问）',
                functions: '函数工具（functions）',
                api: 'API 工具（api）'
            },
            scope: {
                enabled: '启用',
                includeGroups: '包含分组',
                requireOptIn: '需显式声明（opt-in）',
                optInOptionName: 'opt-in 标识',
                optInOptionNameHint: '如 mcpTool'
            },
            hint: {
                groupSelect: '勾选分组，留空不过滤',
                groupEmpty: '暂无可用分组'
            }
        }
    },
    langchain4j: {
        option: {
            yes: '是',
            no: '否'
        },
        transport: {
            'http-sse': 'HTTP+SSE（旧版）',
            'streamable-http': 'Streamable HTTP'
        }
    }
}
