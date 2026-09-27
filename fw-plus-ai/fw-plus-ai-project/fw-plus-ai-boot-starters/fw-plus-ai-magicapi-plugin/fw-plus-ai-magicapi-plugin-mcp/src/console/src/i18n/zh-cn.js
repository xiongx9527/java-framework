export default {
    mcp: {
        resource: {
            title: 'MCP 资源',
            name: 'MCP 资源',
            form: {
                uri: 'URI',
                name: '名称',
                mimeType: 'MIME',
                description: '描述',
                template: '模板',
                placeholder: {
                    uri: '例如 mcp://docs/readme',
                    name: '请输入资源名称',
                    mimeType: '例如 text/plain 或 application/json',
                    description: '请输入资源描述'
                }
            }
        },
        prompt: {
            title: 'MCP 提示词',
            name: 'MCP 提示词',
            form: {
                name: '名称',
                description: '描述',
                arguments: '参数',
                messages: '消息',
                placeholder: {
                    name: '请输入提示词名称',
                    description: '请输入提示词描述',
                    arguments: 'JSON 参数数组，例如 [{"name":"content","description":"内容","required":true}]',
                    messages: 'JSON 消息数组，例如 [{"role":"user","content":{"type":"text","text":"{{content}}"}}]'
                }
            }
        },
        server: {
            title: 'MCP 服务实例',
            name: 'MCP 服务实例'
        }
    }
}
