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
    if (typeof payload.dimension === 'string' && payload.dimension.trim().length > 0) {
        payload.dimension = Number(payload.dimension)
    }
    if (!payload.dimension && payload.dimension !== 0) {
        delete payload.dimension
    }
    if (typeof payload.metadataKeysText === 'string') {
        payload.metadataKeys = payload.metadataKeysText
            .split(',')
            .map(item => item.trim())
            .filter(item => item.length > 0)
    }
    delete payload.metadataKeysText
    return payload
}

export default (request, $i, modal) => {
    return {
        dialogWidth: '960px',
        doTest: (info) => {
            let payload
            try {
                payload = normalize(info)
            } catch (e) {
                modal.alert(e.message, $i('datasource.test'))
                return
            }
            request.sendJson('/vector/datasource/test', payload)
                .success((res) => {
                    if (res === 'ok' || (res && res.data === 'ok')) {
                        modal.alert($i('datasource.connected'), $i('datasource.test'))
                    } else {
                        const message = res && (res.message || res.msg || res.data) ? (res.message || res.msg || res.data) : res
                        modal.alert($i('datasource.connectFailed', message), $i('datasource.test'))
                    }
                })
        }
    }
}
