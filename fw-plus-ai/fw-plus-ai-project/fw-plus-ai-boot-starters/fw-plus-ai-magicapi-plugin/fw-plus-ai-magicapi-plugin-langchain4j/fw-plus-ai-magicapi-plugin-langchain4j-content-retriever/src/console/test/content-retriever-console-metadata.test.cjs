/**
 * 本类型 console 元数据与后端 provider 的交叉校验（解析后端源码为权威口径）。
 */
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const {pathToFileURL} = require('node:url');

const consoleDir = path.resolve(__dirname, '..');
const pluginDir = path.resolve(consoleDir, '../../../../../..',
    'fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/langchain4j');
const providerDir = path.join(pluginDir, 'retriever/provider');

const TARGET_TYPE = 'langchain4j-content-retriever';
const TARGET_METHOD = 'build';
const READER_CALL = /\bo\.(required|string|integer|longValue|dbl|floatValue|bool|enumValue|duration|stringList|stringMap)\(\s*"([^"]+)"/g;

/** 提取 build 方法体（大括号配对） */
function bodyOf(src, method) {
    const re = new RegExp('public ([\\w<>,\\s.]+?) ' + method + '\\([^)]*\\)\\s*\\{');
    const match = re.exec(src);
    if (!match) {
        return null;
    }
    const start = match.index + match[0].length;
    let depth = 1;
    let i = start;
    while (i < src.length && depth > 0) {
        if (src[i] === '{') depth++;
        else if (src[i] === '}') depth--;
        i++;
    }
    return src.slice(start, i - 1);
}

/** 本类型下 provider → 字段名（后端权威口径） */
function backendProviders() {
    const result = {};
    for (const file of fs.readdirSync(providerDir)) {
        if (!file.endsWith('.java')) continue;
        const src = fs.readFileSync(path.join(providerDir, file), 'utf8');
        const declared = /public String provider\(\)\s*\{\s*return "([^"]+)"/.exec(src);
        const body = bodyOf(src, TARGET_METHOD);
        if (!declared || body === null) continue;
        const names = [];
        let m;
        READER_CALL.lastIndex = 0;
        while ((m = READER_CALL.exec(body)) !== null) {
            if (!names.includes(m[2])) names.push(m[2]);
        }
        result[declared[1]] = names;
    }
    return result;
}

let metadata;
let zhCN;
let en;
test.before(async () => {
    metadata = await import(pathToFileURL(path.join(consoleDir, 'src/metadata/providers.js')));
    zhCN = (await import(pathToFileURL(path.join(consoleDir, 'src/i18n/zh-cn.js')))).default;
    en = (await import(pathToFileURL(path.join(consoleDir, 'src/i18n/en.js')))).default;
});

