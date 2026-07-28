export default (request, $i, modal, JavaClass) => {
    let findResources
    const wsModuleClass = 'com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule'

    JavaClass.setExtensionAttribute(
        wsModuleClass,
        () => {
            JavaClass.loadClass(wsModuleClass)
            return (findResources && (findResources('ws-datasource')[0]?.children || [])
                .filter((it) => it.key)
                .map((it) => ({
                    name: it.key,
                    type: wsModuleClass,
                    comment: it.name
                }))) || []
        }
    )

    return {
        injectResources: (fn) => findResources = fn,
        requireScript: false,
        dialogWidth: '1100px',
        doTest: (info) => {
            request.sendJson('/ws/datasource/test', info)
                .success((res) => {
                    if (res === 'ok') {
                        modal.alert($i('datasource.connected'), $i('datasource.test'))
                    } else {
                        modal.alert($i('datasource.connectFailed', res), $i('datasource.test'))
                    }
                })
        }
    }
}
