import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import createDbBrowserService from '../src/service/magic-db-browser.js'

const createRequest = () => {
    const calls = []
    const reply = (method, url, payload) => {
        calls.push({ method, url, payload })
        return {
            success(callback) {
                callback({ url, payload })
                return this
            },
            exception() { return this },
            error() { return this }
        }
    }
    return {
        calls,
        sendGet: (url, params) => reply('GET', url, params),
        sendJson: (url, body) => reply('POST', url, body)
    }
}

test('DB 浏览服务通过注入的 request 调用元数据和预览接口', async () => {
    const request = createRequest()
    const service = createDbBrowserService(request)
    const context = { key: 'orders', catalog: 'shop', schema: 'public' }
    const target = { ...context, objectName: 'items', objectType: 'TABLE' }
    const preview = { ...target, page: 2, pageSize: 20, sortColumn: 'id', sortDirection: 'DESC' }

    await Promise.all([
        service.listDataSources(),
        service.listNamespaces('orders'),
        service.listObjects(context, 'TABLE'),
        service.listRoutines(context, 'FUNCTION'),
        service.getObjectDetail(target),
        service.preview(preview)
    ])

    assert.deepEqual(request.calls, [
        { method: 'GET', url: '/db/browser/datasources', payload: undefined },
        { method: 'GET', url: '/db/browser/namespaces', payload: { key: 'orders' } },
        { method: 'GET', url: '/db/browser/objects', payload: { ...context, type: 'TABLE' } },
        { method: 'GET', url: '/db/browser/routines', payload: { ...context, type: 'FUNCTION' } },
        { method: 'GET', url: '/db/browser/object/detail', payload: target },
        { method: 'POST', url: '/db/browser/preview', payload: preview }
    ])
})

test('DB 浏览树为命名空间和对象请求提供加载态与空态', async () => {
    const component = await readFile(new URL('../src/components/magic-db-browser.vue', import.meta.url), 'utf8')

    assert.match(component, /loading = reactive\(\{ sources: false, namespaces: false, objects: false, detail: false, preview: false \}\)/)
    assert.match(component, /v-if="loading\.namespaces && source\.key === loadingSourceKey"/)
    assert.match(component, /v-else-if="source\.key === selectedSource && namespaces\.length === 0"/)
    assert.match(component, /v-if="loading\.objects && namespaceKey\(namespace\) === loadingNamespaceKey"/)
    assert.match(component, /v-else-if="activeNamespace && namespaceKey\(namespace\) === namespaceKey\(activeNamespace\) && \(objectGroups\.every\(group => group\.items\.length === 0\) \|\| !hasFilteredObjects\)"/)
    assert.match(component, /const changePage = offset => \{ const targetPage = previewState\.page \+ offset; loadPreview\(\{ \.\.\.previewState, page: targetPage \}\) \}/)
    assert.match(component, /const loadPreview = async \(state = previewState\)/)
    assert.match(component, /Object\.assign\(previewState, state\)/)
    assert.match(component, /!hasFilteredObjects/)
})
