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
      <label>{{ $i('s3.datasource.form.other') }}</label>
      <magic-monaco-editor v-model:value="info.other" :placeholder="$i('s3.datasource.form.placeholder.other')" language="json" style="height:150px" />
    </div>
  </div>
</template>

<script setup>
import {inject} from 'vue'

const $i = inject('i18n.format')

const props = defineProps({
  info: {
    type: Object,
    default: () => ({ other: '{}' })
  }
})

const platformOptions = [
  { text: 'minio', value: 'minio' },
  { text: 'aliyun-oss', value: 'aliyun-oss' },
  { text: 'tencent-cos', value: 'tencent-cos' },
  { text: 'aws-s3', value: 'aws-s3' }
]

const presignedUrlMethodOptions = [
  { text: 'PUT', value: 'PUT' },
  { text: 'POST', value: 'POST' }
]
</script>
