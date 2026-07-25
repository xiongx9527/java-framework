<template>
  <div class="magic-db-browser">
    <aside class="db-tree">
      <div class="db-tree-header"><strong>{{ $i('db.datasource') }}</strong><button @click="loadDataSources">{{ $i('db.refresh') }}</button></div>
      <div v-if="loading.sources" class="db-state">{{ $i('db.loading') }}</div>
      <div v-else-if="sources.length === 0" class="db-state">{{ $i('db.empty') }}</div>
      <template v-else v-for="source in sources" :key="source.key">
        <button class="tree-node source" :class="{ selected: source.key === selectedSource }" @click="selectSource(source)">{{ source.name || source.key }}</button>
        <template v-if="source.key === selectedSource || source.key === loadingSourceKey">
          <div v-if="loading.namespaces && source.key === loadingSourceKey" class="db-state">{{ $i('db.loading') }}</div>
          <div v-else-if="source.key === selectedSource && namespaces.length === 0" class="db-state">{{ $i('db.empty') }}</div>
          <template v-else-if="source.key === selectedSource">
            <div v-for="namespace in namespaces" :key="namespaceKey(namespace)" class="namespace">
              <button class="tree-node namespace-name" @click="loadNamespace(namespace)">{{ displayNamespace(namespace) }}</button>
              <div v-if="loading.objects && namespaceKey(namespace) === loadingNamespaceKey" class="db-state">{{ $i('db.loading') }}</div>
              <div v-else-if="activeNamespace && namespaceKey(namespace) === namespaceKey(activeNamespace) && (objectGroups.every(group => group.items.length === 0) || !hasFilteredObjects)" class="db-state">{{ $i('db.empty') }}</div>
              <template v-else-if="activeNamespace && namespaceKey(namespace) === namespaceKey(activeNamespace)">
                <template v-for="group in objectGroups" :key="group.type">
                  <div v-if="filteredItems(group.items).length" class="group-label">{{ $i(group.label) }}</div>
                  <button v-for="item in filteredItems(group.items)" :key="itemKey(item)" class="tree-node object" :class="{ selected: selectedObject && itemKey(item) === itemKey(selectedObject) }" @click="selectObject(item)">{{ item.name }}</button>
                </template>
              </template>
            </div>
          </template>
        </template>
      </template>
      <div v-if="error" class="db-error">{{ error }}</div>
    </aside>
    <main class="db-detail">
      <div class="db-toolbar">
        <input v-model="objectFilter" :placeholder="$i('db.filter')" />
        <button :disabled="!selectedSource" @click="loadDataSources">{{ $i('db.refresh') }}</button>
      </div>
      <div v-if="loading.detail" class="db-state">{{ $i('db.loading') }}</div>
      <div v-else-if="!selectedObject" class="db-state">{{ $i('db.empty') }}</div>
      <template v-else>
        <header class="object-title"><strong>{{ selectedObject.name }}</strong><span>{{ selectedObject.type }}</span><small>{{ selectedObject.remarks }}</small></header>
        <section v-if="isPreviewable">
          <h4>{{ $i('db.columns') }}</h4>
          <table class="metadata-table"><thead><tr><th>{{ $i('db.name') }}</th><th>{{ $i('db.type') }}</th><th>{{ $i('db.nullable') }}</th><th>{{ $i('db.defaultValue') }}</th><th>{{ $i('db.remarks') }}</th></tr></thead>
            <tbody><tr v-for="column in detail.columns || []" :key="column.name"><td>{{ column.name }} <span v-if="isPrimaryKey(column.name)">*</span></td><td>{{ column.typeName }}{{ column.size ? `(${column.size})` : '' }}</td><td>{{ column.nullable === 1 ? 'Y' : 'N' }}</td><td>{{ column.defaultValue }}</td><td>{{ column.remarks }}</td></tr></tbody></table>
          <h4>{{ $i('db.indexes') }}</h4>
          <table class="metadata-table"><thead><tr><th>{{ $i('db.name') }}</th><th>{{ $i('db.columns') }}</th><th>{{ $i('db.direction') }}</th><th>{{ $i('db.type') }}</th></tr></thead>
            <tbody><tr v-for="index in detail.indexes || []" :key="`${index.name}-${index.columnName}`"><td>{{ index.name }}</td><td>{{ index.columnName }}</td><td>{{ index.ascOrDesc }}</td><td>{{ index.nonUnique ? 'NON_UNIQUE' : 'UNIQUE' }}</td></tr></tbody></table>
          <div class="preview-heading"><h4>{{ $i('db.preview') }}</h4><label>{{ $i('db.sort') }}<select v-model="previewDraft.sortColumn"><option value="">-</option><option v-for="column in detail.columns || []" :key="column.name" :value="column.name">{{ column.name }}</option></select></label><select v-model="previewDraft.sortDirection"><option value="ASC">ASC</option><option value="DESC">DESC</option></select><select v-model="previewDraft.filterColumn"><option value="">{{ $i('db.filter') }}</option><option v-for="column in detail.columns || []" :key="column.name" :value="column.name">{{ column.name }}</option></select><input v-model="previewDraft.filterValue" :placeholder="$i('db.value')" /><button @click="applyPreview">{{ $i('db.apply') }}</button><button @click="clearFilter">{{ $i('db.clear') }}</button></div>
          <div v-if="loading.preview" class="db-state">{{ $i('db.loading') }}</div>
          <table v-else class="preview-table"><thead><tr><th v-for="column in preview.columns || []" :key="column">{{ column }}</th></tr></thead><tbody><tr v-for="(row, index) in preview.rows || []" :key="index"><td v-for="column in preview.columns || []" :key="column">{{ row[column] }}</td></tr></tbody></table>
          <div class="pagination"><button :disabled="previewState.page <= 1" @click="changePage(-1)">{{ $i('db.previous') }}</button><span>{{ previewState.page }}</span><button :disabled="!preview.hasMore" @click="changePage(1)">{{ $i('db.next') }}</button></div>
        </section>
        <section v-else><h4>{{ $i('db.objects') }}</h4><dl><dt>{{ $i('db.name') }}</dt><dd>{{ selectedObject.name }}</dd><dt>{{ $i('db.type') }}</dt><dd>{{ selectedObject.type }}</dd><dt>{{ $i('db.remarks') }}</dt><dd>{{ selectedObject.remarks || '-' }}</dd></dl></section>
      </template>
    </main>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { getDbBrowserService } from '../service/magic-db-browser.js'

