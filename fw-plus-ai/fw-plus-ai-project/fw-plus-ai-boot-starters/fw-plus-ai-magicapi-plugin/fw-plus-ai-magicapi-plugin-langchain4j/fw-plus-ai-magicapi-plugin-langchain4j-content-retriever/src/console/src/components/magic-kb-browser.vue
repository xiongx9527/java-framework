<template>
  <div class="magic-kb-browser">
    <aside class="kb-side">
      <div class="kb-side-header">
        <strong>{{ $i('kb.dataset.title') }}</strong>
        <button @click="loadDatasets">{{ $i('kb.refresh') }}</button>
        <button @click="openDatasetForm()">{{ $i('kb.dataset.create') }}</button>
        <input ref="importInput" type="file" accept=".json" style="display:none" @change="onImportFile"/>
        <button @click="$refs.importInput.click()">{{ $i('kb.dataset.import') }}</button>
      </div>
      <div v-if="loading.datasets" class="kb-state">{{ $i('kb.loading') }}</div>
      <div v-else-if="!datasets.length" class="kb-state">{{ $i('kb.empty') }}</div>
      <button v-for="ds in datasets" :key="ds.id" class="kb-node" :class="{ selected: ds.id === selectedDatasetId }"
              @click="selectDataset(ds)">
        <span>{{ ds.name }}</span>
        <small>{{ ds.status === 'active' ? $i('kb.status.active') : $i('kb.status.disabled') }}</small>
      </button>
      <div v-if="error" class="kb-error">{{ error }}</div>
    </aside>

    <main class="kb-main">
      <div v-if="!selectedDataset" class="kb-state">{{ $i('kb.selectDataset') }}</div>
      <template v-else>
        <div class="kb-tabs">
          <button v-for="t in views" :key="t.id" class="kb-tab" :class="{ active: activeView === t.id }"
                  @click="activeView = t.id">{{ $i(t.label) }}</button>
          <span class="kb-tab-tip">{{ selectedDataset.name }}</span>
        </div>

        <!-- 数据集视图：配置明细 + 编辑/删除 + 引用检索器 -->
        <section v-if="activeView === 'dataset'" class="kb-view">
          <table class="kb-table">
            <tbody>
              <tr><th>{{ $i('kb.dataset.embeddingModel') }}</th><td>{{ selectedDataset.embeddingModelKey || '-' }}</td></tr>
              <tr><th>{{ $i('kb.dataset.embeddingModelActive') }}</th><td>{{ selectedDataset.embeddingModelActive || '-' }}</td></tr>
              <tr><th>{{ $i('kb.dataset.embeddingStore') }}</th><td>{{ selectedDataset.embeddingStoreKey || '-' }}</td></tr>
              <tr><th>{{ $i('kb.dataset.scoringModel') }}</th><td>{{ selectedDataset.scoringModelKey || '-' }}</td></tr>
              <tr><th>{{ $i('kb.dataset.segmentMode') }}</th><td>{{ selectedDataset.segmentMode || 'general' }}</td></tr>
              <tr><th>{{ $i('kb.dataset.segmentParams') }}</th><td>{{ selectedDataset.segmentMaxLength }} / {{ selectedDataset.segmentOverlap }}<template v-if="selectedDataset.segmentDelimiter"> / {{ selectedDataset.segmentDelimiter }}</template></td></tr>
              <tr><th>{{ $i('kb.dataset.topK') }}</th><td>{{ selectedDataset.retrievalTopK }} / {{ selectedDataset.retrievalScoreThreshold }}</td></tr>
              <tr v-if="selectedDataset.errorMsg"><th>{{ $i('kb.dataset.errorMsg') }}</th><td class="kb-error">{{ selectedDataset.errorMsg }}</td></tr>
              <tr><th>{{ $i('kb.dataset.retrievers') }}</th><td>
                <span v-if="!referencers.length">{{ $i('kb.empty') }}</span>
                <code v-for="r in referencers" :key="r.key" class="kb-tag">{{ r.name || r.key }}</code>
              </td></tr>
            </tbody>
          </table>
          <div class="kb-actions">
            <button @click="openDatasetForm(selectedDataset)">{{ $i('kb.edit') }}</button>
            <button @click="openFieldsModal">{{ $i('kb.dataset.metadataFields') }}</button>
            <button @click="exportDataset">{{ $i('kb.dataset.export') }}</button>
            <button @click="openPreview">{{ $i('kb.dataset.preview') }}</button>
            <button class="danger" @click="removeDataset">{{ $i('kb.delete') }}</button>
          </div>
        </section>

        <!-- 文档视图 -->
        <section v-else-if="activeView === 'document'" class="kb-view">
          <div class="kb-actions">
            <input ref="fileInput" type="file" multiple style="display:none" @change="onUpload"/>
            <button @click="$refs.fileInput.click()">{{ $i('kb.document.upload') }}</button>
            <button @click="openMoveForm" :disabled="!checkedDocuments.length">{{ $i('kb.document.move') }}</button>
            <button @click="loadDocuments">{{ $i('kb.refresh') }}</button>
            <button :disabled="!checkedDocuments.length" @click="batchDocuments('enable')">{{ $i('kb.document.enable') }}</button>
            <button :disabled="!checkedDocuments.length" @click="batchDocuments('disable')">{{ $i('kb.document.disable') }}</button>
            <button :disabled="!checkedDocuments.length" @click="batchDocuments('archive')">{{ $i('kb.document.archive') }}</button>
            <button :disabled="!checkedDocuments.length" @click="batchDocuments('unarchive')">{{ $i('kb.document.unarchive') }}</button>
            <button class="danger" :disabled="!checkedDocuments.length" @click="batchDocuments('delete')">{{ $i('kb.delete') }}</button>
          </div>
          <div v-if="dragOver" class="kb-dragover" @dragover.prevent @dragleave="dragOver=false" @drop.prevent="onDrop">{{ $i('kb.document.dropHere') }}</div>
          <div v-else-if="loading.documents" class="kb-state" @dragover.prevent="dragOver=true" @dragleave="dragOver=false" @drop.prevent="onDrop">{{ $i('kb.loading') }}</div>
          <div v-else-if="!documents.length" class="kb-state">{{ $i('kb.empty') }}</div>
          <table v-else class="kb-table">
            <thead>
              <tr>
                <th><input type="checkbox" :checked="allChecked" @change="toggleAll"/></th>
                <th>{{ $i('kb.common.name') }}</th>
                <th>{{ $i('kb.document.status') }}</th>
                <th>{{ $i('kb.document.enabled') }}</th>
                <th>{{ $i('kb.document.archived') }}</th>
                <th>{{ $i('kb.common.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="doc in documents" :key="doc.id">
                <td><input type="checkbox" :value="doc.id" v-model="checkedDocuments"/></td>
                <td>{{ doc.name }}</td>
                <td>
                  <span :class="{ 'kb-error': doc.indexingStatus === 'error' }">{{ doc.indexingStatus }}</span>
                  <small v-if="doc.indexingStatus === 'error'">{{ doc.errorMsg }}</small>
                </td>
                <td>{{ doc.enabled === '1' ? $i('kb.option.yes') : $i('kb.option.no') }}</td>
                <td>{{ doc.archived === '1' ? $i('kb.option.yes') : $i('kb.option.no') }}</td>
                <td>
                  <button @click="openTextForm(doc)">{{ $i('kb.document.editText') }}</button>
                  <button @click="reindexDocument(doc)">{{ $i('kb.document.reindex') }}</button>
                  <button @click="patchDocument(doc, 'enabled', doc.enabled !== '1')">{{ $i('kb.document.toggleEnabled') }}</button>
                  <button @click="patchDocument(doc, 'archived', doc.archived !== '1')">{{ $i('kb.document.toggleArchived') }}</button>
                  <button @click="openMetadataForm(doc)">{{ $i('kb.document.metadata') }}</button>
                  <button @click="downloadDocument(doc)">{{ $i('kb.document.download') }}</button>
                  <button class="danger" @click="removeDocument(doc)">{{ $i('kb.delete') }}</button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <!-- 分段视图 -->
        <section v-else-if="activeView === 'segment'" class="kb-view">
          <div class="kb-actions">
            <select v-model="segmentDocumentId">
              <option value="">{{ $i('kb.segment.selectDocument') }}</option>
              <option v-for="doc in documents" :key="doc.id" :value="doc.id">{{ doc.name }}</option>
            </select>
            <button :disabled="!segmentDocumentId" @click="loadSegments">{{ $i('kb.refresh') }}</button>
            <button :disabled="!segmentDocumentId" @click="openSegmentCreate">{{ $i('kb.segment.create') }}</button>
          </div>
          <div v-if="!segmentDocumentId" class="kb-state">{{ $i('kb.segment.selectDocumentTip') }}</div>
          <div v-else-if="loading.segments" class="kb-state">{{ $i('kb.loading') }}</div>
          <div v-else-if="!segments.length" class="kb-state">{{ $i('kb.empty') }}</div>
          <table v-else class="kb-table">
            <thead>
              <tr>
                <th>#</th>
                <th>{{ $i('kb.segment.content') }}</th>
                <th>{{ $i('kb.segment.keywords') }}</th>
                <th>{{ $i('kb.segment.tokens') }}</th>
                <th>{{ $i('kb.segment.parent') }}</th>
                <th>{{ $i('kb.segment.hits') }}</th>
                <th>{{ $i('kb.common.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="seg in segments" :key="seg.id">
                <td>{{ seg.position }}</td>
                <td class="kb-content" :title="seg.content">{{ seg.content }}</td>
                <td>{{ seg.keywords || '-' }}</td>
                <td>{{ seg.tokens }}</td>
                <td>{{ seg.parentId ? 'parent' : '-' }}</td>
                <td>{{ seg.hitCount }}</td>
                <td>
                  <button @click="openSegmentForm(seg)">{{ $i('kb.edit') }}</button>
                  <button @click="patchSegment(seg, seg.enabled !== '1')">{{ $i('kb.document.toggleEnabled') }}</button>
                  <button class="danger" @click="removeSegment(seg)">{{ $i('kb.delete') }}</button>
                </td>
              </tr>
            </tbody>
          </table>
          <div class="kb-actions">
            <span class="kb-tab-tip">{{ $i('kb.pagination.total') }}{{ documentsTotal }}</span>
            <button :disabled="documentsPage <= 1" @click="changeDocumentsPage(-1)">{{ $i('kb.pagination.prev') }}</button>
            <span>{{ documentsPage }}</span>
            <button :disabled="documentsPage * 20 >= documentsTotal" @click="changeDocumentsPage(1)">{{ $i('kb.pagination.next') }}</button>
          </div>
        </section>

        <!-- 召回测试视图 -->
        <section v-else class="kb-view">
          <div class="kb-actions">
            <input v-model="hitQuery" style="flex:1" :placeholder="$i('kb.hit.queryTip')" @keyup.enter="runHitTest"/>
            <button :disabled="!hitQuery" @click="runHitTest">{{ $i('kb.hit.run') }}</button>
          </div>
          <div class="kb-form-row">
            <label>{{ $i('kb.hit.documentId') }}</label>
            <input v-model="hitDocumentId" :placeholder="$i('kb.hit.documentIdTip')"/>
          </div>
          <div class="kb-form-row">
            <label>{{ $i('kb.hit.documentId') }}</label>
            <input v-model="hitDocumentId" :placeholder="$i('kb.hit.documentIdTip')"/>
          </div>
          <div class="kb-form-row">
            <label>{{ $i('kb.hit.metadataCondition') }}</label>
            <input v-model="hitMetadata" :placeholder="$i('kb.hit.metadataTip')"/>
          </div>
          <div v-if="!hits.length" class="kb-state">{{ $i('kb.empty') }}</div>
          <table v-else class="kb-table">
            <thead>
              <tr>
                <th>{{ $i('kb.hit.score') }}</th>
                <th>{{ $i('kb.segment.content') }}</th>
                <th>{{ $i('kb.document.name') }}</th>
                <th>{{ $i('kb.segment.parent') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="hit in hits" :key="hit.segmentId">
                <td>{{ hit.score == null ? '-' : hit.score.toFixed ? hit.score.toFixed(4) : hit.score }}</td>
                <td class="kb-content">{{ hit.content }}</td>
                <td>{{ hit.documentName || '-' }}</td>
                <td class="kb-content" :title="hit.parentContent || ''">{{ hit.parentContent ? hit.parentContent.slice(0, 80) : '-' }}</td>
              </tr>
            </tbody>
          </table>
        </section>
      </template>
    </main>

    <!-- 数据集表单 -->
    <div v-if="datasetForm" class="kb-modal-mask" @click.self="datasetForm = null">
      <div class="kb-modal">
        <h4>{{ datasetForm.id ? $i('kb.dataset.edit') : $i('kb.dataset.create') }}</h4>
        <div class="kb-form-row"><label>{{ $i('kb.common.name') }} *</label><input v-model="datasetForm.name"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.common.description') }}</label><input v-model="datasetForm.description"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.embeddingModel') }} *</label><input v-model="datasetForm.embeddingModelKey"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.embeddingStore') }} *</label><input v-model="datasetForm.embeddingStoreKey"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.scoringModel') }}</label><input v-model="datasetForm.scoringModelKey"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.chatModel') }}</label><input v-model="datasetForm.chatModelKey" :placeholder="$i('kb.dataset.chatModelTip')"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.segmentMode') }}</label>
          <select v-model="datasetForm.segmentMode">
            <option value="general">general</option>
            <option value="parent-child">parent-child</option>
            <option value="qa">qa</option>
          </select>
        </div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.segmentMaxLength') }}</label><input v-model="datasetForm.segmentMaxLength" type="number"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.segmentOverlap') }}</label><input v-model="datasetForm.segmentOverlap" type="number"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.segmentDelimiter') }}</label><input v-model="datasetForm.segmentDelimiter"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.topK') }}</label><input v-model="datasetForm.retrievalTopK" type="number"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.dataset.scoreThreshold') }}</label><input v-model="datasetForm.retrievalScoreThreshold" type="number" step="0.0001"/></div>
        <div class="kb-modal-actions">
          <button @click="saveDataset">{{ $i('kb.save') }}</button>
          <button @click="datasetForm = null">{{ $i('kb.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- 分段表单 -->
    <div v-if="segmentForm" class="kb-modal-mask" @click.self="segmentForm = null">
      <div class="kb-modal">
        <h4>{{ segmentFormMode === 'create' ? $i('kb.segment.create') : $i('kb.segment.edit') }}</h4>
        <div class="kb-form-row"><label>{{ $i('kb.segment.content') }}</label><textarea v-model="segmentForm.content" rows="5"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.segment.keywords') }}</label><input v-model="segmentForm.keywords"/></div>
        <div class="kb-form-row"><label>{{ $i('kb.segment.answer') }}</label><textarea v-model="segmentForm.answer" rows="2"/></div>
        <div class="kb-modal-actions">
          <button @click="saveSegment">{{ $i('kb.save') }}</button>
          <button @click="segmentForm = null">{{ $i('kb.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- 切分预览 -->
    <div v-if="previewItems" class="kb-modal-mask" @click.self="previewItems = null">
      <div class="kb-modal">
        <h4>{{ $i('kb.dataset.preview') }}</h4>
        <div v-if="!previewItems.length" class="kb-state">{{ $i('kb.empty') }}</div>
        <table v-else class="kb-table">
          <thead><tr><th>#</th><th>{{ $i('kb.segment.content') }}</th><th>{{ $i('kb.segment.tokens') }}</th><th>{{ $i('kb.segment.parent') }}</th></tr></thead>
          <tbody>
            <tr v-for="(item, index) in previewItems" :key="index">
              <td>{{ item.position }}</td>
              <td class="kb-content" :title="item.content">{{ item.content }}</td>
              <td>{{ item.tokens }}</td>
              <td>{{ item.parent ? $i('kb.option.yes') : '-' }}</td>
            </tr>
          </tbody>
        </table>
        <div class="kb-modal-actions"><button @click="previewItems = null">{{ $i('kb.cancel') }}</button></div>
      </div>
    </div>

    <!-- 文档迁移 -->
    <div v-if="moveForm" class="kb-modal-mask" @click.self="moveForm = null">
      <div class="kb-modal">
        <h4>{{ $i('kb.document.move') }}</h4>
        <div class="kb-form-row">
          <label>{{ $i('kb.dataset.title') }}</label>
          <select v-model="moveForm.targetDatasetId">
            <option v-for="ds in datasets" :key="ds.id" :value="ds.id" v-if="ds.id !== selectedDatasetId">{{ ds.name }}</option>
          </select>
        </div>
        <div class="kb-modal-actions">
          <button @click="doMove">{{ $i('kb.save') }}</button>
          <button @click="moveForm = null">{{ $i('kb.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- 元数据字段管理 -->
    <div v-if="fieldsModal" class="kb-modal-mask" @click.self="fieldsModal = null">
      <div class="kb-modal">
        <h4>{{ $i('kb.dataset.metadataFields') }}</h4>
        <table v-if="metadataFields.length" class="kb-table">
          <thead><tr><th>{{ $i('kb.common.name') }}</th><th>{{ $i('kb.fieldType') }}</th><th>{{ $i('kb.common.description') }}</th><th>{{ $i('kb.common.actions') }}</th></tr></thead>
          <tbody>
            <tr v-for="field in metadataFields" :key="field.id">
              <td>{{ field.name }}</td>
              <td>{{ field.type }}</td>
              <td>{{ field.description || '-' }}</td>
              <td><button class="danger" @click="removeField(field)">{{ $i('kb.delete') }}</button></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="kb-state">{{ $i('kb.empty') }}</div>
        <div class="kb-form-row" style="margin-top:8px">
          <label>{{ $i('kb.common.name') }}</label><input v-model="newField.name"/>
        </div>
        <div class="kb-form-row">
          <label>{{ $i('kb.fieldType') }}</label>
          <select v-model="newField.type"><option value="string">string</option><option value="number">number</option><option value="time">time</option></select>
        </div>
        <div class="kb-form-row">
          <label>{{ $i('kb.common.description') }}</label><input v-model="newField.description"/>
        </div>
        <div class="kb-modal-actions"><button @click="addField">{{ $i('kb.save') }}</button></div>
        <div class="kb-modal-actions"><button @click="fieldsModal = null">{{ $i('kb.cancel') }}</button></div>
      </div>
    </div>

    <!-- 编辑文档文本 -->
    <div v-if="textForm" class="kb-modal-mask" @click.self="textForm = null">
      <div class="kb-modal">
        <h4>{{ $i('kb.document.editText') }}</h4>
        <div class="kb-form-row"><label>{{ $i('kb.segment.content') }}</label><textarea v-model="textForm.text" rows="10"/></div>
        <div class="kb-modal-actions">
          <button @click="saveText">{{ $i('kb.save') }}</button>
          <button @click="textForm = null">{{ $i('kb.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- metadata 表单 -->
    <div v-if="metadataForm" class="kb-modal-mask" @click.self="metadataForm = null">
      <div class="kb-modal">
        <h4>{{ $i('kb.document.metadata') }}</h4>
        <div v-if="metadataFields.length" class="kb-form-row">
          <label>{{ $i('kb.dataset.metadataFields') }}</label>
          <code v-for="field in metadataFields" :key="field.id" class="kb-tag">{{ field.name }}({{ field.type }})</code>
        </div>
        <div class="kb-form-row"><label>{{ $i('kb.document.metadataJson') }}</label><textarea v-model="metadataForm.json" rows="6"/></div>
        <div class="kb-modal-actions">
          <button @click="saveMetadata">{{ $i('kb.save') }}</button>
          <button @click="metadataForm = null">{{ $i('kb.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { computed, inject, ref } from 'vue'
import { kbApi } from '../service/kb-service.js'

const $i = inject('i18n.format')
const modal = inject('modal')

const loading = ref({ datasets: false, documents: false, segments: false })
const error = ref('')
const datasets = ref([])
const selectedDatasetId = ref('')
const documents = ref([])
const documentsPage = ref(1)
const documentsTotal = ref(0)
const checkedDocuments = ref([])
const segments = ref([])
const segmentsPage = ref(1)
const segmentsTotal = ref(0)
const segmentDocumentId = ref('')
const segmentFormMode = ref('edit')
const hits = ref([])
const hitQuery = ref('')
const hitMetadata = ref('')
const hitDocumentId = ref('')
const hitDocumentId = ref('')
const dragOver = ref(false)
const previewItems = ref(null)
const moveForm = ref(null)
const textForm = ref(null)
const metadataFields = ref([])
const fieldsModal = ref(false)
const newField = ref({ name: '', type: 'string', description: '' })
const referencers = ref([])
const activeView = ref('dataset')
const datasetForm = ref(null)
const segmentForm = ref(null)
const metadataForm = ref(null)

const views = [
  { id: 'dataset', label: 'kb.view.dataset' },
  { id: 'document', label: 'kb.view.document' },
  { id: 'segment', label: 'kb.view.segment' },
  { id: 'hit', label: 'kb.view.hit' }
]

const selectedDataset = computed(() => datasets.value.find(d => d.id === selectedDatasetId.value) || null)
const allChecked = computed(() => documents.value.length > 0 && checkedDocuments.value.length === documents.value.length)

const fail = (e) => {
  error.value = (e && e.message) || String(e)
}

const loadDatasets = async () => {
  loading.value.datasets = true
  error.value = ''
  try {
    datasets.value = (await kbApi.listDatasets()) || []
  } catch (e) {
    fail(e)
  } finally {
    loading.value.datasets = false
  }
}

const selectDataset = async (ds) => {
  selectedDatasetId.value = ds.id
  activeView.value = 'dataset'
  hits.value = []
  await Promise.all([loadDocuments(), loadReferencers()])
}

const loadDocuments = async () => {
  if (!selectedDatasetId.value) return
  loading.value.documents = true
  try {
    const pageData = (await kbApi.listDocuments(selectedDatasetId.value, documentsPage.value, 20)) || {}
    documents.value = pageData.list || []
    documentsTotal.value = pageData.total || 0
    checkedDocuments.value = []
  } catch (e) {
    fail(e)
  } finally {
    loading.value.documents = false
  }
}

const loadReferencers = async () => {
  referencers.value = []
  try {
    referencers.value = (await kbApi.retrievers(selectedDatasetId.value)) || []
  } catch (e) {
    referencers.value = []
  }
}

const openDatasetForm = (ds) => {
  datasetForm.value = ds
    ? { ...ds }
    : { name: '', description: '', embeddingModelKey: '', embeddingStoreKey: '', scoringModelKey: '',
        segmentMode: 'general', segmentMaxLength: 1000, segmentOverlap: 200, segmentDelimiter: '',
        retrievalTopK: 4, retrievalScoreThreshold: 0 }
}

const saveDataset = async () => {
  try {
    if (datasetForm.value.id) {
      await kbApi.updateDataset(datasetForm.value.id, datasetForm.value)
    } else {
      await kbApi.createDataset(datasetForm.value)
    }
    datasetForm.value = null
    await loadDatasets()
  } catch (e) {
    modal && modal.alert ? modal.alert(e.message) : fail(e)
  }
}

const removeDataset = async () => {
  if (!selectedDataset.value) return
  if (!window.confirm($i('kb.dataset.deleteConfirm'))) return
  try {
    await kbApi.deleteDataset(selectedDataset.value.id)
    selectedDatasetId.value = ''
    documents.value = []
    segments.value = []
    await loadDatasets()
  } catch (e) {
    fail(e)
  }
}

const onUpload = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file || !selectedDatasetId.value) return
  try {
    await kbApi.upload(selectedDatasetId.value, file)
    await loadDocuments()
  } catch (e) {
    fail(e)
  }
}

const reindexDocument = async (doc) => {
  try {
    await kbApi.reindex(doc.id)
    await loadDocuments()
  } catch (e) {
    fail(e)
  }
}

const patchDocument = async (doc, field, value) => {
  try {
    await kbApi.patchDocument(doc.id, field, value)
    await loadDocuments()
  } catch (e) {
    fail(e)
  }
}

const batchDocuments = async (action) => {
  if (!checkedDocuments.value.length) return
  try {
    await kbApi.batchDocuments(selectedDatasetId.value, [...checkedDocuments.value], action)
    await loadDocuments()
  } catch (e) {
    fail(e)
  }
}

const removeDocument = async (doc) => {
  if (!window.confirm($i('kb.document.deleteConfirm'))) return
  try {
    await kbApi.deleteDocument(doc.id)
    await loadDocuments()
  } catch (e) {
    fail(e)
  }
}

const openMetadataForm = (doc) => {
  let json = '{}'
  try {
    json = doc.metadata ? JSON.stringify(JSON.parse(doc.metadata), null, 2) : '{}'
  } catch (e) {
    json = doc.metadata || '{}'
  }
  metadataForm.value = { id: doc.id, json }
}

const saveMetadata = async () => {
  try {
    const parsed = JSON.parse(metadataForm.value.json || '{}')
    await kbApi.setMetadata(metadataForm.value.id, parsed)
    metadataForm.value = null
    await loadDocuments()
  } catch (e) {
    fail(e)
  }
}

const downloadDocument = (doc) => kbApi.download(doc.id, doc.name)

const loadSegments = async () => {
  if (!segmentDocumentId.value) return
  loading.value.segments = true
  try {
    const pageData = (await kbApi.listSegments(segmentDocumentId.value, segmentsPage.value, 20)) || {}
    segments.value = pageData.list || []
    segmentsTotal.value = pageData.total || 0
  } catch (e) {
    fail(e)
  } finally {
    loading.value.segments = false
  }
}

const openSegmentForm = (seg) => {
  segmentForm.value = { id: seg.id, content: seg.content, keywords: seg.keywords, answer: seg.answer }
}

const patchSegment = async (seg, value) => {
  try {
    await kbApi.patchSegment(seg.id, value)
    await loadSegments()
  } catch (e) {
    fail(e)
  }
}

const removeSegment = async (seg) => {
  try {
    await kbApi.deleteSegment(seg.id)
    await loadSegments()
  } catch (e) {
    fail(e)
  }
}

const runHitTest = async () => {
  try {
    let metadataCondition = null
    if (hitMetadata.value && hitMetadata.value.trim()) {
      metadataCondition = JSON.parse(hitMetadata.value)
    }
    hits.value = (await kbApi.hitTest(selectedDatasetId.value, {
      query: hitQuery.value, metadataCondition, documentId: hitDocumentId.value || null
    })) || []
  } catch (e) {
    fail(e)
  }
}

// ---------------- 批量/拖拽上传 ----------------

const uploadFiles = async (files) => {
  if (!files || !files.length || !selectedDatasetId.value) return
  try {
    for (const file of files) {
      await kbApi.upload(selectedDatasetId.value, file)
    }
    await loadDocuments()
  } catch (e) {
    fail(e)
  }
}

const onDrop = (event) => {
  dragOver.value = false
  if (event.dataTransfer && event.dataTransfer.files) {
    uploadFiles(event.dataTransfer.files)
  }
}

// ---------------- 导入/导出/迁移/切分预览 ----------------

const onImportFile = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  try {
    const text = await file.text()
    const datasetId = await kbApi.importDataset(text)
    modal && modal.alert ? modal.alert($i('kb.dataset.importDone') + datasetId) : null
    await loadDatasets()
  } catch (e) {
    fail(e)
  }
}

const exportDataset = () => {
  if (!selectedDataset.value) return
  kbApi.exportDataset(selectedDataset.value.id, selectedDataset.value.name)
}

const openPreview = async () => {
  const ds = selectedDataset.value
  if (!ds) return
  try {
    previewItems.value = (await kbApi.previewSplit({
      text: 'This is a sample text for split preview. '.repeat(10),
      segmentMode: ds.segmentMode,
      segmentMaxLength: ds.segmentMaxLength,
      segmentOverlap: ds.segmentOverlap,
      segmentDelimiter: ds.segmentDelimiter
    })) || []
  } catch (e) {
    fail(e)
  }
}

const openMoveForm = () => {
  if (!checkedDocuments.value.length) return
  moveForm.value = { targetDatasetId: '' }
}

const doMove = async () => {
  if (!moveForm.value.targetDatasetId) return
  try {
    for (const id of checkedDocuments.value) {
      await kbApi.moveDocument(id, moveForm.value.targetDatasetId)
    }
    moveForm.value = null
    await loadDocuments()
  } catch (e) {
    fail(e)
  }
}

const changeDocumentsPage = async (delta) => {
  const next = documentsPage.value + delta
  if (next < 1 || (documentsTotal.value && (next - 1) * 20 >= documentsTotal.value)) return
  documentsPage.value = next
  await loadDocuments()
}

const changeSegmentsPage = async (delta) => {
  const next = segmentsPage.value + delta
  if (next < 1 || (segmentsTotal.value && (next - 1) * 20 >= segmentsTotal.value)) return
  segmentsPage.value = next
  await loadSegments()
}

// 新增分段
const openSegmentCreate = () => {
  segmentFormMode.value = 'create'
  segmentForm.value = { documentId: segmentDocumentId.value, content: '', keywords: '', answer: '' }
}

const saveSegment = async () => {
  try {
    if (segmentFormMode.value === 'create') {
      await kbApi.createSegment(segmentForm.value.documentId, {
        content: segmentForm.value.content,
        keywords: segmentForm.value.keywords,
        answer: segmentForm.value.answer
      })
    } else {
      await kbApi.updateSegment(segmentForm.value.id, {
        content: segmentForm.value.content,
        keywords: segmentForm.value.keywords,
        answer: segmentForm.value.answer
      })
    }
    segmentForm.value = null
    await loadSegments()
  } catch (e) {
    fail(e)
  }
}

// 编辑文档文本（create-by-text）
const openTextForm = async (doc) => {
  let text = ''
  try {
    const bytes = doc.filePath ? await fetch('/kb/document/' + doc.id + '/download').then(r => r.arrayBuffer()) : null
    if (bytes) text = new TextDecoder().decode(bytes)
  } catch (e) {
    text = ''
  }
  textForm.value = { id: doc.id, text }
}

const saveText = async () => {
  try {
    await kbApi.updateText(textForm.value.id, textForm.value.text)
    textForm.value = null
    await loadDocuments()
  } catch (e) {
    fail(e)
  }
}

// ---------------- 元数据字段管理 ----------------

const openFieldsModal = async () => {
  fieldsModal.value = true
  try {
    metadataFields.value = (await kbApi.metadataFields(selectedDatasetId.value)) || []
  } catch (e) {
    fail(e)
  }
}

const addField = async () => {
  try {
    await kbApi.createMetadataField(selectedDatasetId.value, newField.value)
    newField.value = { name: '', type: 'string', description: '' }
    metadataFields.value = (await kbApi.metadataFields(selectedDatasetId.value)) || []
  } catch (e) {
    fail(e)
  }
}

const removeField = async (field) => {
  try {
    await kbApi.deleteMetadataField(field.id)
    metadataFields.value = (await kbApi.metadataFields(selectedDatasetId.value)) || []
  } catch (e) {
    fail(e)
  }
}

const toggleAll = () => {
  checkedDocuments.value = allChecked.value ? [] : documents.value.map(d => d.id)
}

loadDatasets()
</script>
<style scoped>
.magic-kb-browser { display: flex; height: 100%; background: var(--magic-background-color, #fff); color: var(--magic-text-color, #333); }
.kb-side { width: 240px; border-right: 1px solid var(--magic-border-color, #eee); overflow: auto; padding: 8px; }
.kb-side-header { display: flex; align-items: center; gap: 6px; margin-bottom: 8px; }
.kb-side-header strong { flex: 1; }
.kb-node { display: flex; justify-content: space-between; width: 100%; text-align: left; padding: 6px 8px; border: none; background: transparent; cursor: pointer; border-radius: 4px; }
.kb-node.selected, .kb-node:hover { background: var(--magic-hover-color, #f5f7fa); }
.kb-main { flex: 1; display: flex; flex-direction: column; overflow: auto; padding: 10px; }
.kb-tabs { display: flex; gap: 6px; align-items: center; border-bottom: 1px solid var(--magic-border-color, #eee); padding-bottom: 6px; }
.kb-tab { border: none; background: transparent; cursor: pointer; padding: 4px 10px; border-radius: 4px; }
.kb-tab.active { background: var(--magic-primary-color, #409eff); color: #fff; }
.kb-tab-tip { margin-left: auto; color: var(--magic-text-sub-color, #909399); font-size: 12px; }
.kb-view { display: flex; flex-direction: column; gap: 8px; overflow: auto; }
.kb-actions { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }
.kb-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.kb-table th, .kb-table td { border: 1px solid var(--magic-border-color, #eee); padding: 5px 8px; text-align: left; }
.kb-table thead th { background: var(--magic-hover-color, #f5f7fa); }
.kb-content { max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.kb-tag { background: var(--magic-hover-color, #f5f7fa); border-radius: 3px; padding: 1px 5px; margin-right: 4px; }
.kb-state { color: var(--magic-text-sub-color, #909399); padding: 12px 0; text-align: center; }
.kb-error { color: #f56c6c; }
.kb-node button, .kb-actions button, .kb-side-header button { cursor: pointer; }
.danger { color: #f56c6c; }
.kb-modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: flex; align-items: center; justify-content: center; z-index: 999; }
.kb-modal { background: var(--magic-background-color, #fff); color: var(--magic-text-color, #333); border-radius: 6px; padding: 16px; width: 480px; max-height: 80vh; overflow: auto; }
.kb-form-row { display: flex; align-items: flex-start; margin-bottom: 8px; }
.kb-form-row label { width: 150px; text-align: right; padding-right: 8px; padding-top: 4px; font-size: 12px; }
.kb-form-row input, .kb-form-row select, .kb-form-row textarea { flex: 1; box-sizing: border-box; padding: 4px 6px; border: 1px solid var(--magic-border-color, #ddd); border-radius: 4px; background: transparent; color: inherit; }
.kb-modal-actions { display: flex; justify-content: flex-end; gap: 8px; }
.kb-dragover { border: 2px dashed var(--magic-primary-color, #409eff); border-radius: 6px; padding: 20px; text-align: center; color: var(--magic-primary-color, #409eff); }
</style>
