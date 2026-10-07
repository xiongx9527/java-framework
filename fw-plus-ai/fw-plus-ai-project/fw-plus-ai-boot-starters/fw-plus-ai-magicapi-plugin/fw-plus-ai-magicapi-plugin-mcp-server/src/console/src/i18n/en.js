export default {
    mcp: {
        server: {
            title: 'MCP Server Instance',
            name: 'MCP Server Instance',
            form: {
                key: 'Key',
                keyHint: 'Unique key, e.g. report',
                name: 'Name',
                nameHint: 'Display name, defaults to key',
                transport: 'Transport',
                endpoint: 'Endpoint',
                endpointHint: 'e.g. /mcp/report',
                enabled: 'Enabled',
                auth: 'Auth Provider',
                authNone: 'Not configured (reject requests)',
                functions: 'Function tools (functions)',
                api: 'API tools (api)'
            },
            scope: {
                enabled: 'Enabled',
                includeGroups: 'Include groups',
                includePaths: 'Include paths',
                excludePaths: 'Exclude paths',
                methods: 'Methods',
                requireOptIn: 'Require opt-in',
                optInOptionName: 'Opt-in flag',
                optInOptionNameHint: 'e.g. mcpTool'
            },
            hint: {
                list: 'Comma separated, e.g. /a/**, /b/**',
                methods: 'Comma separated, e.g. GET, POST'
            }
        }
    },
    langchain4j: {
        option: {
            yes: 'Yes',
            no: 'No'
        },
        transport: {
            'http-sse': 'HTTP+SSE (legacy)',
            'streamable-http': 'Streamable HTTP'
        }
    }
}
