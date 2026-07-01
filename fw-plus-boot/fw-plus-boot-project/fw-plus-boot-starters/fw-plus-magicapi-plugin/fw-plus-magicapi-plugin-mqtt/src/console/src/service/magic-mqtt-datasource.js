export default (request, $i, modal, JavaClass) => {
    let findResources

    JavaClass.setExtensionAttribute(
        'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.module.MqttModule',
        () => {
            JavaClass.loadClass('com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.module.MqttModule')
            return (findResources && (findResources('mqtt-datasource')[0]?.children || [])
                .filter((it) => it.key)
                .map((it) => ({
                    name: it.key,
                    type: 'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.module.MqttModule',
                    comment: it.name
                }))) || []
        }
    )

    return {
        injectResources: (fn) => findResources = fn,
        dialogWidth: '1100px',
        doTest: (info) => {
            request.sendJson('/mqtt/datasource/test', info)
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