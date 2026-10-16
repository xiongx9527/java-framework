/**
 * MCP 服务实例表单「函数/api 范围字段收敛」回归测试：
 * 函数与 api 区块只保留「包含组」下拉（magic-group-select），不再暴露其余筛选文本字段。
 */
const test = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const path = require('node:path');

const consoleDir = path.resolve(__dirname, '..');
const formPath = path.join(consoleDir, 'src/components/magic-mcp-server-info.vue');
const groupSelectPath = path.join(consoleDir, 'src/components/magic-group-select.vue');

const form = () => fs.readFileSync(formPath, 'utf8');

test('magic-group-select 组件存在', () => {
    assert.ok(fs.existsSync(groupSelectPath), '缺少 magic-group-select.vue');
});

test('表单使用 magic-group-select 承载包含组', () => {
    assert.ok(form().includes('magic-group-select'), '表单未使用 magic-group-select');
});

test('函数与 api 区块不再包含 methods/路径筛选字段', () => {
    const src = form();
    for (const banned of ['includePaths', 'excludePaths', "'methods'", '"methods"']) {
        assert.ok(!src.includes(banned), `表单仍包含被移除字段: ${banned}`);
    }
});

test('包含组与 requireOptIn 保留', () => {
    const src = form();
    assert.ok(src.includes('includeGroups'), '包含组绑定缺失');
    assert.ok(src.includes('requireOptIn'), 'requireOptIn 开关缺失');
});
