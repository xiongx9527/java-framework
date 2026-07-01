<template>
    <ul class="magic-panel-toolbar">
        <li v-for="(toolbar, key) in panelToolbars" :key="key" :data-title="toolbar.title" :data-tooltip-direction="tooltipDirection"
        @click.stop="onClick(toolbar)" :class="{ disabled: toolbar.disabled, selected: toolbar.selected }">
            <magic-icon :icon="toolbar.icon" />
        </li>
    </ul>
</template>
<script setup>
import { ref } from 'vue'
const props = defineProps({ 
    toolbars: Array, 
    tooltipDirection: {
        type: String,
        default: 'right'
    }
})
const panelToolbars = ref(props.toolbars)
const onClick = toolbar => {
    if(toolbar.selectable){
        if(toolbar.disabled !== true) {
            props.toolbars.filter(it => it.selectable).forEach(it => it.selected = false)
            toolbar.selected = toolbar.selected !== true
            toolbar.onSelect && toolbar.onSelect(toolbar.selected)
        }
    } else {
        toolbar.disabled !== true && toolbar.onClick()
    }
}
</script>
<style scoped>
.magic-panel-toolbar{
    width: var(--magic-panel-toolbar-width);
    border-right: 1px solid var(--main-border-color);
    height: 100%;
    text-align: center;
}
.magic-panel-toolbar li{
    cursor: pointer;
    border-radius: 2px;
    width: var(--magic-panel-toolbar-size);
    height: var(--magic-panel-toolbar-size);
    line-height: var(--magic-panel-toolbar-size);
    margin:3px auto;
    font-size: 1em;
}
.magic-panel-toolbar li:not(.disabled):hover,
.magic-panel-toolbar li:not(.disabled).selected{
    background-color: var(--main-hover-icon-background-color);
}
.magic-panel-toolbar li.disabled :deep(.magic-icon > *){
    fill: var(--button-disabled-color);
}
</style>