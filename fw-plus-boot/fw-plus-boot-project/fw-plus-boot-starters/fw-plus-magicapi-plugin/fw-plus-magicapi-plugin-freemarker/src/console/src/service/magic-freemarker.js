export default function (bus, constants, $i, Message, request) {
    return {
        // svg text
        getIcon: item => ['FTL', '#02FAF1'],
        // 任务名称
        name: $i('freemarker.name'),
        // 脚本语言
        language: 'html',
        // 默认脚本
        defaultScript: `<#--Hello Magic-APi-FreeMarker-->`,
        // 执行测试的逻辑
        doTest: (opened) => {
            opened.running = true
            const info = opened.item
            const requestConfig = {
                url: '/freemarker/execute',
                method: 'POST',
                responseType: 'text',
                headers: {},
                withCredentials: true,
                data: { id: info.id, params: info.params }
            }
            requestConfig.headers[constants.HEADER_REQUEST_CLIENT_ID] = constants.CLIENT_ID
            requestConfig.headers[constants.HEADER_REQUEST_SCRIPT_ID] = opened.item.id
            requestConfig.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE
            // 设置断点
            requestConfig.headers[constants.HEADER_REQUEST_BREAKPOINTS] = (opened.decorations || []).filter(it => it.options.linesDecorationsClassName === 'breakpoints').map(it => it.range.startLineNumber).join(',')
            const fullName = opened.path()
            bus.$emit(Message.SWITCH_TOOLBAR, 'log')
            bus.status(`开始测试freemarker「${fullName}」`)
            request.execute(requestConfig).then(response => {
                opened.running = false
                info.responseBody = response.data
                info.responseStatus = response.status
                info.responseHeaders = response.headers
                bus.status(`freemarker「${fullName}」测试成功`)
                bus.$emit(Message.SWITCH_TOOLBAR, 'freemarker-result')
            }).catch(error => {
                opened.running = false
                bus.status(`freemarker「${fullName}」测试失败`)
                request.processError(error)
            })
        },
        // 是否允许执行测试
        runnable: true,
        // 是否需要填写路径
        requirePath: true,
        // 合并
        merge: item => item
    }
}
