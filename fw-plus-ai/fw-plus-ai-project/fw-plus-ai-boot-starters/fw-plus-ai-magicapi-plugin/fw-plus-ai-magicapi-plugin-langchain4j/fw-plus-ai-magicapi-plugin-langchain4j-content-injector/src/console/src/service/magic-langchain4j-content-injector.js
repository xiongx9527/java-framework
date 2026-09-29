/**
 * langchain4j-content-injector 资源服务：路径型 .ms 资源，script 为 magic-script（返回注入后的文本或 ChatMessage）。
 */
export default (bus, constants, $i, Message, request) => {
    return {
        getIcon: (item) => ['INJ', '#67C23A'],
        name: $i('langchain4j.type.content-injector'),
        language: 'magicscript',
        defaultScript: `var context = "";
for (var i = 0; i < texts.size(); i++) {
    context = context + texts[i] + "\n";
}
return query + "\n\n参考资料：\n" + context;`,
        doTest: (opened) => {
            opened.running = true
            const info = opened.item
            const requestConfig = {
                baseURL: constants.SERVER_URL,
                url: '/langchain4j/content-injector/execute',
                method: 'POST',
                responseType: 'json',
                headers: {},
                withCredentials: true
            }
            bus.$emit(Message.SWITCH_TOOLBAR, 'log')
            requestConfig.headers[constants.HEADER_REQUEST_CLIENT_ID] = constants.CLIENT_ID
            requestConfig.headers[constants.HEADER_REQUEST_SCRIPT_ID] = opened.item.id
            requestConfig.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE
            requestConfig.headers[constants.HEADER_REQUEST_BREAKPOINTS] = (opened.decorations || [])
                .filter((it) => it.options.linesDecorationsClassName === 'breakpoints')
                .map((it) => it.range.startLineNumber).join(',')
            const fullName = opened.path()
            bus.status(`开始测试内容注入器「${fullName}」`)
            request.sendPost('/langchain4j/content-injector/execute', { id: info.id }, requestConfig)
                .success((res) => { opened.running = false })
                .end(() => {
                    bus.status(`内容注入器「${fullName}」测试完毕`)
                    opened.running = false
                })
        },
        runnable: true,
        // 路径型资源，需填写路径
        requirePath: true,
        merge: (item) => item
    }
}
