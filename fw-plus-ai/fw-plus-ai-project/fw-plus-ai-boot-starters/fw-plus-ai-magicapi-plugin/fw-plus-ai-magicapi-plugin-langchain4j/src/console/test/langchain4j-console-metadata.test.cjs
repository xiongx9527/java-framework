/**
 * 控制台元数据与后端 provider 工厂的交叉校验。
 *
 * 校验目的：元数据表是「数据源类型 × provider → options 字段」的单一事实源，
 * 必须与后端各工厂实际读取的 options 键保持一致，避免出现「后端支持但表单不渲染」
 * 或「表单渲染了后端不读」的漂移。后端源码作为权威口径在此被解析。
 */
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const {pathToFileURL} = require('node:url');

const consoleDir = path.resolve(__dirname, '..');
const projectDir = path.resolve(consoleDir, '../../../../..', 'fw-plus-ai-boot-autoconfigure');
const pluginDir = path.join(projectDir,
    'src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/magicapi/plugin/langchain4j');
const factoryDir = path.join(pluginDir, 'factory');

const METHOD_TO_TYPE = {
    buildChatModel: 'langchain4j-chat-model',
    buildStreamingChatModel: 'langchain4j-streaming-chat-model',
    buildLanguageModel: 'langchain4j-language-model',
    buildStreamingLanguageModel: 'langchain4j-streaming-language-model',
    buildEmbeddingModel: 'langchain4j-embedding-model',
    buildTokenizer: 'langchain4j-tokenizer',
    buildEmbeddingStore: 'langchain4j-embedding-store'
};

const READER_CALL = /\bo\.(required|string|integer|longValue|dbl|floatValue|bool|enumValue|duration|stringList)\(\s*"([^"]+)"/g;
const READER_TO_FIELD_TYPE = {
    required: 'text',
    string: 'text',
    integer: 'number',
    longValue: 'number',
    dbl: 'number',
    floatValue: 'number',
    bool: 'switch',
    enumValue: 'select',
    duration: 'duration',
    stringList: 'tags'
};

const VALID_FIELD_TYPES = ['text', 'number', 'switch', 'select', 'duration', 'tags', 'json'];

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

/** 提取 buildXxx 方法体（按大括号配对） */
function methodBodies(src) {
    const bodies = {};
    const re = /public ([\w<>,\s.]+?) (build\w+)\(([^)]*)\)\s*\{/g;
    let match;
    while ((match = re.exec(src)) !== null) {
        let depth = 1;
        let i = re.lastIndex;
        while (i < src.length && depth > 0) {
            if (src[i] === '{') depth++;
            else if (src[i] === '}') depth--;
            i++;
        }
        bodies[match[2]] = src.slice(re.lastIndex, i - 1);
    }
    return bodies;
}

/** 解析后端工厂：provider → { type → [字段名] } */
function parseBackendFactories() {
    const config = fs.readFileSync(path.join(pluginDir, 'Langchain4jModelFactoryConfiguration.java'), 'utf8');
    const onnxProviders = [...config.matchAll(/new Langchain4jOnnxEmbeddingModelFactory\("([^"]+)"\)/g)]
        .map(m => m[1]);

    const result = {};
    for (const file of javaFiles(factoryDir)) {
        if (file.endsWith('EmbeddingStoreDimensions.java')) continue;
        const src = fs.readFileSync(file, 'utf8');
        const declared = /public String provider\(\)\s*\{\s*return "([^"]+)"/.exec(src);
        const parameterized = /this\.provider = provider/.test(src);
        let providers;
        if (declared) {
            providers = [declared[1]];
        } else if (parameterized) {
            providers = onnxProviders;
        } else {
            continue;
        }

        const bodies = methodBodies(src);
        for (const provider of providers) {
            const byType = {};
            for (const [method, body] of Object.entries(bodies)) {
                const type = METHOD_TO_TYPE[method];
                if (!type) continue;
                const names = [];
                let m;
                READER_CALL.lastIndex = 0;
                while ((m = READER_CALL.exec(body)) !== null) {
                    if (!names.includes(m[2])) names.push(m[2]);
                }
                if (type === 'langchain4j-embedding-store' && /\bdimension\b/.test(body)) {
                    names.push('dimension');
                }
                byType[type] = names;
            }
            result[provider] = byType;
        }
    }
    return result;
}

function parseModelTypes() {
    const src = fs.readFileSync(path.join(pluginDir, 'model', 'Langchain4jModelTypes.java'), 'utf8');
    return [...src.matchAll(/\("(langchain4j-[a-z0-9-]+)"\)/g)].map(m => m[1]);
}

