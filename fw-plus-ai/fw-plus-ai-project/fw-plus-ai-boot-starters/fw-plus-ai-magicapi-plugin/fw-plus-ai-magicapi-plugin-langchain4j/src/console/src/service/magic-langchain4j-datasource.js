/**
 * langchain4j datasource 服务：连接测试按资源类型路由到
 * POST /langchain4j/{short}/datasource/test，协议与官方 datasource test 一致（返回 ok 或中文错误）。
 *
 * @param request magic-api console 的请求对象
 * @param $i      i18n.format
 * @param modal   magic-api console 的弹窗对象
 * @param short   短类型（如 chat-model）
 */
export default (request, $i, modal, short) => {
    return {
        dialogWidth: '1100px',
        doTest: (info) => {
            request.sendJson('/langchain4j/' + short + '/datasource/test', info)
                .success((res) => {
                    if (res === 'ok') {
                        modal.alert($i('datasource.connected'), $i('datasource.test'));
                    } else {
                        modal.alert($i('datasource.connectFailed', res), $i('datasource.test'));
                    }
                });
        }
    };
};
