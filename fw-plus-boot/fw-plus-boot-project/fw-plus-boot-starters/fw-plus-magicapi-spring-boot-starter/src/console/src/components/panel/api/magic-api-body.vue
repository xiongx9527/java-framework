<template>
    <div class="magic-api-body">
        <magic-panel-toolbar :toolbars="toolbars" />
        <div>
            <component :is="component"/>
        </div>
    </div>
    
</template>
<script setup>
import { ref, resolveDynamicComponent } from 'vue'
import $i from '../../../scripts/i18n.js'
const props = defineProps({
    opened: Object
})
const component = ref(resolveDynamicComponent('magic-api-request-body'))
const toolbars = [
    { title: 'Json', selected: true, selectable: true, icon: 'json', onSelect:() => component.value = resolveDynamicComponent('magic-api-request-body')  },
    { title: $i('message.view'), selectable: true, icon: 'structure', onSelect:() => component.value = resolveDynamicComponent('magic-api-request-structure') }
]
</script>
<style scoped>
.magic-api-body {
    display: flex;
	flex: 1;
	height: 100%;
}
.magic-api-body > div{
	background-color: var(--navbar-body-background-color);
	flex: 1;
	overflow: hidden;
	position: relative;
	height: 100%;
    display: flex;
}
</style>
