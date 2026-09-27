export default function (bus, constants, $i, Message, request) {
    return {
        getIcon: item => ['SRV', '#3fb950'],
        name: $i('mcp.server.name'),
        language: 'json',
        defaultScript: `{\n  "key": "report",\n  "endpoint": "/mcp/report",\n  "transport": "http-sse"\n}`,
        requirePath: true,
        merge: item => Object.assign({
            key: '',
            name: '',
            transport: 'http-sse',
            endpoint: '',
            enabled: true,
            tools: null
        }, item),
        runnable: false
    }
}
