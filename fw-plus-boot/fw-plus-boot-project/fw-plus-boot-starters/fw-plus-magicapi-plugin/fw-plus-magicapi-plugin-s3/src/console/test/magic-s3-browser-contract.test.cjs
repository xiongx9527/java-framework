const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const servicePath = path.join(__dirname, '../src/service/magic-s3-browser.js')
const componentPath = path.join(__dirname, '../src/components/magic-s3-browser.vue')
const pluginPath = path.join(__dirname, '../src/index.js')
const hostToolbarPath = path.join(__dirname, '../../../../../fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-toolbar.vue')
const hostPomPath = path.join(__dirname, '../../../../../fw-plus-magicapi-spring-boot-starter/pom.xml')

test('S3 浏览器服务封装存储、列表、预签名和删除接口', () => {
    assert.equal(fs.existsSync(servicePath), true, 'S3 浏览器服务应存在')
    const source = fs.readFileSync(servicePath, 'utf8')
    assert.match(source, /\/s3\/datasource\/availableStorages/)
    assert.match(source, /listFilesResult/)
    assert.match(source, /generateUploadPresignedUrl/)
    assert.match(source, /downloadFile/)
    assert.match(source, /\/delete/)
    assert.match(source, /'sendPost'/)
})

test('S3 浏览器保留下载入口并保护列表请求上下文', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /@click="downloadFile"/)
    assert.match(source, /const fileRequestSeq = ref\(0\)/)
    assert.match(source, /const requestSeq = \+\+fileRequestSeq\.value/)
    assert.match(source, /const expectedContext = fileRequestContext\(more \? nextMarker\.value : null\)/)
    assert.match(source, /if \(requestSeq !== fileRequestSeq\.value \|\| !sameFileRequestContext\(expectedContext\)\) return/)
})

