export default function (bus, constants, $i, Message, request) {
    return {
        getIcon: item => ['RES', '#3fb950'],
        name: $i('mcp.resource.name'),
        language: 'json',
        defaultScript: `{\n  "text": "Hello MCP resource"\n}`,
        runnable: false,
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
