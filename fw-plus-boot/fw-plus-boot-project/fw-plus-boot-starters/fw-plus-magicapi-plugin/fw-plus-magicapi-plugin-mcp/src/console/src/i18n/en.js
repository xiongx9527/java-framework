export default {
    mcp: {
        resource: {
            title: 'MCP Resource',
            name: 'MCP Resource',
            form: {
                uri: 'URI',
                name: 'Name',
                mimeType: 'MIME',
                description: 'Description',
                template: 'Template',
                placeholder: {
                    uri: 'For example mcp://docs/readme',
                    name: 'Please enter resource name',
                    mimeType: 'For example text/plain or application/json',
                    description: 'Please enter resource description'
                }
            }
        },
        prompt: {
            title: 'MCP Prompt',
            name: 'MCP Prompt',
            form: {
                name: 'Name',
                description: 'Description',
                arguments: 'Arguments',
                messages: 'Messages',
                placeholder: {
                    name: 'Please enter prompt name',
                    description: 'Please enter prompt description',
                    arguments: 'JSON array, for example [{"name":"topic","required":true}]',
                    messages: 'JSON array, for example [{"role":"user","content":"..."}]'
                }
            }
        }
    }
}
