---
change: align-s3-browser-console-ui
design-doc: docs/superpowers/specs/2026-07-24-align-s3-browser-console-ui-design.md
base-ref: a3bd4eee0aef7ec8270841cd03293a336e3dbf9b
archived-with: 2026-07-24-align-s3-browser-console-ui
---

# S3 文件管理控制台 UI 对齐实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不修改后端接口和宿主 Magic 控制台的前提下，把 S3 文件管理页对齐 Magic 控制台视觉，并让所有关键请求失败通过 Magic modal 明确弹出。

**Architecture:** 本变更采用插件内重构方案，源码边界限定在 `fw-plus-magicapi-plugin-s3/src/console`，只通过与现有 `setS3BrowserRequest(opt.request)` 同类的模块级 setter 注入 `opt.modal`。`magic-s3-browser.vue` 保持单文件实现：模板按顶部紧凑表单、路径导航、列表、详情四区重排，错误处理收敛为 `extractErrorMessage` 与 `showError`，删除确认优先走 `modal.confirm` 并保留内置确认层降级。

**Tech Stack:** Vue 3 `<script setup>`、Magic 控制台组件 `magic-select` / `magic-input`、Node `node:test` 契约测试、Vite 2、Maven 资源复制链路。

## Global Constraints

- 设计依据：`docs/superpowers/specs/2026-07-24-align-s3-browser-console-ui-design.md`，特别是“确认方案”“错误处理”“样式约束”“测试策略”。
- OpenSpec 任务依据：`openspec/changes/align-s3-browser-console-ui/tasks.md`。
- 源码范围限定在 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console`，不修改宿主 UI，不改后端接口。
- 保留现有交互语义：选择存储、路径导航、搜索、加载更多、basePath 保存、预览、下载和删除。
- 使用控制台变量：`var(--magic-input-height)`、`var(--main-border-color)`、`var(--main-background-color)`；不要引入大卡片、渐变、营销式区块或宿主外视觉。
- 失败弹窗覆盖：加载存储、列文件、保存 basePath、预览、下载、删除。
- 若 `opt.modal` 不存在，页面不能崩溃；至少更新内联 `error` 状态。
- 实施前不得把本计划创建动作混入实现提交；实施完成后再按任务更新 `openspec/changes/align-s3-browser-console-ui/tasks.md`。

---

## File Structure

- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/test/magic-s3-browser-contract.test.cjs`
  - 负责用源码契约锁住插件入口、toolbar admin 门控、modal 注入、统一错误 helper、失败场景弹窗、Magic 控件和主题变量。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/service/magic-s3-browser.js`
  - 负责保存 `activeRequest` 与新增 `activeModal`，暴露 `setS3BrowserModal` / `getS3BrowserModal`，不改变现有 S3 API URL。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js`
  - 负责在插件安装时把 `opt.modal` 注入 S3 浏览服务模块，并保持 `name: 's3'` 与 `s3-browser` toolbar 配置。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/components/magic-s3-browser.vue`
  - 负责 UI 对齐、错误弹窗、删除确认降级、状态恢复语义。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/i18n/zh-cn.js`
  - 负责新增中文错误标题、具体失败文案和 UI 标签。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/i18n/en.js`
  - 负责新增英文错误标题、具体失败文案和 UI 标签。
- Modify: `openspec/changes/align-s3-browser-console-ui/tasks.md`
  - 负责在验证通过后勾选 1.1-4.4。

### Task 1: 契约测试先行

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/test/magic-s3-browser-contract.test.cjs`

**Interfaces:**
- Consumes: 现有 `servicePath`、`componentPath`、`pluginPath`、`hostToolbarPath` 常量。
- Produces: 测试断言后续任务必须实现的源码契约：`setS3BrowserModal`、`getS3BrowserModal`、`extractErrorMessage`、`showError`、`modal.alert`、`modal.confirm`、Magic 控件、主题变量和具体 i18n key。

- [x] **Step 1: 扩展契约测试**

