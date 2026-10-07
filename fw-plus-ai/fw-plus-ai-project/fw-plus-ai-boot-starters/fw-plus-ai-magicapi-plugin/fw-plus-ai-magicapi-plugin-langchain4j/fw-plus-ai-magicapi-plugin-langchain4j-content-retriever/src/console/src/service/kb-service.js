/**
 * 知识库管理端请求封装（/kb/**，随 magic 控制台鉴权）。
 * request 为 magic 控制台注入的请求对象（sendGet/sendJson），由 index.js 调 bindKbRequest 绑定。
 */

let requestInstance = null;

export const bindKbRequest = (request) => {
    requestInstance = request;
};

const requireRequest = () => {
    if (!requestInstance) {
        throw new Error('kb request 未绑定');
    }
    return requestInstance;
};

// request.js 语义：仅 code===1 触发 success（参数为解包后的 data）；code!==1 触发
// exception(code, message)；HTTP 异常触发 error。成功分支直接 resolve(data)，
// 失败分支由各调用点挂载的 exception/error 统一 reject，交组件 catch 展示。
const unwrap = (resolve, reject) => (res) => resolve(res);
const failBind = (reject) => ({
    exception: (code, message) => reject(new Error(message || ('请求失败: ' + code))),
    error: (e) => reject(new Error((e && e.message) || '网络请求失败'))
});

export const kbApi = {
    listDatasets: () => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/dataset').success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    createDataset: (data) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset', data).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    updateDataset: (id, data) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + id, data).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    deleteDataset: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + id, {}).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    hitTest: (id, body) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + id + '/hitTest', body).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    previewSplit: (body) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/preview-split', body).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    exportDataset: (id, name) => {
        const xhr = new XMLHttpRequest();
        xhr.open('GET', '/kb/dataset/' + id + '/export');
        xhr.responseType = 'blob';
        xhr.onload = () => {
            const url = URL.createObjectURL(xhr.response);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'kb-dataset-' + (name || id) + '.json';
            a.click();
            URL.revokeObjectURL(url);
        };
        xhr.send();
    },
    importDataset: (jsonText) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/import', JSON.parse(jsonText)).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    syncDatasetVectors: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + id + '/sync-vectors', {}).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    syncDatasetToEs: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + id + '/sync-es', {}).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    metadataFields: (datasetId) => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/dataset/' + datasetId + '/metadata-fields').success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    createMetadataField: (datasetId, body) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + datasetId + '/metadata-fields', body)
            .success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    deleteMetadataField: (fieldId) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/metadata-fields/' + fieldId + '/delete', {})
            .success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    moveDocument: (id, datasetId) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id + '/move', {datasetId})
            .success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    retrievers: (id) => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/dataset/' + id + '/retrievers').success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    listDocuments: (datasetId, page, pageSize) => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/dataset/' + datasetId + '/document?page=' + (page || 1)
            + '&pageSize=' + (pageSize || 20)).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    reindex: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id + '/reindex', {}).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    patchDocument: (id, field, value) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id + '/' + field + '?value=' + value, {})
            .success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    batchDocuments: (datasetId, documentIds, action) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + datasetId + '/document/batch',
            { documentIds, action }).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    setMetadata: (id, metadata) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id + '/metadata', metadata)
            .success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    deleteDocument: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id, {}).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    listSegments: (documentId, page, pageSize) => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/document/' + documentId + '/segment?page=' + (page || 1)
            + '&pageSize=' + (pageSize || 20)).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    createSegment: (documentId, body) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + documentId + '/segment', body).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    updateText: (id, text) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id + '/text', {text}).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    updateSegment: (id, body) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/segment/' + id, body).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    patchSegment: (id, value) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/segment/' + id + '/enabled?value=' + value, {})
            .success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    deleteSegment: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/segment/' + id, {}).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    /**
     * multipart 上传：magic request 封装不支持 FormData，走原生 XHR（同源，携带控制台登录态）。
     */
    upload: (datasetId, file) => new Promise((resolve, reject) => {
        const form = new FormData();
        form.append('datasetId', datasetId);
        form.append('file', file);
        const xhr = new XMLHttpRequest();
        xhr.open('POST', '/kb/document/upload');
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.onload = () => {
            try {
                const res = JSON.parse(xhr.responseText);
                if (res && res.code === 1) {
                    resolve(res.data);
                } else {
                    reject(new Error((res && res.message) || '上传失败'));
                }
            } catch (e) {
                reject(new Error('上传失败: ' + xhr.status));
            }
        };
        xhr.onerror = () => reject(new Error('上传失败'));
        xhr.send(form);
    }),
    /** 读取知识库全局设置（kb_setting 单行）。 */
    settings: () => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/settings').success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    /** 保存知识库全局设置：后端归一化后落库并热替换快照，保存即生效。 */
    saveSettings: (setting) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/settings', setting).success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    /** 对外 API 可选认证 provider 清单（当前 classpath 实际可用）。 */
    authProviders: () => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/openapi-auth-providers').success(unwrap(resolve, reject)).exception(failBind(reject).exception).error(failBind(reject).error);
    }),
    /** 下载原始文档：XHR blob 触发浏览器保存。 */
    download: (id, fileName) => {
        const xhr = new XMLHttpRequest();
        xhr.open('GET', '/kb/document/' + id + '/download');
        xhr.responseType = 'blob';
        xhr.onload = () => {
            const url = URL.createObjectURL(xhr.response);
            const a = document.createElement('a');
            a.href = url;
            a.download = fileName || 'download';
            a.click();
            URL.revokeObjectURL(url);
        };
        xhr.send();
    }
};
