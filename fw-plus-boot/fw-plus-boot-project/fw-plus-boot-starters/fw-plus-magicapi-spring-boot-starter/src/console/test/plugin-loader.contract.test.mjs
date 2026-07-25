import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const source = readFile(fileURLToPath(new URL('../src/components/magic-editor.vue', import.meta.url)), 'utf8')

test('插件加载向工厂传递当前插件元数据', async () => {
    const editor = await source

    assert.match(editor, /pluginData:\s*plugin/)
})

test('插件加载仅在工厂结果有效时加入集合并通知变更', async () => {
    const [editor, message] = await Promise.all([
        source,
        readFile(fileURLToPath(new URL('../src/scripts/constants/message.js', import.meta.url)), 'utf8')
    ])

    assert.match(editor, /const\s+pluginInstance\s*=\s*window\[plugin\.globalName\]\(/)
    assert.match(editor, /if\s*\(pluginInstance\)\s*\{\s*constants\.PLUGINS\.push\(pluginInstance\)\s*bus\.\$emit\(Message\.PLUGINS_CHANGED\)/s)
    assert.match(message, /PLUGINS_CHANGED:/)
})