let metadata;
let zhCN;
let en;
test.before(async () => {
    metadata = await import(pathToFileURL(path.join(consoleDir, 'src/metadata/providers.js')));
    zhCN = (await import(pathToFileURL(path.join(consoleDir, 'src/i18n/zh-cn.js')))).default;
    en = (await import(pathToFileURL(path.join(consoleDir, 'src/i18n/en.js')))).default;
});

test('数据源类型与后端 Langchain4jModelTypes 完全一致', async () => {
    const types = (await import(pathToFileURL(path.join(consoleDir, 'src/metadata/types.js'))));
    assert.deepStrictEqual(
        types.DATASOURCE_TYPES.map(types.fullType).sort(),
        parseModelTypes().sort(),
        '前端七类必须与后端枚举一致');
});

test('元数据表覆盖每个 (类型 × provider) 且字段与后端读取的 options 键一致', () => {
    const backend = parseBackendFactories();
    const problems = [];

    for (const [provider, byType] of Object.entries(backend)) {
        for (const [type, keys] of Object.entries(byType)) {
            const declared = metadata.PROVIDER_FIELDS[provider];
            const fields = declared && declared[type];
            if (!fields) {
                problems.push(`${type} 缺少 provider '${provider}' 的字段定义`);
                continue;
            }
            const actual = fields.map(f => f.name);
            const missing = keys.filter(k => !actual.includes(k));
            const extra = actual.filter(k => !keys.includes(k));
            if (missing.length) problems.push(`${type}/${provider} 缺少字段: ${missing.join(', ')}`);
            if (extra.length) problems.push(`${type}/${provider} 多出字段: ${extra.join(', ')}`);
        }
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('元数据表不包含后端未声明的 provider 或类型组合', () => {
    const backend = parseBackendFactories();
    const problems = [];
    for (const [provider, byType] of Object.entries(metadata.PROVIDER_FIELDS)) {
        if (!backend[provider]) {
            problems.push(`元数据表存在后端未注册的 provider: ${provider}`);
            continue;
        }
        for (const type of Object.keys(byType)) {
            if (!backend[provider][type]) {
                problems.push(`元数据表存在后端未支持的组合: ${type}/${provider}`);
            }
        }
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('每个短名在其支持的每个类型下至少可被渲染（含空字段列表）', () => {
    for (const [provider, byType] of Object.entries(parseBackendFactories())) {
        for (const type of Object.keys(byType)) {
            const fields = metadata.optionsFields(type, provider);
            assert.ok(Array.isArray(fields), `${type}/${provider} 应返回字段数组`);
        }
    }
});

test('字段类型取值合法，且字段名都有中英文文案', () => {
    const problems = [];
    for (const [provider, byType] of Object.entries(metadata.PROVIDER_FIELDS)) {
        for (const [type, fields] of Object.entries(byType)) {
            for (const f of fields) {
                if (!VALID_FIELD_TYPES.includes(f.type)) {
                    problems.push(`${type}/${provider}.${f.name} 非法字段类型: ${f.type}`);
                }
                if (!zhCN.langchain4j.field[f.name]) {
                    problems.push(`缺少中文文案: langchain4j.field.${f.name}`);
                }
                if (!en.langchain4j.field[f.name]) {
                    problems.push(`缺少英文文案: langchain4j.field.${f.name}`);
                }
            }
        }
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('provider 短名都有中英文展示名', () => {
    for (const p of Object.keys(metadata.PROVIDER_FIELDS)) {
        assert.ok(zhCN.langchain4j.provider[p], `缺少中文 provider 名: ${p}`);
        assert.ok(en.langchain4j.provider[p], `缺少英文 provider 名: ${p}`);
    }
});

test('embedding-store 组件含 embeddingModelKey 选择项', () => {
    const form = fs.readFileSync(path.join(consoleDir, 'src/components/magic-langchain4j-form.vue'), 'utf8');
    assert.match(form, /info\.embeddingModelKey/, '表单必须绑定 embeddingModelKey');
    assert.match(form, /isStore/, '表单必须识别 embedding-store 类型');
    assert.ok(zhCN.langchain4j.common.embeddingModelKey, '缺少 embeddingModelKey 文案');
    assert.ok(zhCN.langchain4j.hint.embeddingModelKey, '缺少 embeddingModelKey 提示');
});

test('index.js 为七类注册 datasource 且各绑定组件', () => {
    const index = fs.readFileSync(path.join(consoleDir, 'src/index.js'), 'utf8');
    for (const short of ['chat-model', 'streaming-chat-model', 'language-model',
        'streaming-language-model', 'embedding-model', 'tokenizer', 'embedding-store']) {
        assert.match(index, new RegExp("'" + short + "': MagicLangchain4j"), `index.js 缺少 ${short} 组件绑定`);
    }
    assert.match(index, /DATASOURCE_TYPES\.map/, 'index.js 必须按类型集合注册');
});
