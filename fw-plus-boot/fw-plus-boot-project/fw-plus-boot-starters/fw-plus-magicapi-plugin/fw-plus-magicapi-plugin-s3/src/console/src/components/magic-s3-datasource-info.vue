<template>
  <div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('s3.datasource.form.placeholder.name')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('s3.datasource.form.placeholder.key')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.platform') }}</label>
      <magic-select v-model:value="info.platform" :placeholder="$i('s3.datasource.form.placeholder.platform')" :options="platformOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.accessKey') }}</label>
      <magic-input v-model:value="info.accessKey" :placeholder="$i('s3.datasource.form.placeholder.accessKey')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.secretKey') }}</label>
      <magic-input v-model:value="info.secretKey" :placeholder="$i('s3.datasource.form.placeholder.secretKey')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.endPointSchema') }}</label>
      <magic-input v-model:value="info.endPointSchema" :placeholder="$i('s3.datasource.form.placeholder.endPointSchema')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.endPoint') }}</label>
      <magic-input v-model:value="info.endPoint" :placeholder="$i('s3.datasource.form.placeholder.endPoint')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.bucketName') }}</label>
      <magic-input v-model:value="info.bucketName" :placeholder="$i('s3.datasource.form.placeholder.bucketName')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.basePath') }}</label>
      <magic-input v-model:value="info.basePath" :placeholder="$i('s3.datasource.form.placeholder.basePath')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.tmpPath') }}</label>
      <magic-input v-model:value="info.tmpPath" :placeholder="$i('s3.datasource.form.placeholder.tmpPath')" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.presignedUrlMethod') }}</label>
      <magic-select v-model:value="info.presignedUrlMethod" :placeholder="$i('s3.datasource.form.placeholder.presignedUrlMethod')" :options="presignedUrlMethodOptions" width="100%" />
    </div>
    <div class="magic-form-row">
      <label>{{ $i('s3.datasource.form.properties') }}</label>
      <magic-monaco-editor v-model:value="info.propertiesText" :placeholder="$i('s3.datasource.form.placeholder.properties')" language="json" style="height:150px" />
    </div>
  </div>
</template>

<script setup>
import {inject, onMounted, ref, watch} from 'vue'
import {loadS3PlatformOptions} from '../service/magic-s3-datasource.js'

const $i = inject('i18n.format')

const props = defineProps({
  info: {
    type: Object,
    default: () => ({ properties: {} })
  }
})

const defaultPlatformOptions = [
  { text: 'minio', value: 'minio' },
  { text: 'aliyun-oss', value: 'aliyun-oss' },
  { text: 'tencent-cos', value: 'tencent-cos' },
  { text: 'aws-s3', value: 'aws-s3' }
]

const platformOptions = ref(defaultPlatformOptions.slice())

const normalizeOptions = (options) => {
  if (!Array.isArray(options)) {
    return []
  }
  const values = new Set()
  return options
    .filter(option => option && option.value)
    .filter(option => {
      if (values.has(option.value)) {
        return false
      }
      values.add(option.value)
      return true
    })
    .map(option => ({
      text: option.text || option.value,
      value: option.value
    }))
}

const ensureCurrentPlatformOption = () => {
  const current = props.info && props.info.platform
  if (!current) {
    return
  }
  if (!platformOptions.value.some(option => option.value === current)) {
    platformOptions.value = platformOptions.value.concat([{ text: current, value: current }])
  }
}

const loadPlatformOptions = () => {
  loadS3PlatformOptions().then(options => {
    if (options !== null) {
      platformOptions.value = normalizeOptions(options)
    }
    ensureCurrentPlatformOption()
  })
}

onMounted(() => {
  ensureCurrentPlatformOption()
  loadPlatformOptions()
})

watch(() => props.info && props.info.platform, () => {
  ensureCurrentPlatformOption()
})

const presignedUrlMethodOptions = [
  { text: 'PUT', value: 'PUT' },
  { text: 'POST', value: 'POST' }
]
</script>
