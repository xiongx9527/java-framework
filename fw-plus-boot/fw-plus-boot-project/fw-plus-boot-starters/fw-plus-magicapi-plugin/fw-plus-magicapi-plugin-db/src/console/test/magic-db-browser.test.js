import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import createDbBrowserService from '../src/service/magic-db-browser.js'
import zhCN from '../src/i18n/zh-cn.js'
import en from '../src/i18n/en.js'

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
        { method: 'POST', url: '/db/browser/preview', payload: { datasourceKey: 'orders', catalog: 'shop', schema: 'public', objectName: 'items', objectType: 'TABLE', page: 2, pageSize: 20, sortColumn: 'id', sortDirection: 'DESC' } }
    ])
})

test('DB 管理入口使用预期展示名称', () => {
    assert.equal(zhCN.db.title, 'db管理')
    assert.equal(en.db.title, 'DB Management')
})

test('DB 管理入口通过通用 adminOnly 标记受 adminEnabled 门控', async () => {
    const pluginSource = await readFile(new URL('../src/index.js', import.meta.url), 'utf8')
    const hostToolbarSource = await readFile(new URL('../../../../../fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-toolbar.vue', import.meta.url), 'utf8')

    assert.match(pluginSource, /id:\s*'db-browser'/)
    assert.match(pluginSource, /adminOnly:\s*true/)
    // magic-toolbar 靠 plugin.name 反查 PLUGINS_DATA 的 adminEnabled，工厂返回缺顶层 name 时入口永远被过滤
    assert.match(pluginSource, /name:\s*'db'/)
    assert.match(hostToolbarSource, /isPluginToolbarVisible/)
    assert.match(hostToolbarSource, /toolbar\.adminOnly/)
    assert.match(hostToolbarSource, /pluginData\.adminEnabled === true/)
})

test('DB 浏览树为命名空间和对象请求提供加载态与空态', async () => {
    const component = await readFile(new URL('../src/components/magic-db-browser.vue', import.meta.url), 'utf8')

    assert.match(component, /loading = reactive\(\{ sources: false, namespaces: false, objects: false, detail: false, preview: false \}\)/)
    assert.match(component, /v-if="loading\.namespaces && source\.key === loadingSourceKey"/)
    assert.match(component, /v-else-if="source\.key === selectedSource && namespaces\.length === 0"/)
    assert.match(component, /v-if="loading\.objects && namespaceKey\(namespace\) === loadingNamespaceKey"/)
    assert.match(component, /v-else-if="activeNamespace && namespaceKey\(namespace\) === namespaceKey\(activeNamespace\) && \(objectGroups\.every\(group => group\.items\.length === 0\) \|\| !hasFilteredObjects\)"/)
    assert.match(component, /const changePage = offset => \{ const targetPage = previewState\.page \+ offset; loadPreview\(\{ \.\.\.previewState, page: targetPage \}\) \}/)
    assert.match(component, /!hasFilteredObjects/)
})

test('DB 预览只在成功后提交查询状态，并在失败时恢复编辑态', async () => {
    const component = await readFile(new URL('../src/components/magic-db-browser.vue', import.meta.url), 'utf8')
    const loadPreviewFor = component.match(/const loadPreviewFor = async \(item, state\) => \{([\s\S]*?)\n\}/)

    assert.match(component, /const previewDraft = reactive\(\{ sortColumn: '', sortDirection: 'ASC', filterColumn: '', filterValue: '' \}\)/)
    assert.ok(loadPreviewFor)
    assert.match(loadPreviewFor[1], /state\.filterColumn && state\.filterValue !== ''/)
    assert.doesNotMatch(loadPreviewFor[1], /filterColumn\.value|filterValue\.value/)
    assert.match(component, /preview\.value = loadedPreview\n\s*Object\.assign\(previewState, state\)/)
    assert.match(component, /if \(requestSeq !== previewRequestSeq\.value\) return\s*restorePreviewDraft\(\)\s*reportError\(cause\)/)
})

test('DB 浏览请求仅允许最新响应提交状态和结束加载态', async () => {
    const component = await readFile(new URL('../src/components/magic-db-browser.vue', import.meta.url), 'utf8')

    assert.match(component, /const sourceRequestSeq = ref\(0\)/)
    assert.match(component, /const dataSourcesRequestSeq = ref\(0\)/)
    assert.match(component, /const namespaceRequestSeq = ref\(0\)/)
    assert.match(component, /const objectRequestSeq = ref\(0\)/)
    assert.match(component, /const previewRequestSeq = ref\(0\)/)
    assert.match(component, /const requestSeq = \+\+dataSourcesRequestSeq\.value/)
    assert.match(component, /if \(requestSeq !== dataSourcesRequestSeq\.value\) return\s*sources\.value = Array\.isArray\(result\) \? result : \[\]/)
    assert.match(component, /catch \(cause\) \{\s*if \(requestSeq === dataSourcesRequestSeq\.value\) reportError\(cause\)\s*\} finally \{\s*if \(requestSeq === dataSourcesRequestSeq\.value\) loading\.sources = false\s*\}/)
    assert.match(component, /const requestSeq = \+\+sourceRequestSeq\.value/)
    assert.match(component, /if \(requestSeq !== sourceRequestSeq\.value\) return\s*selectedSource\.value = source\.key/)
    assert.match(component, /if \(requestSeq === sourceRequestSeq\.value\) \{\s*loading\.namespaces = false/)
    assert.match(component, /const requestSeq = \+\+namespaceRequestSeq\.value/)
    assert.match(component, /if \(requestSeq !== namespaceRequestSeq\.value\) return\s*activeNamespace\.value = namespace/)
    assert.match(component, /if \(requestSeq === namespaceRequestSeq\.value\) \{\s*loading\.objects = false/)
    assert.match(component, /const requestSeq = \+\+objectRequestSeq\.value/)
    assert.match(component, /if \(requestSeq !== objectRequestSeq\.value\) return\s*selectedObject\.value = item/)
    assert.match(component, /if \(requestSeq !== objectRequestSeq\.value\) return\s*selectedObject\.value = item\s*detail\.value = loadedDetail\s*if \(previewRequestSeqForObject === previewRequestSeq\.value\)/)
    assert.match(component, /if \(previewRequestSeqForObject === previewRequestSeq\.value\) \{\s*preview\.value = loadedPreview\s*Object\.assign\(previewState, initialPreviewState\)\s*syncPreviewDraft\(initialPreviewState\)\s*\}/)
    assert.match(component, /if \(requestSeq === objectRequestSeq\.value\) \{\s*loading\.detail = false/)
    assert.match(component, /const requestSeq = \+\+previewRequestSeq\.value/)
    assert.match(component, /if \(requestSeq !== previewRequestSeq\.value\) return\s*preview\.value = loadedPreview/)
    assert.match(component, /if \(requestSeq === previewRequestSeq\.value\) \{\s*loading\.preview = false/)
})
