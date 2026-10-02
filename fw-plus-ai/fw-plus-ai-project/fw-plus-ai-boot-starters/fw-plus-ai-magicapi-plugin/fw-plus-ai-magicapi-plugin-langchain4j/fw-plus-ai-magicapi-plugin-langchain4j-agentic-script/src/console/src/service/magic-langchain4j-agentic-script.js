/**
 * langchain4j-agentic-script 资源服务：路径型 .ms 资源，script 为 magic-script
 * （输入变量注入上下文，返回值即节点输出/工具返回值）。
 */
export default (bus, constants, $i, Message, request) => {
    return {
        getIcon: (item) => ['AS', '#67C23A'],
        name: $i('langchain4j.type.agentic-script'),
        language: 'magicscript',
        defaultScript: `return "脚本节点输出"`,
        doTest: (opened) => {
            opened.running = true
            const info = opened.item
            const requestConfig = {
                baseURL: constants.SERVER_URL,
                url: '/langchain4j/agentic-script/execute',
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
            bus.status(`开始测试 agentic 脚本「${fullName}」`)
            request.sendPost('/langchain4j/agentic-script/execute', { id: info.id }, requestConfig)
                .success((res) => { opened.running = false })
                .end(() => {
                    bus.status(`agentic 脚本「${fullName}」测试完毕`)
                    opened.running = false
                })
        },
        runnable: true,
        // 路径型资源，需填写路径
        requirePath: true,
        merge: (item) => item
    }
}
