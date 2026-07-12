export default function (bus, constants, $i, Message, request) {
    return {
        getIcon: item => ['RES', '#3fb950'],
        name: $i('mcp.resource.name'),
        language: 'json',
        defaultScript: `{\n  "text": "Hello MCP resource"\n}`,
        runnable: true,
        execute: item => request.post('/mcp/diagnostics/execute', {
            jsonrpc: '2.0',
            id: Date.now(),
            method: 'resources/read',
            params: { uri: item.uri }
        }),
        requirePath: true,
        merge: item => Object.assign({
            uri: '',
            name: '',
            mimeType: 'application/json',
            description: '',
            template: false
        }, item)
    }
}
