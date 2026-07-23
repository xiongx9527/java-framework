let s3Request = null

export const loadS3PlatformOptions = () => {
    return new Promise((resolve) => {
        if (!s3Request) {
            resolve(null)
            return
        }
        s3Request.sendGet('/s3/datasource/platform/options')
            .success((res) => {
                if (Array.isArray(res)) {
                    resolve(res)
                    return
                }
                resolve(res && Array.isArray(res.data) ? res.data : null)
            })
            .exception(() => {
                resolve(null)
            })
            .error(() => {
                resolve(null)
            })
    })
}

export default (request, $i, modal, JavaClass) => {
    s3Request = request
    let findResources

    JavaClass.setExtensionAttribute(
        'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo',
        () => {
            JavaClass.loadClass('com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo')
            return (findResources && (findResources('s3-datasource')[0]?.children || [])
                .map((it) => ({
                    name: it.key,
                    type: 'com.jugui.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo',
                    comment: it.name
                }))) || []
        }
    )

    return {
        injectResources: (fn) => findResources = fn,
        dialogWidth: '1100px',
        doTest: (info) => {
            request.sendJson('/s3/datasource/test', info)
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
