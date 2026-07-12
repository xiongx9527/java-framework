export default (request, $i, modal, JavaClass) => {
    let findResources

    JavaClass.setExtensionAttribute(
        'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticsearchDataSourceInfo',
        () => {
            JavaClass.loadClass('com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticsearchDataSourceInfo')
            return (findResources && (findResources('nebula-datasource')[0]?.children || [])
                .map((it) => ({
                    name: it.key,
                    type: 'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticsearchDataSourceInfo',
                    comment: it.name
                }))) || []
        }
    )

    return {
        injectResources: (fn) => findResources = fn,
        dialogWidth: '1100px',
        doTest: (info) => {
            request.sendJson('/elasticsearch/datasource/test', info)
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