const $i = inject('i18n.format')
const sources = ref([])
const namespaces = ref([])
const objects = ref([])
const routines = ref([])
const selectedSource = ref('')
const activeNamespace = ref(null)
const selectedObject = ref(null)
const detail = ref({})
const preview = ref({ columns: [], rows: [], hasMore: false })
const objectFilter = ref('')
const error = ref('')
const loadingSourceKey = ref('')
const loadingNamespaceKey = ref('')
const sourceRequestSeq = ref(0)
const dataSourcesRequestSeq = ref(0)
const namespaceRequestSeq = ref(0)
const objectRequestSeq = ref(0)
const previewRequestSeq = ref(0)
const loading = reactive({ sources: false, namespaces: false, objects: false, detail: false, preview: false })
const previewState = reactive({ page: 1, pageSize: 20, sortColumn: '', sortDirection: 'ASC', filterColumn: '', filterValue: '' })
const previewDraft = reactive({ sortColumn: '', sortDirection: 'ASC', filterColumn: '', filterValue: '' })
const service = () => getDbBrowserService()
const namespaceKey = namespace => `${namespace.catalog || ''}:${namespace.schema || ''}`
const displayNamespace = namespace => namespace.schema || namespace.catalog || '(default)'
const itemKey = item => `${item.catalog || ''}:${item.schema || ''}:${item.type}:${item.name}`
const objectGroups = computed(() => [
  { type: 'TABLE', label: 'db.table', items: objects.value.filter(item => item.type === 'TABLE') },
  { type: 'VIEW', label: 'db.view', items: objects.value.filter(item => item.type === 'VIEW') },
  { type: 'PROCEDURE', label: 'db.procedure', items: routines.value.filter(item => item.type === 'PROCEDURE') },
  { type: 'FUNCTION', label: 'db.function', items: routines.value.filter(item => item.type === 'FUNCTION') }
])
const isPreviewable = computed(() => selectedObject.value && ['TABLE', 'VIEW'].includes(selectedObject.value.type))
const filteredItems = items => {
  const query = objectFilter.value.trim().toLowerCase()
  return query ? items.filter(item => item.name.toLowerCase().includes(query)) : items
}
const hasFilteredObjects = computed(() => objectGroups.value.some(group => filteredItems(group.items).length > 0))
const context = namespace => ({ key: selectedSource.value, catalog: namespace.catalog, schema: namespace.schema })
const reportError = cause => { error.value = cause && cause.message ? cause.message : $i('db.loadFailed') }
const syncPreviewDraft = state => Object.assign(previewDraft, {
  sortColumn: state.sortColumn,
  sortDirection: state.sortDirection,
  filterColumn: state.filterColumn,
  filterValue: state.filterValue
})
const restorePreviewDraft = () => syncPreviewDraft(previewState)

