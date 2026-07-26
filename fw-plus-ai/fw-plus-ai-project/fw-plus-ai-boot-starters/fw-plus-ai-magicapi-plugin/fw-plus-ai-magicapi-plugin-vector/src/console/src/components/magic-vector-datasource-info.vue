<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('vector.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('vector.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('vector.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('vector.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('vector.datasource.form.provider') }}</label>
      <magic-select v-model:value="info.provider" :placeholder="$i('vector.datasource.form.placeholder.provider')" inputable :options="providerOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('vector.datasource.form.dimension') }}</label>
      <magic-input v-model:value="info.dimension" :placeholder="$i('vector.datasource.form.placeholder.dimension')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('vector.datasource.form.metadataKeys') }}</label>
      <magic-input v-model:value="metadataKeysText" :placeholder="$i('vector.datasource.form.placeholder.metadataKeys')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('vector.datasource.form.properties') }}</label>
      <magic-monaco-editor v-model:value="propertiesText" :placeholder="$i('vector.datasource.form.placeholder.properties')" language="json" style="height:220px" />
    </div>
  </div>
</template>

<script setup>
import {inject, onMounted, ref, watch} from 'vue'

const $i = inject('i18n.format')

const props = defineProps({
  info: {
    type: Object,
    default: () => ({ properties: {} })
  }
})

const providerOptions = [
  { text: 'in-memory', value: 'in-memory' },
  { text: 'milvus', value: 'milvus' },
  { text: 'qdrant', value: 'qdrant' },
  { text: 'weaviate', value: 'weaviate' },
  { text: 'redis', value: 'redis' },
  { text: 'elasticsearch', value: 'elasticsearch' },
  { text: 'azure-ai-search', value: 'azure-ai-search' },
  { text: 'azure-cosmos-mongo-vcore', value: 'azure-cosmos-mongo-vcore' },
  { text: 'azure-cosmos-nosql', value: 'azure-cosmos-nosql' },
  { text: 'cassandra', value: 'cassandra' },
  { text: 'astra-db', value: 'astra-db' },
  { text: 'chroma', value: 'chroma' },
  { text: 'couchbase', value: 'couchbase' },
  { text: 'mongodb-atlas', value: 'mongodb-atlas' },
  { text: 'opensearch', value: 'opensearch' },
  { text: 'oracle', value: 'oracle' },
  { text: 'pgvector', value: 'pgvector' },
  { text: 'pinecone', value: 'pinecone' },
  { text: 'tablestore', value: 'tablestore' },
  { text: 'vearch', value: 'vearch' },
  { text: 'vespa', value: 'vespa' },
  { text: 'infinispan', value: 'infinispan' },
  { text: 'neo4j', value: 'neo4j' }
]

const propertiesText = ref('{}')
const metadataKeysText = ref('')

const formatJson = (value) => {
  if (!value) {
    return '{}'
  }
  if (typeof value === 'string') {
    return value
  }
  return JSON.stringify(value, null, 2)
}

const syncFromInfo = () => {
  if (!props.info.propertiesText) {
    props.info.propertiesText = formatJson(props.info.properties)
  }
  propertiesText.value = props.info.propertiesText
  if (!props.info.metadataKeysText) {
    props.info.metadataKeysText = Array.isArray(props.info.metadataKeys) ? props.info.metadataKeys.join(',') : ''
  }
  metadataKeysText.value = props.info.metadataKeysText
}

onMounted(syncFromInfo)

watch(propertiesText, (value) => {
  props.info.propertiesText = value
})

watch(metadataKeysText, (value) => {
  props.info.metadataKeysText = value
})
</script>
