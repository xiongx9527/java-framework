/**
 * 本类型 console 元数据与后端 RedisDataSourceInfo / BaseService.validateField 的交叉校验
 * （解析后端源码为权威口径）。
 */
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const {pathToFileURL} = require('node:url');

const consoleDir = path.resolve(__dirname, '..');
const backendDir = path.resolve(consoleDir, '../../../../../fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/redis');
const entityFile = path.join(backendDir, 'model/RedisDataSourceInfo.java');
const baseServiceFile = path.join(backendDir, 'common/BaseService.java');

const TARGET_TYPE = 'redis-datasource';

/** 后端实体声明的字段（Lombok @Data，private 字段即持久化字段） */
function backendEntityFields() {
    const src = fs.readFileSync(entityFile, 'utf8');
    const names = [];
    let m;
    const re = /^\s*private\s+[\w<>,\s.\[\]]+?\s+(\w+)\s*(?:=[^;]*)?;/gm;
    while ((m = re.exec(src)) !== null) {
        if (!names.includes(m[1])) names.push(m[1]);
    }
    return names;
}

/** 后端必填口径：BaseService.validateField(entity.getXxx(), "xxx") */
function backendRequiredFields() {
    const src = fs.readFileSync(baseServiceFile, 'utf8');
    const names = [];
    let m;
    const re = /validateRequiredField\(\s*entity\.get\w+\(\)\s*,\s*"(\w+)"\s*\)/g;
    while ((m = re.exec(src)) !== null) {
        if (!names.includes(m[1])) names.push(m[1]);
    }
    return names;
}

/** 递归收集对象的全部键路径（如 field.name / hint.required） */
function keyPaths(obj, prefix, out) {
    for (const key of Object.keys(obj)) {
        const value = obj[key];
        const full = prefix ? prefix + '.' + key : key;
        if (value && typeof value === 'object' && !Array.isArray(value)) {
            keyPaths(value, full, out);
        } else {
            out.push(full);
        }
    }
    return out;
}

let metadata;
let zhCN;
let en;
test.before(async () => {
    metadata = await import(pathToFileURL(path.join(consoleDir, 'src/metadata/redis-datasource.js')));
    zhCN = (await import(pathToFileURL(path.join(consoleDir, 'src/i18n/zh-cn.js')))).default;
    en = (await import(pathToFileURL(path.join(consoleDir, 'src/i18n/en.js')))).default;
});

test('元数据字段与后端 RedisDataSourceInfo（含 MagicEntity 的 name）一致', () => {
    // name 继承自 org.ssssssss.magicapi.core.model.MagicEntity，不在本仓源码内
    const backend = backendEntityFields().concat(['name']);
    const actual = metadata.DATASOURCE_FIELDS.map(f => f.name);
    const missing = backend.filter(k => !actual.includes(k));
    const extra = actual.filter(k => !backend.includes(k));
    const problems = [];
    if (missing.length) problems.push('元数据缺少后端字段: ' + missing.join(', '));
    if (extra.length) problems.push('元数据多出后端不存在的字段: ' + extra.join(', '));
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('元数据必填口径与后端 BaseService.validateField 一致', () => {
    const backend = backendRequiredFields().sort();
    const actual = metadata.DATASOURCE_FIELDS.filter(f => f.required).map(f => f.name).sort();
    assert.deepStrictEqual(actual, backend,
        '必填集合不一致: 前端=' + actual.join(',') + ' 后端=' + backend.join(','));
});

test('字段类型属于已声明的集合，敏感字段与枚举字段定义完整', () => {
    const allowed = ['text', 'number', 'select', 'json'];
    const problems = [];
    for (const f of metadata.DATASOURCE_FIELDS) {
        if (!allowed.includes(f.type)) problems.push('未知字段类型: ' + f.name + '=' + f.type);
        if (f.type === 'select' && !(metadata.SELECT_HINTS[f.name] || []).length) {
            problems.push('select 字段缺少候选值: ' + f.name);
        }
        if (f.type === 'json' && f.required) {
            problems.push('json 字段不应声明为必填: ' + f.name);
        }
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('字段都有中英文文案', () => {
    const problems = [];
    for (const f of metadata.DATASOURCE_FIELDS) {
        if (!zhCN.redis.datasource.field[f.name]) problems.push('缺少中文文案: ' + f.name);
        if (!en.redis.datasource.field[f.name]) problems.push('缺少英文文案: ' + f.name);
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('枚举候选值都有中英文展示名', () => {
    const problems = [];
    for (const field of Object.keys(metadata.SELECT_HINTS)) {
        for (const value of metadata.SELECT_HINTS[field]) {
            if (!zhCN.redis.datasource.extra[field][value]) problems.push('缺少中文枚举名: ' + field + '.' + value);
            if (!en.redis.datasource.extra[field][value]) problems.push('缺少英文枚举名: ' + field + '.' + value);
        }
    }
    assert.deepStrictEqual(problems, [], problems.join('\n'));
});

test('中英文案键集合完全一致', () => {
    const zhKeys = keyPaths(zhCN.redis, '', []).sort();
    const enKeys = keyPaths(en.redis, '', []).sort();
    assert.deepStrictEqual(enKeys, zhKeys, '中英文键集合不一致');
});

test('index.js 注册该 datasource type 并绑定组件', () => {
    const index = fs.readFileSync(path.join(consoleDir, 'src/index.js'), 'utf8');
    assert.ok(index.includes("type: '" + TARGET_TYPE + "'"), 'index.js 缺少 type 注册');
    assert.ok(index.includes('MagicRedisDatasourceInfo'), 'index.js 缺少组件绑定');
    assert.ok(index.includes('redis.datasource.common.title'), 'index.js 应使用 common.title 文案键');
});

test('service 不再引用失效代码，并提供保存前校验钩子', () => {
    const service = fs.readFileSync(path.join(consoleDir, 'src/service/magic-redis-datasource.js'), 'utf8');
    assert.ok(!service.includes('com.jugui'), 'service 不应引用不存在的 com.jugui 包');
    assert.ok(!service.includes('nebula-datasource'), 'service 不应残留 nebula-datasource 笔误');
    assert.ok(/validate[\s\S]*info\.properties\s*=\s*parsed/.test(service), 'service.validate 应把解析结果写回 properties');
    assert.ok(service.includes("delete info.propertiesText"), 'service.validate 应在保存前清理 propertiesText');
});

test('组件未直接绑定 propertiesText，回显走 properties 序列化', () => {
    const component = fs.readFileSync(path.join(consoleDir, 'src/components/magic-redis-datasource-info.vue'), 'utf8');
    assert.ok(!component.includes('v-model:value="info.propertiesText"'), '不应直接双向绑定 info.propertiesText');
    assert.ok(component.includes('v-model:value="propertiesText"'), 'JSON 编辑框应绑定转换后的 propertiesText');
    assert.ok(component.includes('JSON.stringify(info.properties'), '未编辑时应回显 properties 序列化值');
});
