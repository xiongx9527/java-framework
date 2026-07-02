export default function MagicRocketMqDataSource(request, $i, modal, JavaClass) {
    let findResources;
    JavaClass.setExtensionAttribute('com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.module.RocketMqModule', () => {
        JavaClass.loadClass('com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.module.RocketMqModule');
        return findResources && (findResources('rocketmq-datasource')[0]?.children || [])
            .filter((it) => it.key && it.extra === 'producer')
            .map((it) => ({
                name: it.key,
                type: 'com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.producer.RocketMqProducer',
                comment: it.name
            })) || [];
    });

    return {
        injectResources: (fn) => findResources = fn,
        dialogWidth: '1100px',
        doTest: (info) => {
            request.sendJson('/rocketmq/datasource/test', info)
                .success((res) => {
                    if (res === 'ok') {
                        modal.alert($i('datasource.connected'), $i('datasource.test'));
                    } else {
                        modal.alert($i('datasource.connectFailed', res), $i('datasource.test'));
                    }
                });
        }
    };
}
