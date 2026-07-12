<template>
    <div class="magic-debug">
        <magic-panel-toolbar :toolbars="toolbars"/>
        <div class="magic-debug-variables">
            <magic-tree v-if="opened.variables" :data="opened.variables">
                <template v-slot:folder="{ item }">
                    <magic-icon :icon="item.opened ? 'arrow-bottom': 'arrow-right'" @click.stop="item.opened = !item.opened"/>
                    <svg v-if="item.dataType === 'array'" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg"><path d="M201.472 320h-49.6V129.408l-59.2 18.368v-40.32L196.16 70.4h5.312V320z m50.816 320.128H81.024v-33.92l80.832-86.208c6.016-6.656 11.136-12.672 15.36-18.176a133.76 133.76 0 0 0 10.112-15.104 57.728 57.728 0 0 0 5.504-12.8 42.88 42.88 0 0 0 1.6-11.392c0-11.136-2.816-19.84-8.384-26.24-5.632-6.272-13.632-9.472-24.064-9.472a36.224 36.224 0 0 0-15.488 3.2 31.808 31.808 0 0 0-11.328 8.704 39.232 39.232 0 0 0-6.976 13.312 56.32 56.32 0 0 0-2.368 16.832h-49.792c0-11.2 1.984-21.76 6.016-31.744 3.968-9.984 9.792-18.624 17.28-26.112 7.616-7.424 16.704-13.312 27.392-17.664 10.688-4.352 22.784-6.528 36.16-6.528 13.12 0 24.768 1.664 34.88 4.928 10.176 3.2 18.624 7.872 25.536 13.952 6.848 6.08 12.032 13.504 15.616 22.336 3.52 8.768 5.312 18.688 5.312 29.696 0 8.32-1.28 16.384-3.968 24.192-2.56 7.744-6.336 15.552-11.136 23.296a190.08 190.08 0 0 1-17.408 23.68 528.64 528.64 0 0 1-22.784 24.768l-44.096 46.464h107.456v40zM131.456 812.8h26.432c12.608 0 21.888-3.136 27.968-9.408a34.56 34.56 0 0 0 9.088-25.088 38.656 38.656 0 0 0-2.048-12.8 26.304 26.304 0 0 0-16.832-16.704 42.112 42.112 0 0 0-14.912-2.432 41.792 41.792 0 0 0-12.928 1.984 33.664 33.664 0 0 0-10.688 5.568 25.6 25.6 0 0 0-9.856 20.608H78.08a60.8 60.8 0 0 1 6.4-28.032c4.352-8.448 10.176-15.552 17.536-21.44 7.424-5.888 16-10.496 25.984-13.76 9.984-3.2 20.608-4.864 32-4.864 12.544 0 24 1.472 34.368 4.48 10.368 3.072 19.2 7.616 26.688 13.632 7.424 5.952 13.184 13.44 17.28 22.272 4.16 8.896 6.208 19.2 6.208 30.848a52.8 52.8 0 0 1-9.856 30.72 67.968 67.968 0 0 1-28.032 22.976 62.08 62.08 0 0 1 31.488 22.656 61.44 61.44 0 0 1 10.368 35.712 66.56 66.56 0 0 1-25.408 54.336c-7.872 6.208-17.28 10.88-28.032 14.208a121.024 121.024 0 0 1-66.368 0.64 81.92 81.92 0 0 1-27.2-13.248 68.032 68.032 0 0 1-19.2-22.4 66.304 66.304 0 0 1-7.296-32h49.6a29.824 29.824 0 0 0 10.368 22.912 36.48 36.48 0 0 0 11.584 6.592 48.64 48.64 0 0 0 30.4 0 30.336 30.336 0 0 0 19.392-17.792 46.208 46.208 0 0 0-0.256-31.04 28.544 28.544 0 0 0-8.192-11.584 34.304 34.304 0 0 0-12.928-6.592 63.104 63.104 0 0 0-17.088-2.176h-26.432V812.8z" fill="#9AA7B0"></path><path d="M384 128h512v192H384zM384 768h512v192H384zM384 448h512v192H384z" fill="#40B6E0" fill-opacity=".7"></path></svg>
                    <svg v-else viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg"><path d="M192 128h640v192H192zM192 384h640v192H192zM192 640h640v192H192z" fill="#F4AF3D" fill-opacity=".6"></path></svg>
                    <label class="key">{{ item.name }}</label>
                    <label class="separator">=</label>
                    <span v-if="item.size" class="object-type">({{ item.size }})</span>
                    <span v-if="item.type" class="object-type">{{ `\{${item.type}\}` }}</span>
                </template>
                <template v-slot:file="{ item }">
                    <magic-icon icon="empty"/>
                    <svg v-if="item.dataType === 'number'" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg"><path d="M128 128h768v768H128z" fill="#40B6E0" fill-opacity=".6" p-id="8251"></path><path d="M691.968 704V422.912l-62.464 15.36L613.12 373.76l102.912-30.72h53.76V704h-77.824z m-284.288 6.144c-95.744 0-158.72-80.384-158.72-184.32V524.8c0-103.936 64.512-185.344 159.744-185.344 94.72 0 158.72 80.384 158.72 184.32V524.8c0 103.936-64 185.344-159.744 185.344z m1.408-71.168c47.616 0 77.824-48.128 77.824-113.152V524.8c0-65.024-31.744-114.176-78.848-114.176S330.24 458.24 330.24 523.776V524.8c0 65.536 31.232 114.176 78.848 114.176z" fill="#231F20" fill-opacity=".7" p-id="8252"></path></svg>
                    <svg v-else viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg"><path d="M192 128h640v192H192zM192 384h640v192H192zM192 640h640v192H192z" fill="#F4AF3D" fill-opacity=".6"></path></svg>
                    <label class="key">{{ item.name }}</label>
                    <label class="separator">=</label>
                    <span class="magic-data-type" :class="item.dataType">{{ item.value }}</span>
                    <span class="object-type" v-if="item.type && !item.isNull"> ({{ item.type }})</span>
                </template>
            </magic-tree>
            <magic-empty v-else :text="$i('message.empty', $i('message.variable'))" />
        </div>
    </div>
