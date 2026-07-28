export default function (bus, constants, $i, Message, request) {
    return {
        getIcon: item => ['Word', '#2B579A'],
        name: $i('word.name'),
        language: 'json',
        defaultScript: JSON.stringify({
            header: '[]',
            footer: '[]',
            main: '[]',
            paperDirection: 'vertical',
            width: 794,
            height: 1123,
            margins: '[]'
        }, null, 2),
        runnable: true,
        requirePath: true,
        doTest: (opened) => {
            opened.running = true
            const info = opened.item
            const requestConfig = {
                url: '/word/design/generate/' + info.id,
                method: 'POST',
                responseType: 'blob',
                headers: {},
                withCredentials: true,
                data: { data: {} }
            }
            requestConfig.headers[constants.HEADER_REQUEST_CLIENT_ID] = constants.CLIENT_ID
            requestConfig.headers[constants.HEADER_MAGIC_TOKEN] = constants.HEADER_MAGIC_TOKEN_VALUE
            const fullName = opened.path()
            bus.$emit(Message.SWITCH_TOOLBAR, 'word-result')
            bus.status('开始生成Word文档「' + fullName + '」')
            request.execute(requestConfig).then(response => {
                opened.running = false
                const blob = response.data
                const url = window.URL.createObjectURL(blob)
                info.responseBody = url
                bus.status('Word文档「' + fullName + '」生成成功')
                bus.$emit(Message.SWITCH_TOOLBAR, 'word-result')
            }).catch(error => {
                opened.running = false
                bus.status('Word文档「' + fullName + '」生成失败')
                request.processError(error)
            })
        },
        merge: item => item
    }
}
