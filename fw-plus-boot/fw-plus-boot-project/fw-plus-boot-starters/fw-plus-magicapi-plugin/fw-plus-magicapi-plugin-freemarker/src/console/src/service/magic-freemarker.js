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
                 baseURL: constants.SERVER_URL,
                 url: '/freemarker/execute',
                 method: 'POST',
                 responseType: 'json',
                 headers: {},
                 withCredentials: true
             }
             bus.$emit(Message.SWITCH_TOOLBAR, 'log')
             requestConfig.headers[constants.HEADER_REQUEST_CLIENT_ID] = constants.CLIENT_ID
             requestConfig.headers[constants.HEADER_REQUEST_SCRIPT_ID] = opened.item.id
             requestConfig.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE
             // 设置断点
             requestConfig.headers[constants.HEADER_REQUEST_BREAKPOINTS] = (opened.decorations || []).filter(it => it.options.linesDecorationsClassName === 'breakpoints').map(it => it.range.startLineNumber).join(',')
             const fullName = opened.path()
             bus.status(`开始测试freemarker「${fullName}」`)
             console.log('info.params:',info.params) ;
             request.sendPost('/freemarker/execute', { id: info.id ,params: info.params}, requestConfig).success(res => {
                 opened.running = false ;
               // 打开新窗口显示返回的HTML内容
               const newWindow = window.open('', '_blank') ;
               newWindow.document.write(res) ;
               newWindow.document.close() ;
               // 设置窗口标题
               newWindow.document.title = `FreeMarker结果 - ${fullName}`;
             }).end(() => {
                 bus.status(`freemarker「${fullName}」测试完毕`)
                 opened.running = false
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
