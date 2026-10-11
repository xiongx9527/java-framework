/**
 * 本类型 console 元数据与后端 provider 工厂的交叉校验（解析后端源码为权威口径）。
 */
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const {pathToFileURL} = require('node:url');

const consoleDir = path.resolve(__dirname, '..');
const pluginDir = path.resolve(consoleDir, '../../../../../..',
    'fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/langchain4j');
const factoryDir = path.join(pluginDir, 'factory');

const TARGET_TYPE = 'langchain4j-embedding-store';
const TARGET_METHOD = 'buildEmbeddingStore';
const READER_CALL = /\bo\.(required|string|integer|longValue|dbl|floatValue|bool|enumValue|duration|stringList)\(\s*"([^"]+)"/g;

function javaFiles(dir) {
    const result = [];
    for (const entry of fs.readdirSync(dir, {withFileTypes: true})) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            result.push(...javaFiles(full));
        } else if (entry.name.endsWith('.java')) {
            result.push(full);
        }
    }
    return result;
}

/** 提取某个 buildXxx 方法体（大括号配对） */
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
    for (const file of javaFiles(factoryDir)) {
        if (file.endsWith('EmbeddingStoreDimensions.java')) continue;
        const src = fs.readFileSync(file, 'utf8');
        const declared = /public String provider\(\)\s*\{\s*return "([^"]+)"/.exec(src);
        if (!declared) {
            continue;
        }
        const providers = [declared[1]];

        const body = bodyOf(src, TARGET_METHOD);
        if (body === null) continue;
        const names = [];
        let m;
        READER_CALL.lastIndex = 0;
        while ((m = READER_CALL.exec(body)) !== null) {
            if (!names.includes(m[2])) names.push(m[2]);
        }
        if (TARGET_TYPE === 'langchain4j-embedding-store' && /\bdimension\b/.test(body)) {
            names.push('dimension');
        }
        for (const provider of providers) {
            result[provider] = names;
        }
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

test('元数据表与后端 langchain4j-embedding-store 工厂读取的 options 键一致', () => {
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
    assert.ok(index.includes('/langchain4j/embedding-store/datasource/test') === false, '连接测试路径应由 service 拼接');
});
