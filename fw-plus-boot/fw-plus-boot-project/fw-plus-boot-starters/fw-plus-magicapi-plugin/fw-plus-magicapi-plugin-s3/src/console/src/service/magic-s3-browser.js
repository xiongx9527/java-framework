let activeRequest = null

const invoke = (request, method, url, params) => new Promise((resolve, reject) => {
    if (!request || typeof request[method] !== 'function') {
        reject(new Error('Magic-API request is unavailable'))
        return
    }
    request[method](url, params)
        .success(resolve)
        .exception((code, message) => reject(new Error(message || String(code))))
        .error((error) => reject(error instanceof Error ? error : new Error(String(error || 'Request failed'))))
})

const unwrap = response => response && response.data !== undefined ? response.data : response

export const setS3BrowserRequest = request => {
    activeRequest = request
}

export const createS3BrowserService = request => ({
    listStorages: async () => unwrap(await invoke(request, 'sendGet', '/s3/datasource/availableStorages')) || [],
    listFiles: async (storageKey, params) => unwrap(await invoke(request, 'sendGet', `/s3/datasource/${storageKey}/listFilesResult`, params)) || {},
    generatePresignedUrl: async (storageKey, file) => unwrap(await invoke(request, 'sendGet', `/s3/datasource/${storageKey}/generateUploadPresignedUrl`, {
        url: file.url,
        basePath: file.basePath,
        path: file.path,
        fileName: file.filename || file.name
    })),
    downloadFile: async (storageKey, file) => unwrap(await invoke(request, 'sendGet', `/s3/datasource/${storageKey}/generateUploadPresignedUrl`, {
        url: file.url,
        basePath: file.basePath,
        path: file.path,
        fileName: file.filename || file.name
    })),
    deleteFile: (storageKey, url) => invoke(request, 'sendPost', `/s3/datasource/${storageKey}/delete`, { url })
})

export const getS3BrowserService = () => createS3BrowserService(activeRequest)

export default createS3BrowserService
