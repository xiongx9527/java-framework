<template>
    <section class="magic-s3-browser">
        <header class="s3-toolbar">
            <select v-model="selectedStorageKey" :disabled="loading.storages" @change="selectStorage">
                <option value="">{{ $i('s3.browser.selectStorage') }}</option>
                <option v-for="storage in storages" :key="storage.key" :value="storage.key">{{ storage.name || storage.key }}</option>
            </select>
            <nav class="s3-path" :aria-label="$i('s3.browser.path')">
                <button @click="changePath(basePath)">/</button>
                <template v-for="segment in pathSegments" :key="segment.path">
                    <span>/</span><button @click="changePath(segment.path)">{{ segment.name }}</button>
                </template>
            </nav>
            <label class="s3-base-path">{{ $i('s3.browser.basePath') }}<input v-model="basePathDraft" @keyup.enter="saveBasePath" /></label>
            <button :disabled="!selectedStorageKey || loading.files" @click="saveBasePath">{{ $i('s3.browser.save') }}</button>
            <input v-model="search" :placeholder="$i('s3.browser.search')" :disabled="!selectedStorageKey || loading.files" @keyup.enter="reloadFiles" />
            <button :disabled="!selectedStorageKey || loading.files" @click="reloadFiles">{{ $i('s3.browser.searchAction') }}</button>
        </header>

        <p v-if="error" class="s3-error" role="alert">{{ error }}</p>
        <div class="s3-content">
            <div class="s3-list">
                <p v-if="loading.storages || loading.files" class="s3-state">{{ $i('s3.browser.loading') }}</p>
                <p v-else-if="!selectedStorageKey" class="s3-state">{{ $i('s3.browser.selectStorage') }}</p>
                <p v-else-if="items.length === 0" class="s3-state">{{ $i('s3.browser.empty') }}</p>
                <button v-for="item in items" :key="itemKey(item)" class="s3-item" :class="{ selected: selectedFile && itemKey(selectedFile) === itemKey(item) }" @click="selectItem(item)">
                    <span>{{ item.type === 'folder' ? 'DIR' : 'FILE' }}</span>{{ item.name }}
                </button>
                <button v-if="nextMarker" class="s3-load-more" :disabled="loading.more" @click="loadFiles(true)">{{ loading.more ? $i('s3.browser.loading') : $i('s3.browser.loadMore') }}</button>
            </div>
            <aside v-if="selectedFile" class="s3-details">
                <header><strong>{{ selectedFile.name }}</strong><button @click="selectedFile = null">x</button></header>
                <p>{{ $i('s3.browser.previewUrl') }}: <a v-if="previewUrl" :href="previewUrl" target="_blank" rel="noopener">{{ previewUrl }}</a><span v-else>-</span></p>
                <div class="s3-actions">
                    <button :disabled="selectedFile.type === 'folder' || loading.preview" @click="previewFile">{{ $i('s3.browser.preview') }}</button>
                    <button @click="deleteCandidate = selectedFile">{{ $i('s3.browser.delete') }}</button>
                </div>
                <pre>{{ JSON.stringify(selectedFile, null, 2) }}</pre>
            </aside>
        </div>

        <div v-if="deleteCandidate" class="s3-confirm" role="dialog" aria-modal="true">
            <p>{{ $i('s3.browser.confirmDelete', deleteCandidate.name) }}</p>
            <button @click="deleteCandidate = null">{{ $i('s3.browser.cancel') }}</button>
            <button class="danger" :disabled="loading.deleting" @click="deleteFile">{{ $i('s3.browser.delete') }}</button>
        </div>
    </section>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { getS3BrowserService } from '../service/magic-s3-browser.js'