把 `magic-s3-browser-contract.test.cjs` 中现有 3 个测试保留，并追加以下测试：

```javascript
test('S3 插件安装时注入 request 与 modal 且保留 toolbar 契约', () => {
    const pluginSource = fs.readFileSync(pluginPath, 'utf8')
    assert.match(pluginSource, /name:\s*'s3'/)
    assert.match(pluginSource, /id:\s*'s3-browser'/)
    assert.match(pluginSource, /adminOnly:\s*true/)
    assert.match(pluginSource, /setS3BrowserRequest\(opt\.request\)/)
    assert.match(pluginSource, /setS3BrowserModal\(opt\.modal\)/)
})

test('S3 浏览器服务暴露 modal setter 且不改变后端接口契约', () => {
    const source = fs.readFileSync(servicePath, 'utf8')
    assert.match(source, /let activeModal = null/)
    assert.match(source, /export const setS3BrowserModal = modal =>/)
    assert.match(source, /export const getS3BrowserModal = \(\) => activeModal/)
    assert.match(source, /\/s3\/datasource\/availableStorages/)
    assert.match(source, /listFilesResult/)
    assert.match(source, /generateUploadPresignedUrl/)
    assert.match(source, /\/delete/)
})

test('S3 浏览器使用 Magic 控件和控制台主题变量', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /<magic-select[\s\S]*v-model:value="selectedStorageKey"/)
    assert.match(source, /<magic-input[\s\S]*v-model:value="basePathDraft"/)
    assert.match(source, /<magic-input[\s\S]*v-model:value="search"/)
    assert.match(source, /var\(--magic-input-height\)/)
    assert.match(source, /var\(--main-border-color\)/)
    assert.match(source, /var\(--main-background-color/)
    assert.doesNotMatch(source, /<select\s/)
    assert.doesNotMatch(source, /<input\s/)
})

test('S3 浏览器统一提取错误并通过 modal alert 弹出', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /const extractErrorMessage = \(cause, fallback\) =>/)
    assert.match(source, /const showError = \(cause, fallbackKey\) =>/)
    assert.match(source, /getS3BrowserModal\(\)/)
    assert.match(source, /modal\.alert\(message, \$i\('s3\.browser\.errorTitle'\)\)/)
    ;[
        'loadStoragesFailed',
        'listFilesFailed',
        'saveBasePathFailed',
        'previewFailed',
        'downloadFailed',
        'deleteFailed'
    ].forEach(key => assert.match(source, new RegExp(`s3\\\\.browser\\\\.${key}`)))
})

test('S3 删除确认优先使用 Magic modal confirm 并保留内置降级', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /modal\.confirm\(/)
    assert.match(source, /deleteCandidate\.value = selectedFile\.value/)
    assert.match(source, /class="s3-confirm"/)
})
```

- [x] **Step 2: 运行测试确认失败**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
npm test
```

Expected: FAIL。失败点应集中在 `setS3BrowserModal`、`magic-select` / `magic-input`、`extractErrorMessage`、`showError`、`modal.alert`、`modal.confirm` 和新增 i18n key 未出现。

- [x] **Step 3: 复核 Task 1 覆盖 OpenSpec**

确认 `openspec/changes/align-s3-browser-console-ui/tasks.md` 中 1.1、1.2 由本任务覆盖，但此时不要勾选；勾选放到 Task 5 验证通过后统一完成。

### Task 2: 插件 modal 注入契约

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/service/magic-s3-browser.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/test/magic-s3-browser-contract.test.cjs`

**Interfaces:**
- Consumes: 插件安装参数 `opt.request`、`opt.modal`。
- Produces:
  - `setS3BrowserModal(modal: object | null): void`
  - `getS3BrowserModal(): object | null`
  - `index.js` 在安装阶段调用 `setS3BrowserModal(opt.modal)`。

- [x] **Step 1: 更新服务模块**

在 `magic-s3-browser.js` 中加入 modal 存储接口。保留所有已有 request API URL，不改变 `createS3BrowserService` 返回对象：

