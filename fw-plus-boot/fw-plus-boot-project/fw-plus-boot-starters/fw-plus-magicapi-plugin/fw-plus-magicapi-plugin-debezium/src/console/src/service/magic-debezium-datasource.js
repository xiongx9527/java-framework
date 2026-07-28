export default (request, $i, modal, JavaClass) => {
    let findResources

    JavaClass.setExtensionAttribute(
        'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo',
        () => {
            JavaClass.loadClass('com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo')
            return (findResources && (findResources('debezium-datasource')[0]?.children || [])
                .map((it) => ({
                    name: it.key,
                    type: 'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo',
                    comment: it.name
                }))) || []
        }
    )

    return {
        injectResources: (fn) => findResources = fn,
        dialogWidth: '1100px',
        loadConnectorOptions: (success, failure) => {
            request.sendJson('/debezium/datasource/connector-options', {})
                .success((res) => {
                    success && success(res)
                })
                .exception((code, message) => {
                    failure && failure({ code, message })
                })
                .error((err) => {
                    failure && failure(err)
                })
        },
        doTest: (info) => {
            request.sendJson('/debezium/datasource/test', info)
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
