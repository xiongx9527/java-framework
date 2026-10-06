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
            auth: null,
            tools: null
        }, item),
        /**
         * 当前 classpath 可用的认证 provider 清单（后端与 Starter 共用同一份）。
         * 拉取失败时返回 null，调用方回退到全量静态清单。
         */
        authProviders: () => new Promise(resolve => {
            request.sendGet('/mcp/server/authProviders')
                .success(res => resolve(Array.isArray(res) ? res : null))
                .exception(() => resolve(null))
                .error(() => resolve(null))
        }),
        runnable: false
    }
}
