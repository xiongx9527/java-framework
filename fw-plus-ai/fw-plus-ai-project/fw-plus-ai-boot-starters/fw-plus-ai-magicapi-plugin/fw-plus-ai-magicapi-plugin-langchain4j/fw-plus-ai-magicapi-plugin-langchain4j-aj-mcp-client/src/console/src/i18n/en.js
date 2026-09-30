export default {
    langchain4j: {
        type: {
            'aj-mcp-client': 'aj-mcp Client'
        },
        common: {
            name: 'Name',
            key: 'Key',
            enabled: 'Enabled'
        },
        transport: {
            label: 'Transport',
            'http-sse': 'HTTP+SSE (legacy)',
            'streamable-http': 'Streamable HTTP',
            stdio: 'stdio (local subprocess)'
        },
        url: 'Server URL',
        'url-hint': 'SSE endpoint for http-sse; message endpoint for streamable-http',
        command: 'Command',
        'command-hint': 'For stdio, e.g. java,-jar,server.jar (comma separated)',
        environment: 'Environment',
        'environment-hint': 'JSON object, e.g. {"API_KEY":"xxx"}',
        headers: 'Headers',
        'headers-hint': 'JSON object for auth, e.g. {"Authorization":"Bearer xxx"}',
        'filter-tool-names': 'Tool filter',
        'filter-tool-names-hint': 'Only expose these tools, comma separated; empty for all',
        'auth-retry-count': 'Auth retries',
        'auth-retry-count-hint': 'Auto retry on auth failures (expired api-key); 0 to disable',
        hint: {
            required: 'required',
            optional: 'optional'
        },
        option: {
            yes: 'Yes',
            no: 'No'
        }
    }
}
