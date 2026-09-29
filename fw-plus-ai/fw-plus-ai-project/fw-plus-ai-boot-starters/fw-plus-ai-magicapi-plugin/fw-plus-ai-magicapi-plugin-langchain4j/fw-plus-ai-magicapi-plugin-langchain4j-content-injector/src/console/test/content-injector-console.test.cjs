/**
 * 控制台静态校验：资源类型、执行接口路径与 i18n 键必须与后端/模板一致
 * （本模块由 system-message 模板改写而来，用断言防住漏改的 system-message 残留）。
 */
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');
const {pathToFileURL} = require('node:url');

const consoleDir = path.resolve(__dirname, '..');
const RESOURCE_TYPE = 'langchain4j-content-injector';

const read = (relative) => fs.readFileSync(path.join(consoleDir, relative), 'utf8');

test('index.js 注册资源类型与工具栏组件', () => {
    const index = read('src/index.js');
    assert.ok(index.includes(`type: '${RESOURCE_TYPE}'`), 'index.js 缺少资源类型注册');
    assert.ok(index.includes('MagicLangchain4jContentInjectorInfo'), 'index.js 缺少工具栏组件绑定');
    assert.ok(!index.includes('system-message'), 'index.js 仍有 system-message 残留');
});

test('service 指向 content-injector 的执行测试接口', () => {
    const service = read('src/service/magic-langchain4j-content-injector.js');
    assert.ok(service.includes('/langchain4j/content-injector/execute'), '执行测试接口路径不对');
    assert.ok(!service.includes('system-message'), 'service 仍有 system-message 残留');
    assert.ok(service.includes("$i('langchain4j.type.content-injector')"), '资源显示名未指向 content-injector');
});

test('中英文案齐全且无模板残留', async () => {
    const zh = (await import(pathToFileURL(path.join(consoleDir, 'src/i18n/zh-cn.js')))).default;
    const en = (await import(pathToFileURL(path.join(consoleDir, 'src/i18n/en.js')))).default;

    for (const [lang, i18n] of [['zh-cn', zh], ['en', en]]) {
        assert.ok(i18n.langchain4j.type['content-injector'], lang + ' 缺少类型名');
        for (const key of ['title', 'name', 'path', 'params', 'description']) {
            assert.ok(i18n.langchain4j.form[key], lang + ' 缺少 form.' + key);
        }
        for (const key of ['name', 'path', 'description']) {
            assert.ok(i18n.langchain4j.hint[key], lang + ' 缺少 hint.' + key);
        }
        assert.ok(!i18n.langchain4j.type['system-message'], lang + ' 仍有 system-message 类型名');
    }
});

test('组件文件名与引用一致', () => {
    const info = read('src/components/magic-langchain4j-content-injector-info.vue');
    assert.ok(info.includes('./magic-langchain4j-content-injector-params.vue'), 'info 未引用 params 组件');
    assert.ok(info.includes('./magic-langchain4j-content-injector-description.vue'), 'info 未引用 description 组件');
    for (const name of ['magic-langchain4j-content-injector-params.vue',
        'magic-langchain4j-content-injector-description.vue']) {
        assert.ok(fs.existsSync(path.join(consoleDir, 'src/components', name)), '缺少组件 ' + name);
    }
});
