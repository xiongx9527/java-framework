<template>
    <magic-monaco-editor v-model:value="info.dataStructureJson" language="json" @change="onChange"/>
</template>
<script setup>
import { inject } from 'vue'

const info = inject('info')
const opened = inject('opened')

const getType = (value) => {
    if (value === null || value === undefined) return 'String'
    if (Array.isArray(value)) return 'Array'
    if (typeof value === 'object') return 'Object'
    if (typeof value === 'number') return Number.isInteger(value) ? 'Integer' : 'Float'
    if (typeof value === 'boolean') return 'Boolean'
    return 'String'
}

const buildDefNode = (value, srcNode, keyName) => {
    const type = getType(value)
    const children = []
    if (type === 'Object') {
        for (const [key, val] of Object.entries(value)) {
            const srcChild = ((srcNode.children || []).find(c => c.name === key) || {})
            children.push(buildDefNode(val, srcChild, key))
        }
    } else if (type === 'Array' && value.length > 0) {
        const srcChild = (srcNode.children || [])[0] || {}
        children.push(buildDefNode(value[0], srcChild, srcNode.name))
    }
    return {
        name: srcNode.name || keyName || '',
        dataType: type,
        required: srcNode.required || false,
        value: type === 'Object' || type === 'Array' ? '' : String(value),
        defaultValue: srcNode.defaultValue || '',
        validateType: srcNode.validateType || '',
        expression: srcNode.expression || '',
        error: srcNode.error || '',
        description: srcNode.description || '',
        children
    }
}

const definition2TreeList = (definition) => {
    const treeList = []
    const appendNode = (node, level) => {
        const item = {
            level, node,
            name: level === 0 ? 'root' : node.name,
            folder: node?.dataType === 'Object' || node?.dataType === 'Array',
            display: true
        }
        if (item.folder) item.expand = true
        treeList.push(item)
    }
    const appendChild = (items, level) => {
        (items || []).forEach(node => {
            appendNode(node, level)
            appendChild(node.children, level + 1)
        })
    }
    appendNode(definition || {}, 0)
    appendChild(definition?.children || [], 1)
    return treeList
}

const onChange = (e) => {
    if (e.isFlush) return
    try {
        const obj = JSON.parse(info.value.dataStructureJson)
        const def = Array.isArray(obj)
            ? obj.map((item, i) => buildDefNode(item, info.value.dataStructureTree?.[i] || {}, ''))
            : [buildDefNode(obj, info.value.dataStructureTree?.[0] || {}, '')]
        info.value.dataStructureTree = def
        opened.value.dataStructureTree = []
        def.forEach(d => {
            opened.value.dataStructureTree.push(...definition2TreeList(d).slice(1))
        })
    } catch (e) {
        // JSON 解析失败时不更新
    }
}
</script>
