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
                requireOptIn: 'Require opt-in',
                optInOptionName: 'Opt-in flag',
                optInOptionNameHint: 'e.g. mcpTool'
            },
            hint: {
                groupSelect: 'Check groups, empty means no filter',
                groupEmpty: 'No groups available'
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
