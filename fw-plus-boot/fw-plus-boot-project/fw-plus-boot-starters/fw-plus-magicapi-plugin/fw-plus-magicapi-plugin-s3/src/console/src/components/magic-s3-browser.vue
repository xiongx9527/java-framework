<template>
    <section class="magic-s3-browser">
        <header class="s3-toolbar">
            <label>{{ $i('s3.browser.storage') }}</label>
            <magic-select v-model:value="selectedStorageKey" :placeholder="$i('s3.browser.selectStorage')" :options="storageOptions" width="180px" :disabled="loading.storages" @select="selectStorage" />
            <label>{{ $i('s3.browser.basePath') }}</label>
            <magic-input v-model:value="basePathDraft" width="180px" :disabled="!selectedStorageKey || !basePathEditing || loading.files" @keyup.enter="basePathEditing && saveBasePath()" />
            <button class="s3-button" :disabled="!selectedStorageKey || loading.files" @click="handleBasePathAction">{{ basePathEditing ? $i('s3.browser.save') : $i('s3.browser.edit') }}</button>
            <label>{{ $i('s3.browser.searchLabel') }}</label>
            <magic-input v-model:value="search" :placeholder="$i('s3.browser.search')" width="220px" :disabled="!selectedStorageKey || loading.files" @keyup.enter="reloadFiles" />
            <button class="s3-button" :disabled="!selectedStorageKey || loading.files" @click="reloadFiles">{{ $i('s3.browser.searchAction') }}</button>
            <button class="s3-button" :disabled="!selectedStorageKey || loading.files" @click="reloadFiles">{{ $i('s3.browser.refresh') }}</button>
        </header>

        <nav class="s3-path" :aria-label="$i('s3.browser.path')">
            <button class="s3-path-button" @click="changePath(basePath)">/</button>
            <template v-for="segment in pathSegments" :key="segment.path">
                <span>/</span>
                <button class="s3-path-button" @click="changePath(segment.path)">{{ segment.name }}</button>
            </template>
        </nav>

        <div class="s3-content">
            <div class="s3-list">
                <p v-if="loading.storages || loading.files" class="s3-state">{{ $i('s3.browser.loading') }}</p>
                <p v-else-if="!selectedStorageKey" class="s3-state">{{ $i('s3.browser.selectStorage') }}</p>
                <p v-else-if="items.length === 0" class="s3-state">{{ $i('s3.browser.empty') }}</p>
                <button v-for="item in items" :key="itemKey(item)" class="s3-item" :class="{ selected: selectedFile && itemKey(selectedFile) === itemKey(item) }" @click="selectItem(item)">
                    <span class="s3-item-type">{{ item.type === 'folder' ? $i('s3.browser.folder') : $i('s3.browser.file') }}</span>
                    <span class="s3-item-name">{{ item.name }}</span>
                    <span class="s3-item-meta">{{ item.size || item.fileSize || '-' }}</span>
                    <span class="s3-item-meta">{{ item.lastModified || item.updateTime || '-' }}</span>
                </button>
                <button v-if="nextMarker" class="s3-button s3-load-more" :disabled="loading.more" @click="loadFiles(true)">{{ loading.more ? $i('s3.browser.loading') : $i('s3.browser.loadMore') }}</button>
            </div>
            <aside v-if="selectedFile" class="s3-details">
                <header><strong>{{ selectedFile.name }}</strong><button class="s3-icon-button" @click="selectedFile = null">x</button></header>
                <p>{{ $i('s3.browser.previewUrl') }}: <a v-if="previewUrl" :href="previewUrl" target="_blank" rel="noopener">{{ previewUrl }}</a><span v-else>-</span></p>
                <div class="s3-actions">
                    <button class="s3-button" :disabled="selectedFile.type === 'folder' || loading.preview" @click="previewFile">{{ $i('s3.browser.preview') }}</button>
                    <button class="s3-button" :disabled="selectedFile.type === 'folder' || loading.download" @click="downloadFile">{{ $i('s3.browser.download') }}</button>
                    <button class="s3-button danger" @click="requestDeleteFile">{{ $i('s3.browser.delete') }}</button>
                </div>
                <pre>{{ JSON.stringify(selectedFile, null, 2) }}</pre>
            </aside>
        </div>

        <div v-if="deleteCandidate" class="s3-confirm" role="dialog" aria-modal="true">
            <p>{{ $i('s3.browser.confirmDelete', deleteCandidate.name) }}</p>
            <button class="s3-button" @click="deleteCandidate = null">{{ $i('s3.browser.cancel') }}</button>
            <button class="s3-button danger" :disabled="loading.deleting" @click="confirmDeleteFile">{{ $i('s3.browser.delete') }}</button>
        </div>
    </section>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { getS3BrowserModal, getS3BrowserService } from '../service/magic-s3-browser.js'