const loadDataSources = async () => {
  const requestSeq = ++dataSourcesRequestSeq.value
  loading.sources = true
  error.value = ''
  try {
    const result = await service().listDataSources()
    if (requestSeq !== dataSourcesRequestSeq.value) return
    sources.value = Array.isArray(result) ? result : []
    if (!sources.value.some(source => source.key === selectedSource.value)) {
      selectedSource.value = ''
      namespaces.value = []
      activeNamespace.value = null
      selectedObject.value = null
    }
  } catch (cause) {
    if (requestSeq === dataSourcesRequestSeq.value) reportError(cause)
  } finally {
    if (requestSeq === dataSourcesRequestSeq.value) loading.sources = false
  }
}
const selectSource = async source => {
  if (source.key === selectedSource.value && !loading.namespaces) return
  const requestSeq = ++sourceRequestSeq.value
  ++namespaceRequestSeq.value
  ++objectRequestSeq.value
  ++previewRequestSeq.value
  error.value = ''
  loadingSourceKey.value = source.key
  loading.namespaces = true
  loading.objects = false
  loading.detail = false
  loading.preview = false
  try {
    const loadedNamespaces = await service().listNamespaces(source.key) || []
    if (requestSeq !== sourceRequestSeq.value) return
    selectedSource.value = source.key
    namespaces.value = loadedNamespaces
    activeNamespace.value = null
    objects.value = []
    routines.value = []
    selectedObject.value = null
    detail.value = {}
    preview.value = { columns: [], rows: [], hasMore: false }
  } catch (cause) {
    if (requestSeq === sourceRequestSeq.value) reportError(cause)
  } finally {
    if (requestSeq === sourceRequestSeq.value) {
      loading.namespaces = false
      loadingSourceKey.value = ''
    }
  }
}
const loadNamespace = async namespace => {
  const requestSeq = ++namespaceRequestSeq.value
  ++objectRequestSeq.value
  ++previewRequestSeq.value
  error.value = ''
  const requestContext = context(namespace)
  loadingNamespaceKey.value = namespaceKey(namespace)
  loading.objects = true
  loading.detail = false
  loading.preview = false
  try {
    const [loadedObjects, loadedRoutines] = await Promise.all([
      service().listObjects(requestContext), service().listRoutines(requestContext)
    ])
    if (requestSeq !== namespaceRequestSeq.value) return
    activeNamespace.value = namespace
    objects.value = loadedObjects || []
    routines.value = loadedRoutines || []
    selectedObject.value = null
    detail.value = {}
    preview.value = { columns: [], rows: [], hasMore: false }
  } catch (cause) {
    if (requestSeq === namespaceRequestSeq.value) reportError(cause)
  } finally {
    if (requestSeq === namespaceRequestSeq.value) {
      loading.objects = false
      loadingNamespaceKey.value = ''
    }
  }
}
const selectObject = async item => {
  const requestSeq = ++objectRequestSeq.value
  const previewRequestSeqForObject = ++previewRequestSeq.value
  error.value = ''
  loading.detail = true
  loading.preview = true
  try {
    const loadedDetail = await service().getObjectDetail({ ...context(item), objectName: item.name, objectType: item.type }) || {}
    const initialPreviewState = { page: 1, pageSize: 20, sortColumn: '', sortDirection: 'ASC', filterColumn: '', filterValue: '' }
    const loadedPreview = ['TABLE', 'VIEW'].includes(item.type)
      ? await loadPreviewFor(item, initialPreviewState)
      : { columns: [], rows: [], hasMore: false }
    if (requestSeq !== objectRequestSeq.value) return
    selectedObject.value = item
    detail.value = loadedDetail
    if (previewRequestSeqForObject === previewRequestSeq.value) {
      preview.value = loadedPreview
      Object.assign(previewState, initialPreviewState)
      syncPreviewDraft(initialPreviewState)
    }
  } catch (cause) {
    if (requestSeq === objectRequestSeq.value) reportError(cause)
  } finally {
    if (requestSeq === objectRequestSeq.value) {
      loading.detail = false
    }
    if (previewRequestSeqForObject === previewRequestSeq.value) {
      loading.preview = false
    }
  }
}
const loadPreviewFor = async (item, state) => {
  const filters = state.filterColumn && state.filterValue !== '' ? [{ column: state.filterColumn, operator: 'like', value: `%${state.filterValue}%` }] : []
  return await service().preview({ ...context(item), objectName: item.name, objectType: item.type, ...state, filters }) || { columns: [], rows: [], hasMore: false }
}
const loadPreview = async (state = previewState) => {
  if (!isPreviewable.value) return
  const requestSeq = ++previewRequestSeq.value
  const item = selectedObject.value
  error.value = ''
  loading.preview = true
  try {
    const loadedPreview = await loadPreviewFor(item, state)
    if (requestSeq !== previewRequestSeq.value) return
    preview.value = loadedPreview
    Object.assign(previewState, state)
    syncPreviewDraft(state)
  } catch (cause) {
    if (requestSeq !== previewRequestSeq.value) return
    restorePreviewDraft()
    reportError(cause)
  } finally {
    if (requestSeq === previewRequestSeq.value) {
      loading.preview = false
    }
  }
}
const applyPreview = () => loadPreview({ ...previewState, ...previewDraft, page: 1 })
const clearFilter = () => {
  previewDraft.filterColumn = ''
  previewDraft.filterValue = ''
  applyPreview()
}
const changePage = offset => { const targetPage = previewState.page + offset; loadPreview({ ...previewState, page: targetPage }) }
const isPrimaryKey = name => (detail.value.primaryKeys || []).includes(name)