const $i = inject('i18n.format')
const service = () => getS3BrowserService()
const storages = ref([])
const selectedStorageKey = ref('')
const path = ref('/')
const basePath = ref('/')
const basePathDraft = ref('/')
const search = ref('')
const items = ref([])
const nextMarker = ref(null)
const selectedFile = ref(null)
const previewUrl = ref('')
const deleteCandidate = ref(null)
const error = ref('')
const loading = reactive({ storages: false, files: false, more: false, preview: false, deleting: false })
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
const displayError = cause => { error.value = cause && cause.message ? cause.message : $i('s3.browser.loadFailed') }
const mapItems = result => [
    ...(result.dirList || result.directories || result.folders || []).map(file => ({ ...file, name: file.name, type: 'folder' })),
    ...(result.fileList || result.files || []).map(file => ({ ...file, name: file.filename || file.name, filename: file.filename || file.name, type: 'file' }))
]
const requestParams = marker => {
    const relativePath = path.value.startsWith(basePath.value) ? path.value.slice(basePath.value.length) : path.value
    const params = { basePath: basePath.value, path: relativePath }
    if (search.value.trim()) params.filenamePrefix = search.value.trim()
    if (marker) params.marker = marker
    return params
}
const loadFiles = async (more = false) => {
    if (!selectedStorageKey.value || (more && !nextMarker.value)) return
    error.value = ''
    loading[more ? 'more' : 'files'] = true
    try {
        const result = await service().listFiles(selectedStorageKey.value, requestParams(more ? nextMarker.value : null))
        const loadedItems = mapItems(result)
        items.value = more ? [...items.value, ...loadedItems] : loadedItems
        nextMarker.value = result.nextMarker || result.marker || null
    } catch (cause) {
        if (!more) items.value = []
        displayError(cause)
    } finally {
        loading[more ? 'more' : 'files'] = false
    }
}
const selectStorage = () => {
    const storage = storages.value.find(item => item.key === selectedStorageKey.value)
    basePath.value = normalizedPath(storage && storage.basePath)
    basePathDraft.value = basePath.value
    path.value = basePath.value
    selectedFile.value = null
    previewUrl.value = ''
    loadFiles()
}
const changePath = target => { path.value = normalizedPath(target); selectedFile.value = null; previewUrl.value = ''; loadFiles() }
const selectItem = item => item.type === 'folder' ? changePath(`${path.value}${item.name}`) : (selectedFile.value = item, previewUrl.value = '')
const saveBasePath = () => { basePath.value = normalizedPath(basePathDraft.value); basePathDraft.value = basePath.value; changePath(basePath.value) }
const reloadFiles = () => { selectedFile.value = null; previewUrl.value = ''; loadFiles() }
const previewFile = async () => {
    loading.preview = true
    error.value = ''
    try {
        const url = await service().generatePresignedUrl(selectedStorageKey.value, selectedFile.value)
        previewUrl.value = typeof url === 'string' ? url.trim().replace(/`/g, '') : ''
        if (!previewUrl.value) throw new Error($i('s3.browser.previewFailed'))
        window.open(previewUrl.value, '_blank', 'noopener')
    } catch (cause) {
        displayError(cause)
    } finally {
        loading.preview = false
    }
}
const deleteFile = async () => {
    loading.deleting = true
    error.value = ''
    try {
        await service().deleteFile(selectedStorageKey.value, deleteCandidate.value.url)
        selectedFile.value = null
        previewUrl.value = ''
        deleteCandidate.value = null
        await loadFiles()
    } catch (cause) {
        displayError(cause)
    } finally {
        loading.deleting = false
    }
}
const loadStorages = async () => {
    loading.storages = true
    error.value = ''
    try {
        storages.value = await service().listStorages()
        if (storages.value.length) {
            selectedStorageKey.value = storages.value[0].key
            selectStorage()
        }
    } catch (cause) {
        displayError(cause)
    } finally {
        loading.storages = false
    }
}

onMounted(loadStorages)
</script>

<style scoped>
.magic-s3-browser { min-height: 420px; height: 100%; display: flex; flex-direction: column; color: var(--magic-text-color, #333); background: var(--magic-bg, #fff); position: relative; }
.s3-toolbar, .s3-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding: 8px; border-bottom: 1px solid var(--magic-border-color, #ddd); }.s3-toolbar input, .s3-toolbar select, button { height: 28px; border: 1px solid var(--magic-border-color, #ccc); background: var(--magic-bg, #fff); color: inherit; padding: 0 8px; } button { cursor: pointer; } button:disabled { cursor: not-allowed; opacity: .55; }.s3-path { display: flex; align-items: center; gap: 2px; min-width: 0; }.s3-path button { border: 0; padding: 0 3px; height: auto; }.s3-base-path { display: flex; gap: 4px; align-items: center; }.s3-base-path input { width: 110px; }.s3-content { flex: 1; min-height: 0; display: flex; }.s3-list { flex: 1; overflow: auto; padding: 8px; }.s3-item { display: flex; gap: 8px; align-items: center; width: 100%; border: 0; text-align: left; }.s3-item:hover, .s3-item.selected { background: var(--magic-hover-bg, #eef5ff); }.s3-item span { width: 38px; color: #667; font-size: 11px; }.s3-state { padding: 18px; color: #667; }.s3-load-more { display: block; margin: 10px auto; }.s3-details { width: min(42%, 420px); overflow: auto; border-left: 1px solid var(--magic-border-color, #ddd); padding: 10px; }.s3-details header { display: flex; justify-content: space-between; }.s3-details pre { white-space: pre-wrap; overflow-wrap: anywhere; }.s3-error { margin: 0; padding: 8px; color: #b42318; }.s3-confirm { position: absolute; inset: auto 16px 16px auto; padding: 12px; border: 1px solid var(--magic-border-color, #ccc); background: var(--magic-bg, #fff); box-shadow: 0 3px 12px #0003; }.danger { color: #b42318; }
</style>
