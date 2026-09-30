export default {
    langchain4j: {
        type: {
            'mcp-client': 'MCP 客户端'
        },
        common: {
            name: '名称',
            key: 'Key',
            enabled: '启用'
        },
        transport: {
            label: '传输类型',
            http: 'Streamable HTTP',
            stdio: 'stdio（本地子进程）',
            docker: 'docker（容器子进程）'
        },
        url: '服务地址',
        'url-hint': 'Streamable HTTP 消息端点，如 http://host:3000/mcp',
        command: '启动命令',
        'command-hint': '如 npx,-y,mcp-server-xx（英文逗号分隔）',
        environment: '环境变量',
        'environment-hint': 'JSON 对象，如 {"API_KEY":"xxx"}',
        image: '镜像',
        headers: '请求头',
        'headers-hint': 'JSON 对象，认证用，如 {"Authorization":"Bearer xxx"}',
        'headers-function': '动态请求头函数',
        'headers-function-hint': 'magic-api function 名，每次请求调用取最新头（如换 api-key）',
        'headers-cache-seconds': '动态头缓存秒数',
        'headers-cache-hint': 'token 接口不能频繁调时设置，如 300',
        'filter-tool-names': '工具过滤',
        'filter-tool-names-hint': '仅暴露这些工具，英文逗号分隔，留空全部',
        'auth-retry-count': '认证重试次数',
        'auth-retry-count-hint': 'api-key 过期等认证失败时自动重试，0 不重试',
        'fail-if-one-server-fails': '单路失败即失败',
        'listener-function': '消息监听函数',
        'listener-function-hint': '收到服务端消息/生命周期事件时调用，参数 {key, event, detail}',
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
