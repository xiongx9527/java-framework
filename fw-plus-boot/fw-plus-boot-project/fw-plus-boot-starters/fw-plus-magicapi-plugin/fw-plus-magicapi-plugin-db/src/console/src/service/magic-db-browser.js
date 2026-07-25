let activeRequest = null

const invoke = (request, method, url, payload) => new Promise((resolve, reject) => {
    if (!request || typeof request[method] !== 'function') {
        reject(new Error('Magic-API request is unavailable'))
        return
    }
    const response = method === 'sendGet'
        ? request.sendGet(url, payload)
        : request.sendJson(url, payload)
    response.success(resolve).exception((code, message) => reject(new Error(message || String(code)))).error(reject)
})

export const setDbBrowserRequest = request => {
    activeRequest = request
}

export const createDbBrowserService = request => ({
    listDataSources: () => invoke(request, 'sendGet', '/db/browser/datasources'),
    listNamespaces: key => invoke(request, 'sendGet', '/db/browser/namespaces', { key }),
    listObjects: (context, type) => invoke(request, 'sendGet', '/db/browser/objects', { ...context, type }),
    listRoutines: (context, type) => invoke(request, 'sendGet', '/db/browser/routines', { ...context, type }),
    getObjectDetail: target => invoke(request, 'sendGet', '/db/browser/object/detail', target),
    preview: requestBody => {
        const { key, datasourceKey, ...body } = requestBody || {}
        return invoke(request, 'sendJson', '/db/browser/preview', { datasourceKey: datasourceKey || key, ...body })
    }
})

export const getDbBrowserService = () => createDbBrowserService(activeRequest)

export default createDbBrowserService