```javascript
let activeRequest = null
let activeModal = null

const invoke = (request, method, url, params) => new Promise((resolve, reject) => {
    if (!request || typeof request[method] !== 'function') {
        reject(new Error('Magic-API request is unavailable'))
        return
    }
    request[method](url, params)
        .success(resolve)
        .exception((code, message) => reject(new Error(message || String(code))))
        .error((error) => reject(error instanceof Error ? error : new Error(String(error || 'Request failed'))))
})

const unwrap = response => response && response.data !== undefined ? response.data : response

export const setS3BrowserRequest = request => {
    activeRequest = request
}

export const setS3BrowserModal = modal => {
    activeModal = modal
}

export const getS3BrowserModal = () => activeModal
```

- [x] **Step 2: 更新插件入口注入**

在 `index.js` 中把 import 改为：

```javascript
import { setS3BrowserModal, setS3BrowserRequest } from './service/magic-s3-browser.js'
```

并在 `setS3BrowserRequest(opt.request)` 后立即加入：

```javascript
setS3BrowserModal(opt.modal)
```

- [x] **Step 3: 运行契约测试确认本任务通过**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
npm test
```

Expected: 与 modal 注入相关断言 PASS；UI 与错误 helper 相关断言仍可失败，留给 Task 3 和 Task 4。

### Task 3: `magic-s3-browser.vue` UI 与错误弹窗

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/components/magic-s3-browser.vue`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/test/magic-s3-browser-contract.test.cjs`

**Interfaces:**
- Consumes:
  - `getS3BrowserService(): { listStorages, listFiles, generatePresignedUrl, downloadFile, deleteFile }`
  - `getS3BrowserModal(): object | null`
  - i18n keys from Task 4.
- Produces:
  - `extractErrorMessage(cause, fallback): string`
  - `showError(cause, fallbackKey): void`
  - `requestDeleteFile(): void`
  - `confirmDeleteFile(): void`
  - UI 使用 `magic-select`、`magic-input` 和控制台主题变量。

- [x] **Step 1: 更新 `<script setup>` import**

把浏览页 service import 改为：

```javascript
import { getS3BrowserModal, getS3BrowserService } from '../service/magic-s3-browser.js'
```

- [x] **Step 2: 添加 storage select options 与错误 helper**

在 `loading` 定义后加入：

```javascript
const storageOptions = computed(() => storages.value.map(storage => ({
    text: storage.name || storage.key,
    value: storage.key
})))
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
    error.value = message
    const modal = getS3BrowserModal()
    if (modal && typeof modal.alert === 'function') {
        modal.alert(message, $i('s3.browser.errorTitle'))
    }
}
```

删除旧的：

```javascript
const displayError = cause => { error.value = cause && cause.message ? cause.message : $i('s3.browser.loadFailed') }
```

- [x] **Step 3: 把失败路径接入 `showError`**

把 catch 分支替换为具体 fallback key：

```javascript
showError(cause, more ? 's3.browser.loadMoreFailed' : 's3.browser.listFilesFailed')
showError(cause, 's3.browser.previewFailed')
showError(cause, 's3.browser.downloadFailed')
showError(cause, 's3.browser.deleteFailed')
showError(cause, 's3.browser.loadStoragesFailed')
```

`saveBasePath` 不调用后端，但属于用户可见动作；加入 try/catch，确保异常时弹出 `saveBasePathFailed`：

```javascript
const saveBasePath = () => {
    error.value = ''
    try {
        basePath.value = normalizedPath(basePathDraft.value)
        basePathDraft.value = basePath.value
        changePath(basePath.value)
    } catch (cause) {
        showError(cause, 's3.browser.saveBasePathFailed')
    }
}
```

- [x] **Step 4: 删除失败保持当前列表和选中项**

把 `deleteFile` 改名为 `confirmDeleteFile`，成功后才清空选择；catch 中不要清空 `selectedFile`、`deleteCandidate`、`items`：

```javascript
const confirmDeleteFile = async () => {
    if (!deleteCandidate.value) return
    loading.deleting = true
    error.value = ''
    try {
        await service().deleteFile(selectedStorageKey.value, deleteCandidate.value.url)
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
```

- [x] **Step 5: 删除确认优先使用 modal.confirm**

新增 `requestDeleteFile`，保留 `deleteCandidate` 降级：

```javascript
const requestDeleteFile = () => {
    if (!selectedFile.value) return
    const modal = getS3BrowserModal()
    if (modal && typeof modal.confirm === 'function') {
        modal.confirm($i('s3.browser.confirmDelete', selectedFile.value.name), $i('s3.browser.delete'), () => {
            deleteCandidate.value = selectedFile.value
            confirmDeleteFile()
        })
        return
    }
    deleteCandidate.value = selectedFile.value
}
```

- [x] **Step 6: 重构模板为 Magic 控制台风格**

用以下结构替换 `<template>`，保持原有语义但移除原生 `select/input`：

```vue
<template>
    <section class="magic-s3-browser">
        <header class="s3-toolbar">
            <label>{{ $i('s3.browser.storage') }}</label>
            <magic-select v-model:value="selectedStorageKey" :placeholder="$i('s3.browser.selectStorage')" :options="storageOptions" width="180px" :disabled="loading.storages" @change="selectStorage" />
            <label>{{ $i('s3.browser.basePath') }}</label>
            <magic-input v-model:value="basePathDraft" width="180px" @keyup.enter="saveBasePath" />
            <button class="s3-button" :disabled="!selectedStorageKey || loading.files" @click="saveBasePath">{{ $i('s3.browser.save') }}</button>
            <label>{{ $i('s3.browser.searchLabel') }}</label>
            <magic-input v-model:value="search" :placeholder="$i('s3.browser.search')" width="220px" :disabled="!selectedStorageKey || loading.files" @keyup.enter="reloadFiles" />
            <button class="s3-button" :disabled="!selectedStorageKey || loading.files" @click="reloadFiles">{{ $i('s3.browser.searchAction') }}</button>
            <button class="s3-button" :disabled="!selectedStorageKey || loading.files" @click="reloadFiles">{{ $i('s3.browser.refresh') }}</button>
        </header>

        <p v-if="error" class="s3-error" role="alert">{{ error }}</p>
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
            <aside class="s3-details">
                <template v-if="selectedFile">
                    <header><strong>{{ selectedFile.name }}</strong><button class="s3-icon-button" @click="selectedFile = null">x</button></header>
                    <p>{{ $i('s3.browser.previewUrl') }}: <a v-if="previewUrl" :href="previewUrl" target="_blank" rel="noopener">{{ previewUrl }}</a><span v-else>-</span></p>
                    <div class="s3-actions">
                        <button class="s3-button" :disabled="selectedFile.type === 'folder' || loading.preview" @click="previewFile">{{ $i('s3.browser.preview') }}</button>
                        <button class="s3-button" :disabled="selectedFile.type === 'folder' || loading.download" @click="downloadFile">{{ $i('s3.browser.download') }}</button>
                        <button class="s3-button danger" @click="requestDeleteFile">{{ $i('s3.browser.delete') }}</button>
                    </div>
                    <pre>{{ JSON.stringify(selectedFile, null, 2) }}</pre>
                </template>
                <p v-else class="s3-state">{{ $i('s3.browser.selectFile') }}</p>
            </aside>
        </div>

        <div v-if="deleteCandidate" class="s3-confirm" role="dialog" aria-modal="true">
            <p>{{ $i('s3.browser.confirmDelete', deleteCandidate.name) }}</p>
            <button class="s3-button" @click="deleteCandidate = null">{{ $i('s3.browser.cancel') }}</button>
            <button class="s3-button danger" :disabled="loading.deleting" @click="confirmDeleteFile">{{ $i('s3.browser.delete') }}</button>
        </div>
    </section>
</template>
```

- [x] **Step 7: 重写样式为控制台变量和稳定布局**

替换 `<style scoped>` 内容：

```css
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
.s3-error {
    margin: 0;
    padding: 8px;
    color: #b42318;
    border-bottom: 1px solid var(--main-border-color);
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
```

- [x] **Step 8: 运行契约测试**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
npm test
```

Expected: UI、modal 注入、错误 helper 相关断言 PASS；若 i18n key 断言失败，进入 Task 4。

### Task 4: i18n 文案补齐

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/i18n/zh-cn.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/i18n/en.js`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/test/magic-s3-browser-contract.test.cjs`

**Interfaces:**
- Consumes: `showError(cause, fallbackKey)` 使用的 `s3.browser.*` key。
- Produces: 中英文一致的 browser 文案集合。

- [x] **Step 1: 更新中文 browser 文案**

在 `zh-cn.js` 的 `s3.browser` 内加入或替换以下 key：

```javascript
storage: '存储',
searchLabel: '文件名',
refresh: '刷新',
folder: '目录',
file: '文件',
selectFile: '请选择文件',
errorTitle: 'S3 文件管理错误',
loadStoragesFailed: '加载 S3 存储失败',
listFilesFailed: '加载 S3 文件失败',
loadMoreFailed: '加载更多 S3 文件失败',
saveBasePathFailed: '保存基础路径失败',
previewFailed: '无法生成预览链接',
downloadFailed: '无法生成下载链接',
deleteFailed: '删除 S3 文件失败'
```

保留已有 `title`、`selectStorage`、`path`、`basePath`、`save`、`search`、`searchAction`、`loading`、`empty`、`loadMore`、`previewUrl`、`preview`、`download`、`delete`、`confirmDelete`、`cancel`。

- [x] **Step 2: 更新英文 browser 文案**

在 `en.js` 的 `s3.browser` 内加入或替换以下 key：

```javascript
storage: 'Storage',
searchLabel: 'File name',
refresh: 'Refresh',
folder: 'Folder',
file: 'File',
selectFile: 'Select a file',
errorTitle: 'S3 File Manager Error',
loadStoragesFailed: 'Unable to load S3 storages',
listFilesFailed: 'Unable to load S3 files',
loadMoreFailed: 'Unable to load more S3 files',
saveBasePathFailed: 'Unable to save base path',
previewFailed: 'Unable to generate preview URL',
downloadFailed: 'Unable to generate download URL',
deleteFailed: 'Unable to delete S3 file'
```

保留已有 `title`、`selectStorage`、`path`、`basePath`、`save`、`search`、`searchAction`、`loading`、`empty`、`loadMore`、`previewUrl`、`preview`、`download`、`delete`、`confirmDelete`、`cancel`。

- [x] **Step 3: 运行契约测试确认全部通过**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
npm test
```

Expected: `node --test test/*.test.cjs` 全部 PASS。

### Task 5: 构建验证与 OpenSpec tasks 勾选

**Files:**
- Modify: `openspec/changes/align-s3-browser-console-ui/tasks.md`
- Verify generated output: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/dist/magic-s3.1.0.0.iife.js`
- Verify generated output: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/target/classes/magic-editor/plugins/magic-s3.1.0.0.iife.js`

**Interfaces:**
- Consumes: Task 1-4 的源码变更。
- Produces: 通过的测试、前端构建产物、Maven 资源复制产物、已勾选的 OpenSpec tasks。

- [x] **Step 1: 运行前端契约测试**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
npm test
```

Expected: PASS，输出包含 `# pass` 且 `# fail 0`。

- [x] **Step 2: 运行前端构建**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
npm run build
```

Expected: PASS，`dist/magic-s3.1.0.0.iife.js` 重新生成。

- [x] **Step 3: 验证 dist 产物包含关键更新**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console
rg "S3 File Manager Error|S3 文件管理错误|setS3BrowserModal|magic-select|main-border-color" dist/magic-s3.1.0.0.iife.js
```

Expected: 能匹配到错误标题、modal setter、Magic 控件或主题变量相关内容。

- [x] **Step 4: 运行 S3 插件 Maven package**

Run:

```bash
cd fw-plus-boot
./mvnw -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3 -am package -DskipTests
```

Expected: PASS，且新版 IIFE 被复制到 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/target/classes/magic-editor/plugins/magic-s3.1.0.0.iife.js`。

- [x] **Step 5: 验证 target/classes 产物包含关键更新**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3
rg "S3 File Manager Error|S3 文件管理错误|setS3BrowserModal|main-border-color" target/classes/magic-editor/plugins/magic-s3.1.0.0.iife.js
```

Expected: 能匹配到错误标题、modal setter 或主题变量相关内容。

- [x] **Step 6: 检查空白和无关 diff**

Run:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework
git diff --check
git status --short
```

Expected: `git diff --check` 无输出且退出码为 0；`git status --short` 只包含本 change 的源码、构建产物和 `tasks.md` 更新。若出现无关文件，保留用户既有改动，不要回退。

- [x] **Step 7: 勾选 OpenSpec tasks**

在 `openspec/changes/align-s3-browser-console-ui/tasks.md` 中把 1.1-4.4 全部改为已完成：

```markdown
## 1. 基线与契约

- [x] 1.1 对比 `magic-s3-datasource-info.vue`、同类插件信息页和当前 `magic-s3-browser.vue`，确认需要复用的 Magic 控制台布局、控件和主题变量。
- [x] 1.2 补充或更新 S3 插件前端契约测试，覆盖工具栏入口、`name: "s3"`、`s3-browser` 工具栏和错误提示能力约束。

## 2. 页面风格一致性

- [x] 2.1 重构 `magic-s3-browser.vue` 顶部操作区，使用与原 magic-s3 页面一致的紧凑表单布局和 Magic 控件风格。
- [x] 2.2 调整文件列表、路径导航、详情区、空状态、加载状态和删除确认区，使其使用 Magic 控制台一致的边框、间距、颜色变量和稳定尺寸。
- [x] 2.3 保留选择存储、路径导航、搜索、加载更多、basePath 保存、预览、下载和删除的现有交互语义。

## 3. 错误提示

- [x] 3.1 在 `magic-s3-browser.vue` 中封装统一错误消息提取与弹出 helper，优先使用插件上下文可用的 Magic modal/message 机制。
- [x] 3.2 将加载存储、列文件、保存 basePath、预览、下载和删除失败场景接入弹窗/消息提示，并保留后端返回的可读错误信息。
- [x] 3.3 确保删除、预览和下载失败不会误显示成功状态，页面列表和当前选择保持可恢复。

## 4. 验证与构建

- [x] 4.1 运行 S3 插件前端测试，确认契约测试通过。
- [x] 4.2 运行 S3 插件前端构建，确认新版 `dist/magic-s3.1.0.0.iife.js` 包含更新内容。
- [x] 4.3 运行 S3 插件 Maven package 或资源复制链路，确认新版 IIFE 进入 `target/classes/magic-editor/plugins`。
- [x] 4.4 检查目标文件 diff 和 `git diff --check`，确认未引入无关格式问题。
```

- [x] **Step 8: 最终复验**

Run:

```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework
git diff --check
```

Expected: PASS，无空白错误。

## Self-Review

- Spec coverage: 设计文档中的插件内重构、modal setter 注入、Magic 控件、控制台主题变量、六类失败弹窗、删除 confirm 降级、构建和 Maven 复制验证分别由 Task 1-5 覆盖。
- Placeholder scan: 本计划未使用占位式任务描述或跳转式省略描述。
- Type consistency: `setS3BrowserModal` / `getS3BrowserModal` 在 Task 2 定义，Task 3 使用；`extractErrorMessage(cause, fallback)` / `showError(cause, fallbackKey)` 在 Task 3 定义并被失败路径统一调用；i18n key 在 Task 4 生产并被 Task 3 消费。
