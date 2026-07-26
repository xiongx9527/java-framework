const parseJsonObject = (value) => {
    if (!value) {
        return {}
    }
    if (typeof value === 'object') {
        return value
    }
    const parsed = JSON.parse(value)
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
        throw new Error('properties must be a JSON object')
    }
    return parsed
}

const normalize = (info) => {
    const payload = Object.assign({}, info || {})
    payload.properties = parseJsonObject(payload.propertiesText || payload.properties)
    delete payload.propertiesText
    return payload
}

export default (request, $i, modal) => ({
    dialogWidth: '960px',
    doTest: (info) => {
        let payload
        try {
            payload = normalize(info)
        } catch (e) {
            modal.alert(e.message, $i('datasource.test'))
            return
        }
        request.sendJson('/model/datasource/test', payload)
            .success((res) => {
                if (res === 'ok' || (res && res.data === 'ok')) {
                    modal.alert($i('datasource.connected'), $i('datasource.test'))
                } else {
                    const message = res && (res.message || res.msg || res.data) ? (res.message || res.msg || res.data) : res
                    modal.alert($i('datasource.connectFailed', message), $i('datasource.test'))
                }
            })
    }
})