const $i = inject('i18n.format')
const service = () => getS3BrowserService()
const storages = ref([])
const selectedStorageKey = ref('')
const path = ref('/')
const basePath = ref('/')
const basePathDraft = ref('/')
const basePathEditing = ref(false)
const search = ref('')
const items = ref([])
const nextMarker = ref(null)
const selectedFile = ref(null)
const previewUrl = ref('')
const deleteCandidate = ref(null)
const fileRequestSeq = ref(0)
const loading = reactive({ storages: false, files: false, more: false, preview: false, download: false, deleting: false })
const storageOptions = computed(() => [
    { text: $i('s3.browser.select'), value: '' },
    ...storages.value.map(storage => ({
        text: storage.name || storage.key,
        value: storage.key
    }))
])
const pathSegments = computed(() => {
    const relativePath = path.value.startsWith(basePath.value) ? path.value.slice(basePath.value.length) : path.value
    let current = basePath.value
    return relativePath.split('/').filter(Boolean).map(name => ({ name, path: current += `${name}/` }))
})
const itemKey = item => `${item.type}:${item.path || ''}:${item.name}`
const normalizedPath = value => {
    const pathValue = value || '/'
    return `${pathValue.startsWith('/') ? pathValue : `/${pathValue}`}${pathValue.endsWith('/') ? '' : '/'}`.replace(/\/{2,}/g, '/')
}
const fallbackText = key => $i(key)
const extractErrorMessage = (cause, fallback) => {
    if (typeof cause === 'string' && cause.trim()) return cause.trim()
    if (cause && cause.message) return cause.message
    const response = cause && (cause.response || cause.data || cause)
    if (response && typeof response === 'object') {
        return response.message || response.msg || response.error || fallback
    }
    return fallback
}
const showError = (cause, fallbackKey) => {
    const message = extractErrorMessage(cause, fallbackText(fallbackKey))
    const modal = getS3BrowserModal()
    if (modal && typeof modal.alert === 'function') {
        modal.alert(message, $i('s3.browser.errorTitle'))
    } else if (typeof window !== 'undefined' && typeof window.alert === 'function') {
        window.alert(message)
    }
}
const mapItems = result => [
    ...(result.dirList || result.directories || result.folders || []).map(file => ({ ...file, name: file.name, type: 'folder' })),
    ...(result.fileList || result.files || []).map(file => ({ ...file, name: file.filename || file.name, filename: file.filename || file.name, type: 'file' }))
]
const fileRequestContext = marker => ({
    storageKey: selectedStorageKey.value,
    basePath: basePath.value,
    path: path.value,
    search: search.value.trim(),
    marker: marker || null
})
const sameFileRequestContext = expected => {
    const current = fileRequestContext(expected.marker)
    return current.storageKey === expected.storageKey && current.basePath === expected.basePath
        && current.path === expected.path && current.search === expected.search && current.marker === expected.marker
}
const requestParams = context => {
    const relativePath = context.path.startsWith(context.basePath) ? context.path.slice(context.basePath.length) : context.path
    const params = { basePath: context.basePath, path: relativePath }
    if (context.search) params.filenamePrefix = context.search
    if (context.marker) params.marker = context.marker
    return params
}
const loadFiles = async (more = false) => {
    const expectedContext = fileRequestContext(more ? nextMarker.value : null)
    if (!expectedContext.storageKey || (more && !expectedContext.marker)) return
    const requestSeq = ++fileRequestSeq.value
    loading[more ? 'more' : 'files'] = true
    try {
        const result = await service().listFiles(expectedContext.storageKey, requestParams(expectedContext))
        if (requestSeq !== fileRequestSeq.value || !sameFileRequestContext(expectedContext)) return
        const loadedItems = mapItems(result)
        items.value = more ? [...items.value, ...loadedItems] : loadedItems
        nextMarker.value = result.nextMarker || result.marker || null
    } catch (cause) {
        if (requestSeq !== fileRequestSeq.value || !sameFileRequestContext(expectedContext)) return
        if (!more) items.value = []
        showError(cause, more ? 's3.browser.loadMoreFailed' : 's3.browser.listFilesFailed')
    } finally {
        if (requestSeq === fileRequestSeq.value) {
            loading[more ? 'more' : 'files'] = false
        }
    }
}
const selectStorage = () => {
    if (!selectedStorageKey.value) {
        basePath.value = '/'
        basePathDraft.value = basePath.value
        basePathEditing.value = false
        path.value = basePath.value
        items.value = []
        nextMarker.value = null
        selectedFile.value = null
        previewUrl.value = ''
        return
    }
    const storage = storages.value.find(item => item.key === selectedStorageKey.value)
    basePath.value = normalizedPath(storage && storage.basePath)
    basePathDraft.value = basePath.value
    basePathEditing.value = false
    path.value = basePath.value
    selectedFile.value = null
    previewUrl.value = ''
    loadFiles()
}
const changePath = target => { path.value = normalizedPath(target); selectedFile.value = null; previewUrl.value = ''; loadFiles() }
const selectItem = item => item.type === 'folder' ? changePath(`${path.value}${item.name}`) : (selectedFile.value = item, previewUrl.value = '')
const saveBasePath = () => {
    try {
        basePath.value = normalizedPath(basePathDraft.value)
        basePathDraft.value = basePath.value
        basePathEditing.value = false
        changePath(basePath.value)
    } catch (cause) {
        showError(cause, 's3.browser.saveBasePathFailed')
    }
}
const handleBasePathAction = () => {
    if (!basePathEditing.value) {
        basePathEditing.value = true
        return
    }
    saveBasePath()
}
const reloadFiles = () => { selectedFile.value = null; previewUrl.value = ''; loadFiles() }
const cleanUrl = url => typeof url === 'string' ? url.trim().replace(/`/g, '') : ''
const resolveFileUrl = async targetFile => {
    const url = await service().generatePresignedUrl(selectedStorageKey.value, targetFile)
    const resolvedUrl = cleanUrl(url)
    if (!resolvedUrl) throw new Error($i('s3.browser.previewFailed'))
    previewUrl.value = resolvedUrl
    return resolvedUrl
}
const previewFile = async () => {
    loading.preview = true
    try {
        window.open(await resolveFileUrl(selectedFile.value), '_blank', 'noopener')
    } catch (cause) {
        showError(cause, 's3.browser.previewFailed')
    } finally {
        loading.preview = false
    }
}
const downloadFile = async () => {
    loading.download = true
    try {
        const url = cleanUrl(await service().downloadFile(selectedStorageKey.value, selectedFile.value))
        if (!url) throw new Error($i('s3.browser.downloadFailed'))
        previewUrl.value = url
        const link = document.createElement('a')
        link.href = url
        link.target = '_blank'
        link.rel = 'noopener'
        link.download = selectedFile.value.name || selectedFile.value.filename || 'download'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
    } catch (cause) {
        showError(cause, 's3.browser.downloadFailed')
    } finally {
        loading.download = false
    }
}
const confirmDeleteFile = async (targetFile = deleteCandidate.value) => {
    if (!targetFile) return
    loading.deleting = true
    try {
        await service().deleteFile(selectedStorageKey.value, targetFile.url)
        selectedFile.value = null
        previewUrl.value = ''
        deleteCandidate.value = null
        await loadFiles()
    } catch (cause) {
        showError(cause, 's3.browser.deleteFailed')
    } finally {
        loading.deleting = false
    }
}
const requestDeleteFile = () => {
    if (!selectedFile.value) return
    const modal = getS3BrowserModal()
    if (modal && typeof modal.confirm === 'function') {
        modal.confirm($i('s3.browser.delete'), $i('s3.browser.confirmDelete', selectedFile.value.name), () => confirmDeleteFile(selectedFile.value))
        return
    }
    deleteCandidate.value = selectedFile.value
}
const loadStorages = async () => {
    ++fileRequestSeq.value
    loading.storages = true
    try {
        storages.value = await service().listStorages()
    } catch (cause) {
        showError(cause, 's3.browser.loadStoragesFailed')
    } finally {
        loading.storages = false
    }
}

onMounted(loadStorages)
</script>

<style scoped>
.magic-s3-browser {
    min-height: 420px;
    height: 100%;
    display: flex;
    flex-direction: column;
    color: var(--main-color, #333);
    background: var(--main-background-color, #fff);
    position: relative;
}
.s3-toolbar {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
    padding: 5px;
    border-bottom: 1px solid var(--main-border-color);
}
.s3-toolbar label {
    display: inline-block;
    width: auto;
    height: var(--magic-input-height);
    line-height: var(--magic-input-height);
    font-weight: 400;
    text-align: right;
    padding: 0 5px;
}
.s3-button,
.s3-icon-button {
    height: var(--magic-input-height);
    border: 1px solid var(--main-border-color);
    background: var(--main-background-color, #fff);
    color: inherit;
    padding: 0 8px;
    cursor: pointer;
}
.s3-button:hover,
.s3-icon-button:hover,
.s3-item:hover,
.s3-item.selected {
    background: var(--main-hover-background-color, #eef5ff);
}
.s3-button:disabled {
    cursor: not-allowed;
    opacity: .55;
}
.s3-path {
    display: flex;
    align-items: center;
    gap: 2px;
    min-width: 0;
    padding: 5px;
    border-bottom: 1px solid var(--main-border-color);
    overflow: hidden;
}
.s3-path-button {
    border: 0;
    background: transparent;
    height: var(--magic-input-height);
    padding: 0 3px;
    cursor: pointer;
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}
.s3-content {
    flex: 1;
    min-height: 0;
    display: flex;
}
.s3-list {
    flex: 1;
    min-width: 0;
    overflow: auto;
}
.s3-item {
    display: grid;
    grid-template-columns: 52px minmax(0, 1fr) 100px 160px;
    gap: 8px;
    align-items: center;
    width: 100%;
    min-height: 32px;
    border: 0;
    border-bottom: 1px solid var(--main-border-color);
    text-align: left;
    background: transparent;
    color: inherit;
    cursor: pointer;
}
.s3-item-type,
.s3-item-meta {
    color: var(--main-secondary-color, #667);
    font-size: 12px;
}
.s3-item-name,
.s3-details a,
.s3-details pre {
    overflow-wrap: anywhere;
}
.s3-state {
    margin: 0;
    padding: 18px;
    text-align: center;
    color: var(--main-secondary-color, #667);
}
.s3-load-more {
    display: block;
    margin: 10px auto;
}
.s3-details {
    width: min(42%, 420px);
    min-width: 300px;
    overflow: auto;
    border-left: 1px solid var(--main-border-color);
    padding: 10px;
}
.s3-details header {
    display: flex;
    justify-content: space-between;
    gap: 8px;
}
.s3-details pre {
    white-space: pre-wrap;
}
.s3-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    padding: 8px 0;
}
.s3-confirm {
    position: absolute;
    inset: auto 16px 16px auto;
    padding: 12px;
    border: 1px solid var(--main-border-color);
    background: var(--main-background-color, #fff);
    box-shadow: 0 3px 12px #0003;
}
.danger {
    color: #b42318;
}
</style>
