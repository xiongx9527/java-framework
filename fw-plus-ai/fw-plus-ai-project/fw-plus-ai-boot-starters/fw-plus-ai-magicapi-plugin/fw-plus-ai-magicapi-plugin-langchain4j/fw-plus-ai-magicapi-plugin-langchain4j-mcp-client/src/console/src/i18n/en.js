export default {
    langchain4j: {
        type: {
            'mcp-client': 'MCP Client'
        },
        common: {
            name: 'Name',
            key: 'Key',
            enabled: 'Enabled'
        },
        transport: {
            label: 'Transport',
            http: 'Streamable HTTP',
            stdio: 'stdio (local subprocess)',
            docker: 'docker (container subprocess)'
        },
        url: 'Server URL',
        'url-hint': 'Streamable HTTP message endpoint, e.g. http://host:3000/mcp',
        command: 'Command',
        'command-hint': 'e.g. npx,-y,mcp-server-xx (comma separated)',
        environment: 'Environment',
        'environment-hint': 'JSON object, e.g. {"API_KEY":"xxx"}',
        image: 'Image',
        headers: 'Headers',
        'headers-hint': 'JSON object for auth, e.g. {"Authorization":"Bearer xxx"}',
        'headers-function': 'Dynamic headers function',
        'headers-function-hint': 'magic-api function name, invoked per request for fresh headers (e.g. new api-key)',
        'headers-cache-seconds': 'Headers cache (seconds)',
        'headers-cache-hint': 'Set when the token API must not be called too often, e.g. 300',
        'filter-tool-names': 'Tool filter',
        'filter-tool-names-hint': 'Only expose these tools, comma separated; empty for all',
        'auth-retry-count': 'Auth retries',
        'auth-retry-count-hint': 'Auto retry on auth failures (expired api-key); 0 to disable',
        'fail-if-one-server-fails': 'Fail if one server fails',
        'listener-function': 'Listener function',
        'listener-function-hint': 'Invoked on server messages/lifecycle events with {key, event, detail}',
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
