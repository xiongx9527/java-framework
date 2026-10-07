<template>
  <div class="magic-mcp-server-form">
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.form.key') }}</label>
      <magic-input v-model:value="info.key" :placeholder="$i('mcp.server.form.keyHint')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.form.name') }}</label>
      <magic-input v-model:value="info.name" :placeholder="$i('mcp.server.form.nameHint')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.form.transport') }}</label>
      <magic-select v-model:value="info.transport" :options="transportOptions" :default-select="true" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.form.endpoint') }}</label>
      <magic-input v-model:value="info.endpoint" :placeholder="$i('mcp.server.form.endpointHint')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.form.enabled') }}</label>
      <magic-select v-model:value="info.enabled" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.form.auth') }}</label>
      <magic-select v-model:value="authProvider" :options="authProviderOptions" width="100%"/>
    </div>

    <div class="magic-form-section">{{ $i('mcp.server.form.functions') }}</div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.enabled') }}</label>
      <magic-select v-model:value="functions.enabled" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.includeGroups') }}</label>
      <magic-input v-model:value="fnIncludeGroups" :placeholder="$i('mcp.server.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.includePaths') }}</label>
      <magic-input v-model:value="fnIncludePaths" :placeholder="$i('mcp.server.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.excludePaths') }}</label>
      <magic-input v-model:value="fnExcludePaths" :placeholder="$i('mcp.server.hint.list')" width="100%"/>
    </div>

    <div class="magic-form-section">{{ $i('mcp.server.form.api') }}</div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.enabled') }}</label>
      <magic-select v-model:value="api.enabled" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.methods') }}</label>
      <magic-input v-model:value="apiMethods" :placeholder="$i('mcp.server.hint.methods')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.includeGroups') }}</label>
      <magic-input v-model:value="apiIncludeGroups" :placeholder="$i('mcp.server.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.includePaths') }}</label>
      <magic-input v-model:value="apiIncludePaths" :placeholder="$i('mcp.server.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.excludePaths') }}</label>
      <magic-input v-model:value="apiExcludePaths" :placeholder="$i('mcp.server.hint.list')" width="100%"/>
    </div>
    <div class="magic-form-row">
      <label>{{ $i('mcp.server.scope.requireOptIn') }}</label>
      <magic-select v-model:value="api.requireOptIn" :options="enabledOptions" :default-select="true" width="100%"/>
    </div>
    <div class="magic-form-row" v-if="api.requireOptIn">
      <label>{{ $i('mcp.server.scope.optInOptionName') }}</label>
      <magic-input v-model:value="api.optInOptionName" :placeholder="$i('mcp.server.scope.optInOptionNameHint')" width="100%"/>
    </div>
  </div>
</template>
<script setup>
import {computed, inject, onMounted, ref} from 'vue'

const $i = inject('i18n.format')
const services = inject('service', null)
const mcpService = services ? services['mcp-server'] : null

const props = defineProps({
  info: {
    type: Object,
    default: () => ({})
  }
})

// datasource 表单打开时字段可能缺省，兜底成完整结构以便逐字段双向绑定
if (!props.info.transport) {
  props.info.transport = 'http-sse'
}
if (props.info.enabled == null) {
  props.info.enabled = true
}
if (!props.info.tools) {
  props.info.tools = {}
}
if (!props.info.tools.functions) {
  props.info.tools.functions = {}
}
if (!props.info.tools.api) {
  props.info.tools.api = {}
}

const info = props.info
const functions = info.tools.functions
const api = info.tools.api

const enabledOptions = [
  {text: $i('langchain4j.option.yes'), value: true},
  {text: $i('langchain4j.option.no'), value: false}
]

const transportOptions = [
  {text: $i('langchain4j.transport.http-sse'), value: 'http-sse'},
  {text: $i('langchain4j.transport.streamable-http'), value: 'streamable-http'}
]

// 认证方式（provider）：与 Starter 共用同一份内置清单；'' 表示不配置，该实例拒绝所有请求（401）
const ALL_AUTH_PROVIDERS = ['permit-all', 'sa-token-login', 'sa-token-oauth2', 'sa-token-jwt', 'sa-token-apikey', 'sa-token-sso']
// 后端返回的当前 classpath 可用清单（classpath 缺失的模块不可选）；null=尚未拉取或拉取失败
const availableProviders = ref(null)

onMounted(() => {
  if (mcpService && typeof mcpService.authProviders === 'function') {
    mcpService.authProviders().then(list => {
      availableProviders.value = Array.isArray(list) && list.length > 0 ? list : null
    })
  }
})

const authProvider = computed({
  get: () => (info.auth && info.auth.provider) || '',
  set: value => {
    if (!info.auth) {
      info.auth = {}
    }
    info.auth.provider = value
  }
})

const authProviderOptions = computed(() => {
  const values = [''].concat(availableProviders.value || ALL_AUTH_PROVIDERS)
  const current = info.auth && info.auth.provider
  // 当前值不在可用清单内也附加为选项，保证已存值可见（保存时由后端校验拒绝）
  if (current && values.indexOf(current) < 0) {
    values.push(current)
  }
  return values.map(value => ({
    text: value === '' ? $i('mcp.server.form.authNone') : value,
    value
  }))
})

const splitList = (value) => String(value == null ? '' : value)
  .split(',')
  .map(s => s.trim())
  .filter(s => s.length > 0)

// 列表字段：表单内以逗号分隔文本编辑，落到 info 上为字符串数组
const listField = (scope, key) => computed({
  get: () => (scope[key] || []).join(', '),
  set: (value) => { scope[key] = splitList(value) }
})

const fnIncludeGroups = listField(functions, 'includeGroups')
const fnIncludePaths = listField(functions, 'includePaths')
const fnExcludePaths = listField(functions, 'excludePaths')
const apiMethods = listField(api, 'methods')
const apiIncludeGroups = listField(api, 'includeGroups')
const apiIncludePaths = listField(api, 'includePaths')
const apiExcludePaths = listField(api, 'excludePaths')
</script>
<style scoped>
.magic-mcp-server-form {
  display: flex;
  flex-direction: column;
  padding: 5px;
}

.magic-mcp-server-form .magic-form-row {
  display: flex;
  align-items: center;
}

.magic-mcp-server-form .magic-form-row label {
  display: inline-block;
  width: 140px;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 400;
  text-align: right;
  padding: 0 5px;
}

.magic-mcp-server-form .magic-form-section {
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  font-weight: 600;
  padding-left: 150px;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  margin: 5px 0;
}
</style>
