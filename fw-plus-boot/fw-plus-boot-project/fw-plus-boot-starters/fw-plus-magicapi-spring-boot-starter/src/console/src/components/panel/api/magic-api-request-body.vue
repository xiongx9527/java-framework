<template>
    <magic-monaco-editor v-model:value="info.requestBody" language="json" @change="onChange"/>
</template>
<script setup>
import { inject } from 'vue'
import { parseJson } from '../../../scripts/parsing/parser.js'
import { definition2TreeList, structureHasChanged } from '../../../scripts/utils.js'
import bus from '../../../scripts/bus.js'
import $i from '../../../scripts/i18n.js'
import Message from '../../../scripts/constants/message.js'
const info = inject('info')
const opened = inject('opened')
const onChange = e => {
    if(e.isFlush){
        return
    }
    const definition = parseJson(info.value.requestBody, info.value.requestBodyDefinition)
    if(definition){
        if(structureHasChanged(info.value.requestBodyDefinition, definition)){
            bus.$emit(Message.NOTIFY, {
                title: $i('message.tips'),
                id: 'requestBodyStructure',
                icon: 'info',
                content: $i('api.structure.content', 'RequestBody'),
                buttons: [{
                    title: $i('api.structure.ok'),
                    onClick: () => {
                        info.value.requestBodyDefinition = definition
                        opened.value.requestBodyTree = definition2TreeList(definition)
                    }
                }, {
                    title: $i('message.cancel'),
                    onClick: () => {}
                }]
            })
            
        }
    }
}
</script>
<style scoped>
.magic-monaco-editor{
	position: absolute;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
}
</style>