</template>
<script setup>
import { computed, inject, ref } from 'vue'
import bus from '../../../scripts/bus.js'
import $i from '../../../scripts/i18n.js'
import Message from '../../../scripts/constants/message.js'
import { processTree } from '../../../scripts/utils.js'
const opened = inject('opened')
const disabled = computed(() => !opened.value.variables)

const toolbars = ref([{
    title: $i('editor.tooltip.resume') + '(F8)',
    icon: 'continue',
    disabled,
    onClick(){
        bus.$emit(Message.DEBUG_CONTINUE)
    }
}, {
    title: $i('editor.tooltip.stepInto') + '(F6)',
    icon: 'step-over',
    disabled,
    onClick(){
        bus.$emit(Message.DEBUG_SETPINTO)
    }
}, {
    title: $i('resource.header.expand'),
    icon: 'expand-all',
    disabled,
    onClick: () => processTree(opened.value.variables, it => it.opened = true)
}, {
    title: $i('resource.header.collapse'),
    icon: 'collapse-all',
    disabled,
    onClick: () => processTree(opened.value.variables, it => it.opened = false)
}])
</script>
<style scoped>
.magic-panel-toolbar :deep(.magic-icon-continue){
    fill: var(--icon-debug-color)
}
.magic-panel-toolbar :deep(.magic-icon-step-over){
    fill: var(--icon-step-color)
}
.magic-debug{
    display: flex;
    flex: 1;
}
.magic-debug .magic-debug-variables{
    flex: 1;
    background-color: var(--navbar-body-background-color);
    position: relative;
    overflow: auto;
}
.magic-debug :deep(.magic-tree){
    position: absolute;
    width: 100%;
    height: 100%;
}
.magic-debug :deep(.magic-tree .magic-tree-item){
    height: auto;
}
.magic-debug .object-type{
    color: var(--resource-span-color);
}
.magic-debug label.key{
    color: var(--data-type-key-color);
    padding-left: 3px;
}
.magic-debug label.separator{
    padding: 0 5px;
}
.magic-debug svg{
    display: inline-block;
    width: 16px;
    height: 16px;
    vertical-align: middle;
}
</style>