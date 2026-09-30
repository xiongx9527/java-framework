export default {
    langchain4j: {
        type: {
            'aj-mcp-client': 'aj-mcp 客户端'
        },
        common: {
            name: '名称',
            key: 'Key',
            enabled: '启用'
        },
        transport: {
            label: '传输类型',
            'http-sse': 'HTTP+SSE（旧版）',
            'streamable-http': 'Streamable HTTP',
            stdio: 'stdio（本地子进程）'
        },
        url: '服务地址',
        'url-hint': 'http-sse 填 SSE 端点；streamable-http 填消息端点',
        command: '启动命令',
        'command-hint': 'stdio 传输用，如 java,-jar,server.jar（英文逗号分隔）',
        environment: '环境变量',
        'environment-hint': 'JSON 对象，如 {"API_KEY":"xxx"}',
        headers: '请求头',
        'headers-hint': 'JSON 对象，认证用，如 {"Authorization":"Bearer xxx"}',
        'filter-tool-names': '工具过滤',
        'filter-tool-names-hint': '仅暴露这些工具，英文逗号分隔，留空全部',
        'auth-retry-count': '认证重试次数',
        'auth-retry-count-hint': 'api-key 过期等认证失败时自动重试，0 不重试',
        hint: {
            required: '必填',
            optional: '选填'
        },
        option: {
            yes: '是',
            no: '否'
        }
    }
}
