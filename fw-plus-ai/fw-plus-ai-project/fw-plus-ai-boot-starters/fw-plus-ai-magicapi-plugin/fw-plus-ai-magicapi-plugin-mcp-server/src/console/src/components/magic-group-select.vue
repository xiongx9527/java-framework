<template>
  <div class="magic-group-select" :style="{width}" @click.stop="toggle">
    <span :class="{placeholder: !showText}">{{ showText || placeholder }}</span>
    <magic-icon icon="arrow-bottom"/>
    <ul v-show="visible" class="magic-group-select-list" @click.stop>
      <li v-for="g in groups" :key="g.path" @click.stop="toggleGroup(g)">
        <magic-checkbox :value="g.selected"/>
        <label>{{ g.name }}</label>
        <span v-if="g.path && g.path !== g.name">({{ g.path }})</span>
      </li>
      <li v-if="!groups.length" class="magic-group-select-empty">{{ emptyText }}</li>
    </ul>
  </div>
</template>
<script setup>
import {computed, inject, onUnmounted, ref} from 'vue'

const props = defineProps({
  // 选中的组路径数组（includeGroups 值，与后端 getGroupPath 匹配语义一致）
  value: Array,
  // 资源类型：function | api
  type: String,
  width: String,
  placeholder: String,
  emptyText: String
})
const emit = defineEmits(['update:value'])
const resources = inject('resources', null)
const visible = ref(false)
const groups = ref([])

// 从资源树递归收集分组节点（名称 + 路径）
const collect = (nodes, result) => {
  nodes && nodes.forEach(node => {
    if (node.folder) {
      result.push({name: node.name, path: node.path || node.id, selected: (props.value || []).includes(node.path || node.id)})
    }
    collect(node.children, result)
  })
}

const loadGroups = () => {
  const result = []
  const find = (items) => {
    items && items.forEach(it => {
      if (it.key === props.type) {
        collect(it.tree, result)
      } else if (it.children) {
        find(it.children)
      }
    })
  }
  find(typeof resources === 'function' ? resources() : [])
  groups.value = result
}

const toggle = () => {
  visible.value = !visible.value
  if (visible.value) {
    loadGroups()
  }
}

const toggleGroup = (g) => {
  g.selected = !g.selected
  emit('update:value', groups.value.filter(it => it.selected).map(it => it.path))
}

const showText = computed(() => (props.value || []).join(', '))

const close = () => visible.value = false
document.body.addEventListener('click', close)
onUnmounted(() => document.body.removeEventListener('click', close))
</script>
<style scoped>
.magic-group-select {
  position: relative;
  display: inline-block;
  background: var(--select-background-color);
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  width: 80px;
  font-size: 12px;
  text-align: left;
  border: 1px solid var(--input-border-color);
  cursor: pointer;
}

.magic-group-select span {
  display: inline-block;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  border-radius: 0;
  outline: 0;
  padding-left: 5px;
  padding-right: 20px;
  width: calc(100% - 25px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--main-color)
}

.magic-group-select span.placeholder {
  color: var(--resource-span-color)
}

.magic-group-select .magic-icon {
  position: absolute;
  right: 5px;
  top: 50%;
  transform: translateY(-50%);
  height: 1.3em;
}

.magic-group-select-list {
  display: block;
  position: fixed;
  z-index: 10;
  max-height: 260px;
  overflow: auto;
  background: var(--select-option-background-color);
  border: 1px solid var(--select-option-border-color);
  margin-top: -2px;
  margin-left: -1px;
  padding: 0;
  box-sizing: content-box;
}

.magic-group-select-list li {
  padding: 0 5px;
  text-align: left;
  height: var(--magic-input-height);
  line-height: var(--magic-input-height);
  text-overflow: ellipsis;
  word-break: keep-all;
}

.magic-group-select-list li:hover {
  background: var(--select-option-hover-background-color);
  color: var(--select-option-hover-color);
}

.magic-group-select-list li label {
  cursor: pointer;
}

.magic-group-select-list li span {
  display: inline;
  color: var(--resource-span-color);
  padding: 0;
}

.magic-group-select-empty {
  color: var(--resource-span-color);
}
</style>
