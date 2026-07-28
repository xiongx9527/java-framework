export default (request, $i, modal, JavaClass) => {
    let findResources

    JavaClass.setExtensionAttribute(
        'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.module.RabbitMqModule',
        () => {
            JavaClass.loadClass('com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.module.RabbitMqModule')
            return (findResources && (findResources('rabbitmq-datasource')[0]?.children || [])
                .filter((it) => it.key && it.extra === 'producer')
                .map((it) => ({
                    name: it.key,
                    type: 'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.producer.RabbitMqProducer',
                    comment: it.name
                }))) || []
        }
    )

    return {
        injectResources: (fn) => findResources = fn,
        dialogWidth: '1100px',
        doTest: (info) => {
            request.sendJson('/rabbitmq/datasource/test', info)
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