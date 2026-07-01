<template>
    <div class="magic-word-data-structure" :class="{ collapsed }">
        <magic-panel-toolbar :toolbars="toolbars" />
        <div v-show="!collapsed">
            <component :is="component"/>
        </div>
    </div>
</template>
<script setup>
import { computed, ref, inject } from 'vue'
import MagicWordDataStructureJson from './magic-word-data-structure-json.vue'
import MagicWordDataStructureTree from './magic-word-data-structure-tree.vue'

const $i = inject('i18n.format')
const component = ref(MagicWordDataStructureJson)
const collapsed = ref(false)

const toolbars = computed(() => [
    { title: 'Json', selected: !collapsed.value && component.value === MagicWordDataStructureJson, selectable: true, icon: 'json', onSelect:() => { component.value = MagicWordDataStructureJson; collapsed.value = false } },
    { title: $i('message.view'), selected: !collapsed.value && component.value === MagicWordDataStructureTree, selectable: true, icon: 'structure', onSelect:() => { component.value = MagicWordDataStructureTree; collapsed.value = false } }
])
</script>
<style scoped>
.magic-word-data-structure {
    display: flex;
    flex: 1;
    height: 100%;
}
.magic-word-data-structure > div {
    background-color: var(--navbar-body-background-color);
    flex: 1;
    overflow: hidden;
    position: relative;
    height: 100%;
    display: flex;
}
</style>
