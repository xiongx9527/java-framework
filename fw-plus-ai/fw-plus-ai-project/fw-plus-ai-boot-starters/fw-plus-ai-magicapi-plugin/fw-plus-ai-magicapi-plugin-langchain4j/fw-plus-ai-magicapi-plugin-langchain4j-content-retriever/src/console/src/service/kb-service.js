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

const unwrap = (resolve, reject) => ({
    success: (res) => {
        if (res && res.code === 200) {
            resolve(res.data);
        } else {
            reject(new Error((res && res.message) || '请求失败'));
        }
    }
});

export const kbApi = {
    listDatasets: () => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/dataset').success(unwrap(resolve, reject)).error?.(e => reject(new Error(e)));
    }),
    createDataset: (data) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset', data).success(unwrap(resolve, reject));
    }),
    updateDataset: (id, data) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + id, data).success(unwrap(resolve, reject));
    }),
    deleteDataset: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + id, {}).success(unwrap(resolve, reject));
    }),
    hitTest: (id, body) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + id + '/hitTest', body).success(unwrap(resolve, reject));
    }),
    previewSplit: (body) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/preview-split', body).success(unwrap(resolve, reject));
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
        requireRequest().sendJson('/kb/dataset/import', JSON.parse(jsonText)).success(unwrap(resolve, reject));
    }),
    moveDocument: (id, datasetId) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id + '/move', {datasetId})
            .success(unwrap(resolve, reject));
    }),
    retrievers: (id) => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/dataset/' + id + '/retrievers').success(unwrap(resolve, reject));
    }),
    listDocuments: (datasetId) => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/dataset/' + datasetId + '/document').success(unwrap(resolve, reject));
    }),
    reindex: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id + '/reindex', {}).success(unwrap(resolve, reject));
    }),
    patchDocument: (id, field, value) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id + '/' + field + '?value=' + value, {})
            .success(unwrap(resolve, reject));
    }),
    batchDocuments: (datasetId, documentIds, action) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/dataset/' + datasetId + '/document/batch',
            { documentIds, action }).success(unwrap(resolve, reject));
    }),
    setMetadata: (id, metadata) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id + '/metadata', metadata)
            .success(unwrap(resolve, reject));
    }),
    deleteDocument: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/document/' + id, {}).success(unwrap(resolve, reject));
    }),
    listSegments: (documentId) => new Promise((resolve, reject) => {
        requireRequest().sendGet('/kb/document/' + documentId + '/segment').success(unwrap(resolve, reject));
    }),
    updateSegment: (id, body) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/segment/' + id, body).success(unwrap(resolve, reject));
    }),
    patchSegment: (id, value) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/segment/' + id + '/enabled?value=' + value, {})
            .success(unwrap(resolve, reject));
    }),
    deleteSegment: (id) => new Promise((resolve, reject) => {
        requireRequest().sendJson('/kb/segment/' + id, {}).success(unwrap(resolve, reject));
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
                if (res && res.code === 200) {
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
