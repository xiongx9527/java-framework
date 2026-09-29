/**
 * langchain4j-system-message 资源服务：路径型 .ms 资源，script 为 magic-script（返回系统提示词文本）。
 */
export default (bus, constants, $i, Message, request) => {
    return {
        getIcon: (item) => ['SYS', '#409EFF'],
        name: $i('langchain4j.type.system-message'),
        language: 'magicscript',
        defaultScript: `return "你是一个乐于助人的助手。"`,
        doTest: (opened) => {
            opened.running = true
            const info = opened.item
            const requestConfig = {
                baseURL: constants.SERVER_URL,
                url: '/langchain4j/system-message/execute',
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
            bus.status(`开始测试系统提示词「${fullName}」`)
            request.sendPost('/langchain4j/system-message/execute', { id: info.id }, requestConfig)
                .success((res) => { opened.running = false })
                .end(() => {
                    bus.status(`系统提示词「${fullName}」测试完毕`)
                    opened.running = false
                })
        },
        runnable: true,
        // 路径型资源，需填写路径
        requirePath: true,
        merge: (item) => item
    }
}
