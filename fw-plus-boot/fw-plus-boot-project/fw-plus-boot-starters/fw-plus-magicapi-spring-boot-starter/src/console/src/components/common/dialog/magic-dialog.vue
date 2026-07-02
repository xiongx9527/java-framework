<template>
    <teleport to=".magic-editor" v-if="value">
        <div class="magic-dialog" tabindex="1" :class="(shade ? 'magic-dialog__shade' : '') + (className ? ' ' + className : '')" @mousemove="onMousemove" @mouseup="onMouseup" @keydown="onKeydown">
            <div class="magic-dialog-main" :style="{ position, top, left, width, height,'max-width': maxWidth }" ref="dialog">
                <div class="magic-dialog-header none-select" :class="{ moveable }" @mousedown="onMousedown">
                    {{ title }}
                    <span v-if="showClose" @mousedown.stop="close">
                        <magic-icon icon="close"/>
                    </span>
                </div>
                <div class="magic-dialog-body" :style="{padding,'max-height': maxHeight,height: contentHeight, overflow}">
                    <template v-if="content">
                    {{ content }}
                    </template>
                    <slot v-else></slot>
                </div>
            </div>
        </div>
    </teleport>
</template>
<script setup>
import { onUpdated, ref } from 'vue'
const props = defineProps({
    value: {
        type: Boolean,
        default: true
    },
    className: String,
    showClose: {
        type: Boolean,
        default: true,
    },
    shade: {
        type: Boolean,
        default: true
    },
    padding: {
        type: String,
        default: '5px 10px'
    },
    height: {
        type: String,
        default: 'auto'
    },
    width: {
        type: String,
        default: 'auto'
    },
    moveable: {
        type: Boolean,
        default: true
    },
    overflow: {
        type: String,
        default: 'auto'
    },
    top: String,
    maxHeight: String,
    maxWidth: String,
    contentHeight: String,
    title: String,
    content: String,
    onClose: Function
})
const emit = defineEmits(['update:value', 'onClose'])
const top = ref(props.top || 'auto')
const left = ref('auto')
const position = ref('relative')
const dialog = ref(null)
function hide(){
    emit('update:value', false)
}
const close = () =>{
    emit('close')
    if (typeof props.onClose === 'function') {
        props.onClose()
    }
    hide()
}
let start
let moving = false
let rect;
let maxRect;
function onMousedown(e) {
    if(props.moveable) {
        start = e
        rect = e.target.parentNode.getBoundingClientRect()
        maxRect = document.getElementsByClassName('magic-editor')[0].getBoundingClientRect()
        moving = true
    }
}
function onMousemove(e){
    if(props.moveable && moving) {
        let y = Math.min(Math.max(rect.top + (e.pageY - start.pageY), maxRect.y), maxRect.y + maxRect.height - rect.height)
        let x = Math.min(Math.max(rect.left + (e.pageX - start.pageX), maxRect.x), maxRect.x + maxRect.width - rect.width)
        top.value = y + 'px'
        left.value = x + 'px'
        position.value = 'absolute'
    }
    
}
let updated = false
const updataSize = () => {
    if(!updated && dialog.value){
        position.value = 'absolute'
        left.value = dialog.value.getBoundingClientRect().x + 'px'
    }
    updated = true
}
if(props.top){
    onUpdated(updataSize)
}
const onKeydown = e => {
    if(props.showClose && e.keyCode === 27){
        close()
    }
}
function onMouseup(e){
    moving = false
}
defineExpose({ close })
</script>

<style scoped>
.magic-dialog{
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 999999;
    text-align: center;
}
.magic-dialog__shade{
    background-color: rgba(0,0,0,.3);
}
.magic-dialog::before{
    content: "";
    display: inline-block;
    height: 100%;
    vertical-align: middle;
    margin-right: -0.25em;
}
.magic-dialog-main{
    background: var(--main-background-color);
    border: 1px solid var(--dialog-border-color);
    display: inline-block;
    vertical-align: middle;
    position: relative;
    min-width: 250px;
    box-shadow: 0 0 8px var(--dialog-shadow-color);
    max-width: 800px;
    color: var(--main-color);
    transition: none;
}
.magic-dialog-header{
    height: 30px;
    line-height: 30px;
    padding-left: 30px;
    padding-right: 75px;
    background-position: 7px 7px;
    background-repeat: no-repeat;
    text-align: left;
}
.magic-dialog-header.moveable{
    cursor: move;
}
.magic-dialog-header span{
    display: inline-block;
    width: 30px;
    position: absolute;
    right: 0;
    text-align: center;
    cursor: pointer;
    font-size: 12px;
    height: 30px;
    line-height: 30px;
}
.magic-dialog-header span:hover{
    background-color: #e81123;
}
.magic-dialog-header span:hover .magic-icon{
    fill: #fff
}
.magic-dialog-body{
    text-align: left;
    word-break: break-all;
}
.magic-dialog :deep(.magic-loading){
    min-width: 200px;
    min-height: 200px;
}
</style>