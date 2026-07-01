<template>
    <magic-panel-toolbar :toolbars="toolbars"/>
</template>
<script setup>
import $i from '../../../scripts/i18n.js'
const props = defineProps({
    value: Array,
    index: Number
})
const emit = defineEmits(['update:index'])
const toolbars = [{
    title: $i('message.addRow'),
    icon: 'plus',
    onClick(){
        props.value.push({})
        emit('update:index', props.value.length - 1)
    }
},{
    title: $i('message.removeRow'),
    icon: 'minus',
    onClick(){
        if(props.value.length > 0){
            props.value.splice(props.index == -1 ? props.value.length - 1 : props.index, 1)
            emit('update:index', -1)
        }
    }
}]
</script>