onMounted(loadDataSources)
</script>

<style scoped>
.magic-db-browser { display: flex; min-height: 420px; height: 100%; background: var(--magic-bg, #fff); color: var(--magic-text-color, #333); }
.db-tree { width: 270px; min-width: 220px; overflow: auto; border-right: 1px solid var(--magic-border-color, #ddd); padding: 8px; }
.db-detail { flex: 1; min-width: 0; overflow: auto; padding: 12px; }
.db-tree-header, .db-toolbar, .preview-heading, .pagination { display: flex; align-items: center; gap: 8px; }
.db-tree-header { justify-content: space-between; margin-bottom: 6px; }
button, input, select { border: 1px solid var(--magic-border-color, #ccc); background: var(--magic-bg, #fff); color: inherit; height: 28px; padding: 0 8px; }
button { cursor: pointer; } button:disabled { cursor: not-allowed; opacity: .55; }
.tree-node { display: block; width: 100%; border: 0; text-align: left; background: transparent; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.tree-node:hover, .tree-node.selected { background: var(--magic-hover-bg, #eef5ff); }
.source { font-weight: 600; }.namespace { margin-left: 10px; }.namespace-name { font-weight: 500; }.object { margin-left: 18px; width: calc(100% - 18px); }.group-label { margin: 5px 0 2px 18px; font-size: 12px; color: #778; }
.db-state, .db-error { padding: 16px; color: #778; }.db-error { color: #b42318; }.object-title { display: flex; gap: 10px; align-items: baseline; margin: 8px 0 14px; }.object-title small { color: #778; }
.metadata-table, .preview-table { width: 100%; border-collapse: collapse; margin-bottom: 14px; }.metadata-table th, .metadata-table td, .preview-table th, .preview-table td { border: 1px solid var(--magic-border-color, #ddd); padding: 6px; text-align: left; white-space: nowrap; }.preview-table { display: table; overflow: auto; }.preview-heading { flex-wrap: wrap; }.preview-heading h4 { margin-right: auto; } dl { display: grid; grid-template-columns: 120px 1fr; gap: 6px; } dt { font-weight: 600; } dd { margin: 0; }.pagination { justify-content: flex-end; }
</style>
