export default function (bus, constants, $i, Message, request) {
    return {
        getIcon: item => ['PRM', '#d29922'],
        name: $i('mcp.prompt.name'),
        language: 'json',
        defaultScript: JSON.stringify({
            arguments: [{
                name: 'content',
                description: 'Prompt input content',
                required: true
            }],
            messages: [{
                role: 'user',
                content: {
                    type: 'text',
                    text: '{{content}}'
                }
            }]
        }, null, 2),
        runnable: true,
        execute: item => request.post('/mcp/diagnostics/execute', {
            jsonrpc: '2.0',
            id: Date.now(),
            method: 'prompts/get',
            params: {
                name: item.name,
                arguments: item.previewArguments || {}
            }
        }),
        requirePath: true,
        merge: item => Object.assign({
            name: '',
            description: '',
            arguments: [],
            messages: []
        }, item)
    }
}
