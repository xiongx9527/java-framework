export default (request, $i, modal, JavaClass) => {
    let findResources

    JavaClass.setExtensionAttribute(
        'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaModule',
        () => {
            JavaClass.loadClass('com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaModule')
            return (findResources && (findResources('kafka-datasource')[0]?.children || [])
                .filter((it) => it.key && it.extra === 'producer')
                .map((it) => ({
                    name: it.key,
                    type: 'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.producer.KafkaProducer',
                    comment: it.name
                }))) || []
        }
    )

    return {
        injectResources: (fn) => findResources = fn,
        dialogWidth: '1100px',
        doTest: (info) => {
            request.sendJson('/kafka/datasource/test', info)
                .success((res) => {
                    if (res === 'ok') {
                        modal.alert($i('datasource.connected'), $i('datasource.test'))
                    } else {
                        modal.alert($i('datasource.connectFailed', res), $i('datasource.test'))
                    }
                })
        },
        doDelete: (item) => {
            return new Promise((resolve, reject) => {
                request.sendJson('/kafka/datasource/doDelete', item)
                    .success((res) => {
                        if (res === 'ok') {
                            resolve()
                        } else {
                            reject(new Error(res || '无法删除该数据源'))
                        }
                    })
                    .error((err) => {
                        reject(new Error(err || '删除校验请求失败'))
                    })
            })
        }
    }
}