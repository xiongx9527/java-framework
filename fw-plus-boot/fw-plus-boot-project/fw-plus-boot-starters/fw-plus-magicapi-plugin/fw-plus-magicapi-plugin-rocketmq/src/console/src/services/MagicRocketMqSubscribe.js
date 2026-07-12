export default function MagicRocketMqSubscribe(bus, constants, $i, Message, request) {
    return {
        getIcon: (item) => ['ROMQ', '#1296db'],
        name: $i('rocketmq.name'),
        language: 'magicscript',
        defaultScript: `return null; // 正常处理时返回null`,
        doTest: (opened) => {
            opened.running = true;
            const info = opened.item;
            const requestConfig = {
                baseURL: constants.SERVER_URL,
                url: '/rocketmq/subscribe/execute',
                method: 'POST',
                responseType: 'json',
                headers: {},
                withCredentials: true
            };
            bus.$emit(Message.SWITCH_TOOLBAR, 'log');
            requestConfig.headers[constants.HEADER_REQUEST_CLIENT_ID] = constants.CLIENT_ID;
            requestConfig.headers[constants.HEADER_REQUEST_SCRIPT_ID] = opened.item.id;
            requestConfig.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE;
            requestConfig.headers[constants.HEADER_REQUEST_BREAKPOINTS] = (opened.decorations || [])
                .filter((it) => it.options.linesDecorationsClassName === 'breakpoints')
                .map((it) => it.range.startLineNumber)
                .join(',');
            const fullName = opened.path();
            bus.status(`开始测试 RocketMq 订阅【${fullName}】`);
            request.sendPost('/rocketmq/subscribe/execute', { id: info.id }, requestConfig)
                .success((res) => {
                    opened.running = false;
                })
                .end(() => {
                    bus.status(`RocketMq 订阅【${fullName}】测试完毕`);
                    opened.running = false;
                });
        },
        runnable: false,
        requirePath: true,
        merge: (item) => item
    };
}