test('S3 管理入口通过通用 adminOnly 标记受 adminEnabled 门控', () => {
    const pluginSource = fs.readFileSync(pluginPath, 'utf8')
    const hostToolbarSource = fs.readFileSync(hostToolbarPath, 'utf8')
    assert.match(pluginSource, /adminOnly: true/)
    assert.match(hostToolbarSource, /isPluginToolbarVisible/)
    assert.match(hostToolbarSource, /toolbar\.adminOnly/)
    assert.match(hostToolbarSource, /pluginData\.adminEnabled === true/)
    assert.match(hostToolbarSource, /toolbars\.push\({[\s\S]*id: it\.id/)
    assert.doesNotMatch(hostToolbarSource, /toolbars\.unshift\({[\s\S]*id: it\.id/)
})

test('Magic 控制台前端构建在资源复制前执行以保持 hash 资源一致', () => {
    const source = fs.readFileSync(hostPomPath, 'utf8')
    assert.match(source, /<directory>src\/console\/dist<\/directory>/)
    assert.match(source, /<targetPath>magic-editor<\/targetPath>/)
    assert.match(source, /<id>npm-install<\/id>[\s\S]*<phase>generate-resources<\/phase>/)
    assert.match(source, /<id>npm-build<\/id>[\s\S]*<phase>generate-resources<\/phase>/)
    assert.doesNotMatch(source, /<id>npm-build<\/id>[\s\S]*<phase>prepare-package<\/phase>/)
})

test('S3 插件安装时注入 request 与 modal 且保留 toolbar 契约', () => {
    const pluginSource = fs.readFileSync(pluginPath, 'utf8')
    assert.match(pluginSource, /name:\s*'s3'/)
    assert.match(pluginSource, /id:\s*'s3-browser'/)
    assert.match(pluginSource, /adminOnly:\s*true/)
    assert.match(pluginSource, /setS3BrowserRequest\(opt\.request\)/)
    assert.match(pluginSource, /setS3BrowserModal\(opt\.modal\)/)
})

test('S3 外链入口由插件 UI 注册且宿主不维护 S3 专属状态栏逻辑', () => {
    const pluginSource = fs.readFileSync(pluginPath, 'utf8')
    const hostStatusBarPath = path.join(__dirname, '../../../../../fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue')
    const hostStatusBarSource = fs.readFileSync(hostStatusBarPath, 'utf8')

    assert.match(pluginSource, /statusBarItems:\s*\[/)
    assert.match(pluginSource, /title:\s*'X-file-storage'/)
    assert.match(pluginSource, /icon:\s*'#magic-s3-s3'/)
    assert.match(pluginSource, /displayKey:\s*'s3'/)
    assert.match(pluginSource, /order:\s*100/)
    assert.match(pluginSource, /window\.open\('https:\/\/x-file-storage\.xuyanwu\.cn\/', '_blank'\)/)
    assert.match(hostStatusBarSource, /createPluginStatusBarItemsController/)
    assert.doesNotMatch(hostStatusBarSource, /hasS3Plugin/)
    assert.doesNotMatch(hostStatusBarSource, /openS3/)
    assert.doesNotMatch(hostStatusBarSource, /s3-datasource/)
})

test('S3 外链入口通过响应式插件状态栏列表触发重新渲染', () => {
    const hostStatusBarPath = path.join(__dirname, '../../../../../fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue')
    const hostStatusBarSource = fs.readFileSync(hostStatusBarPath, 'utf8')

    assert.match(hostStatusBarSource, /const pluginStatusBarItems = ref\(\[\]\)/)
    assert.match(hostStatusBarSource, /const \{ refresh: refreshPluginStatusBarItems \} = createPluginStatusBarItemsController\({/)
    assert.match(hostStatusBarSource, /getPlugins: \(\) => constants\.PLUGINS/)
    assert.match(hostStatusBarSource, /getHeader: \(\) => props\.config\.header/)
    assert.match(hostStatusBarSource, /setItems: items => pluginStatusBarItems\.value = items/)
    assert.match(hostStatusBarSource, /subscribe: listener => bus\.\$on\(Message\.PLUGINS_CHANGED, listener\)/)
    assert.match(hostStatusBarSource, /const insertPluginStatusBarItems = array => {/)
    assert.match(hostStatusBarSource, /array\.splice\(0, 0, \.\.\.items\)/)
    assert.doesNotMatch(hostStatusBarSource, /pluginStatusBarItems\.value[\s\S]*\.forEach\(it => array\.push\(it\)\)/)
    assert.doesNotMatch(hostStatusBarSource, /array\.findIndex\(it => it\.displayKey === 'repo'\)/)
    assert.doesNotMatch(hostStatusBarSource, /constants\.PLUGINS\.filter\(plugin => plugin\.statusBarItems/)
})

test('S3 浏览器服务暴露 modal setter 且不改变后端接口契约', () => {
    const source = fs.readFileSync(servicePath, 'utf8')
    assert.match(source, /let activeModal = null/)
    assert.match(source, /export const setS3BrowserModal = modal =>/)
    assert.match(source, /export const getS3BrowserModal = \(\) => activeModal/)
    assert.match(source, /\.error\(\(error, response, raw\) => reject\(normalizeRequestError\(error, response, raw\)\)\)/)
    assert.doesNotMatch(source, /String\(error \|\| 'Request failed'\)/)
    assert.match(source, /\/s3\/datasource\/availableStorages/)
    assert.match(source, /listFilesResult/)
    assert.match(source, /generateUploadPresignedUrl/)
    assert.match(source, /\/delete/)
})

test('S3 浏览器使用 Magic 控件和控制台主题变量', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /<magic-select[\s\S]*v-model:value="selectedStorageKey"/)
    assert.match(source, /@select="selectStorage"/)
    assert.match(source, /<magic-input[\s\S]*v-model:value="basePathDraft"/)
    assert.match(source, /<magic-input[\s\S]*v-model:value="search"/)
    assert.match(source, /var\(--magic-input-height\)/)
    assert.match(source, /var\(--main-border-color\)/)
    assert.match(source, /var\(--main-background-color/)
    assert.doesNotMatch(source, /<select\s/)
    assert.doesNotMatch(source, /<input\s/)
})

test('S3 存储下拉默认显示请选择且不自动选择第一个存储', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /const storageOptions = computed\(\(\) => \[\s*{\s*text: \$i\('s3\.browser\.select'\),\s*value: ''\s*},/)
    assert.match(source, /if \(!selectedStorageKey\.value\) {[\s\S]*items\.value = \[\][\s\S]*return[\s\S]*}/)
    assert.doesNotMatch(source, /selectedStorageKey\.value = storages\.value\[0\]\.key/)
})

test('S3 基础路径默认只读并通过编辑按钮进入保存状态', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /const basePathEditing = ref\(false\)/)
    assert.match(source, /:disabled="!selectedStorageKey \|\| !basePathEditing \|\| loading\.files"/)
    assert.match(source, /@click="handleBasePathAction"/)
    assert.match(source, /{{ basePathEditing \? \$i\('s3\.browser\.save'\) : \$i\('s3\.browser\.edit'\) }}/)
    assert.match(source, /const handleBasePathAction = \(\) => {[\s\S]*basePathEditing\.value = true[\s\S]*saveBasePath\(\)/)
    assert.match(source, /basePathEditing\.value = false/)
})

test('S3 浏览器统一提取错误并通过 modal alert 弹出', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /const extractErrorMessage = \(cause, fallback\) =>/)
    assert.match(source, /const showError = \(cause, fallbackKey\) =>/)
    assert.match(source, /getS3BrowserModal\(\)/)
    assert.match(source, /modal\.alert\(message, \$i\('s3\.browser\.errorTitle'\)\)/)
    assert.doesNotMatch(source, /class="s3-error"/)
    assert.doesNotMatch(source, /role="alert"/)
    assert.doesNotMatch(source, /const error = ref\(/)
    assert.doesNotMatch(source, /error\.value/)
    ;[
        'loadStoragesFailed',
        'listFilesFailed',
        'saveBasePathFailed',
        'previewFailed',
        'downloadFailed',
        'deleteFailed'
    ].forEach(key => assert.match(source, new RegExp(`s3\\.browser\\.${key}`)))
})

test('S3 删除确认优先使用 Magic modal confirm 并保留内置降级', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /const confirmDeleteFile = async \(targetFile = deleteCandidate\.value\) =>/)
    assert.match(source, /modal\.confirm\(\$i\('s3\.browser\.delete'\), \$i\('s3\.browser\.confirmDelete', selectedFile\.value\.name\), \(\) => confirmDeleteFile\(selectedFile\.value\)\)/)
    assert.match(source, /deleteCandidate\.value = selectedFile\.value/)
    assert.match(source, /class="s3-confirm"/)
})

test('S3 文件详情只在选中文件后展示且默认不占位', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /<aside\s+v-if="selectedFile"\s+class="s3-details">/)
    assert.doesNotMatch(source, /\$i\('s3\.browser\.selectFile'\)/)
    assert.match(source, /selectedFile\.value = null/)
})

test('S3 状态提示文本居中显示', () => {
    const source = fs.readFileSync(componentPath, 'utf8')
    assert.match(source, /\.s3-state\s*{[\s\S]*text-align:\s*center/)
})
