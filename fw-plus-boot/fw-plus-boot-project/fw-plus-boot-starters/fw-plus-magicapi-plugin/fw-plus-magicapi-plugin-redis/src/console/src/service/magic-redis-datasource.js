import {DATASOURCE_FIELDS} from '../metadata/redis-datasource.js'

/**
 * redis datasource service：资源注入、保存前校验与连接测试。
 * 保存前校验由控制台 doSave 的可选 service.validate 钩子调用，失败抛错即阻止保存请求。
 */
export default (request, $i, modal) => {
    let findResources

    const fallback = (key, name) => {
        const text = $i(key)
        return text === key ? name : text
    }

    const fieldLabel = (name) => fallback('redis.datasource.field.' + name, name)

    /**
     * 保存前校验：必填字段非空（口径与后端 BaseService.validateField 一致），
     * properties 原始 JSON 文本合法后解析写回 info.properties。
     * 失败抛出错误，由控制台弹窗提示并阻止保存请求。
     */
    const validate = (info) => {
        for (const f of DATASOURCE_FIELDS) {
            if (!f.required) {
                continue
            }
            const value = info[f.name] == null ? '' : String(info[f.name]).trim()
            if (value === '') {
                throw new Error($i('redis.datasource.hint.requiredField', fieldLabel(f.name)))
            }
        }
        // properties：JSON 文本暂存在 info.propertiesText，校验通过后写回 properties
        if (info.propertiesText !== undefined) {
            const raw = info.propertiesText == null ? '' : String(info.propertiesText).trim()
            if (raw === '') {
                info.properties = {}
            } else {
                let parsed
                try {
                    parsed = JSON.parse(raw)
                } catch (e) {
                    throw new Error($i('redis.datasource.hint.jsonInvalid'))
                }
                if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
                    throw new Error($i('redis.datasource.hint.jsonInvalid'))
                }
                info.properties = parsed
            }
            delete info.propertiesText
        }
    }

    return {
        injectResources: (fn) => findResources = fn,
        dialogWidth: '1100px',
        validate,
        doTest: (info) => {
            request.sendJson('/redis/datasource/test', info)
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