test('元数据表与后端 langchain4j-content-retriever provider 读取的 options 键一致', () => {
    const backend = backendProviders();
    const problems = [];
    for (const provider of Object.keys(backend)) {
        const fields = metadata.PROVIDER_FIELDS[provider];
        if (!fields) {
            problems.push('缺少 provider 字段定义: ' + provider);
            continue;
        }
        const actual = fields.map(f => f.name);
        const missing = backend[provider].filter(k => !actual.includes(k));
        const extra = actual.filter(k => !backend[provider].includes(k));
        if (missing.length) problems.push(provider + ' 缺少字段: ' + missing.join(', '));
        if (extra.length) problems.push(provider + ' 多出字段: ' + extra.join(', '));
    }
    for (const provider of Object.keys(metadata.PROVIDER_FIELDS)) {
        if (!backend[provider]) problems.push('元数据表存在后端未注册的 provider: ' + provider);
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('引用字段的必填标记与后端 o.required 一致', () => {
    const required = {};
    for (const file of fs.readdirSync(providerDir)) {
        if (!file.endsWith('.java')) continue;
        const src = fs.readFileSync(path.join(providerDir, file), 'utf8');
        const declared = /public String provider\(\)\s*\{\s*return "([^"]+)"/.exec(src);
        const body = bodyOf(src, TARGET_METHOD);
        if (!declared || body === null) continue;
        const names = [];
        let m;
        const requiredCall = /\bo\.required\(\s*"([^"]+)"/g;
        while ((m = requiredCall.exec(body)) !== null) {
            names.push(m[1]);
        }
        required[declared[1]] = names;
    }
    const problems = [];
    for (const provider of Object.keys(metadata.REFERENCE_FIELDS)) {
        for (const ref of metadata.referenceFields(provider)) {
            const backendRequired = (required[provider] || []).includes(ref.name);
            if (!!ref.required !== backendRequired) {
                problems.push(provider + ' 的 ' + ref.name + ' 必填标记与后端不一致（后端'
                    + (backendRequired ? '必填' : '选填') + '）');
            }
        }
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('引用的 datasource 字段与 provider 字段表一一对应', () => {
    const problems = [];
    for (const provider of Object.keys(metadata.PROVIDER_FIELDS)) {
        for (const ref of metadata.referenceFields(provider)) {
            const declared = metadata.PROVIDER_FIELDS[provider].some(f => f.name === ref.name);
            if (!declared) problems.push(provider + ' 引用字段未在字段表中声明: ' + ref.name);
        }
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('字段都有中英文文案', () => {
    const problems = [];
    for (const provider of Object.keys(metadata.PROVIDER_FIELDS)) {
        for (const f of metadata.PROVIDER_FIELDS[provider]) {
            if (!zhCN.langchain4j.field[f.name]) problems.push('缺少中文文案: ' + f.name);
            if (!en.langchain4j.field[f.name]) problems.push('缺少英文文案: ' + f.name);
        }
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('provider 短名都有中英文展示名', () => {
    for (const p of Object.keys(metadata.PROVIDER_FIELDS)) {
        assert.ok(zhCN.langchain4j.provider[p], '缺少中文 provider 名: ' + p);
        assert.ok(en.langchain4j.provider[p], '缺少英文 provider 名: ' + p);
    }
});

test('index.js 注册该 datasource type 并绑定组件', () => {
    const index = fs.readFileSync(path.join(consoleDir, 'src/index.js'), 'utf8');
    assert.ok(index.includes("type: '" + TARGET_TYPE + "'"), 'index.js 缺少 type 注册');
    assert.ok(index.includes('MagicLangchain4jInfo'), 'index.js 缺少组件绑定');
    assert.ok(index.includes('/langchain4j/content-retriever/datasource/test') === false, '连接测试路径应由 service 拼接');
});

test('index.js 工厂返回含顶层 name（magic-toolbar 反查 adminEnabled 依赖它）', () => {
    // magic-toolbar.vue: pluginData = PLUGINS_DATA.find(item => item.name === plugin.name)，
    // 其中 plugin 是工厂返回对象；缺失顶层 name 时 adminOnly 的 kb-browser 入口永远被过滤。
    const index = fs.readFileSync(path.join(consoleDir, 'src/index.js'), 'utf8');
    const ret = index.indexOf('return {');
    assert.ok(ret > -1, 'index.js 缺少工厂 return');
    const head = index.slice(ret, ret + 400);
    assert.ok(
        head.includes("name: '" + TARGET_TYPE + "'"),
        'index.js 工厂 return 开头缺少顶层 name: ' + TARGET_TYPE
    );
});

test('设置视图不受 selectedDataset 守卫（kb_setting 独立于数据集）', () => {
    const src = fs.readFileSync(path.join(consoleDir, 'src/components/magic-kb-browser.vue'), 'utf8');
    const tabs = src.indexOf('class="kb-tabs"');
    const emptyState = src.indexOf('kb.selectDataset');
    assert.ok(tabs > -1 && emptyState > -1, '缺少 tab 栏或空态');
    // tab 栏必须在空态守卫之前渲染（常驻），且空态只对非 setting 视图生效
    assert.ok(tabs < emptyState, 'tab 栏被 selectedDataset 空态守卫包住了');
    assert.ok(
        /activeView !== 'setting' && !selectedDataset/.test(src),
        '空态守卫未排除 setting 视图'
    );
});
