/**
 * MCP server 数据源服务：参照 langchain4j 各类型极简 datasource service。
 * doTest 调 POST /mcp/datasource/test（返回 "ok" 或错误描述，sendJson 自动解包 JsonBean.data）。
 */
export default (request, $i, modal) => {
    return {
        dialogWidth: '1100px',
        doTest: (info) => {
            request.sendJson('/mcp/datasource/test', info)
                .success((res) => {
                    if (res === 'ok') {
                        modal.alert($i('datasource.connected'), $i('datasource.test'));
                    } else {
                        modal.alert($i('datasource.connectFailed', res), $i('datasource.test'));
                    }
                });
        },
        /**
         * 拉取当前 classpath 可用的认证 provider 清单（后端 List<String>，code=1 解包后
         * 直接是数组）。失败时 resolve(null)，组件按 Array.isArray 判定回退内置全量清单
         * ——组件 .then 无 catch，reject 会产生 unhandled rejection。
         */
        authProviders: () => new Promise((resolve) => {
            request.sendGet('/mcp/server/authProviders')
                .success((list) => resolve(list))
                .exception(() => resolve(null))
                .error(() => resolve(null));
        })
    };
};