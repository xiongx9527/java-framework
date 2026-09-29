/**
 * langchain4j-embedding-model 的连接测试：协议与官方 datasource test 一致（返回 ok 或中文错误）。
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
