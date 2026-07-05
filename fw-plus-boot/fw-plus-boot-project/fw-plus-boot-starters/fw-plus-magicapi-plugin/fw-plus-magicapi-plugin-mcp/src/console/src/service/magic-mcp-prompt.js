export default function (bus, constants, $i, Message, request) {
    return {
        getIcon: item => ['PRM', '#d29922'],
        name: $i('mcp.prompt.name'),
        language: 'json',
        defaultScript: `{\n  "messages": [\n    {\n      "role": "user",\n      "content": "Describe the current context."\n    }\n  ]\n}`,
        runnable: false,
        requirePath: true,
        merge: item => Object.assign({
            name: '',
            description: '',
            arguments: [],
            messages: []
        }, item)
    }
}
