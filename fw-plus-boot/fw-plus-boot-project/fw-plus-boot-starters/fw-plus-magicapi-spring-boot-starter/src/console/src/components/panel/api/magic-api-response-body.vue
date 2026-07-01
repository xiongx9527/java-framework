<template>
    <magic-monaco-editor v-if="!opened.responseBlob" v-model:value="responseBody" language="json" :readonly="true"/>
    <iframe v-else-if="blobURL" :src="blobURL" @load="onIframeLoad" ref="iframe"/>
    <magic-empty v-else :text="$i('message.empty', $i('message.responseBody'))"/>
</template>
<script setup>
import { computed, inject, ref } from 'vue'
import { download } from '../../../scripts/utils.js'
import $i from '../../../scripts/i18n.js'
const info = inject('info')
const opened = inject('opened')
const iframe = ref(null)
const elementRoot = inject('ELEMENT_ROOT')
const responseBody = computed(() => info.value.responseBody || '')
const blobURL = computed(() => {
    if(opened.value.responseBlob){
        const disposition = opened.value.responseHeaders['content-disposition'];
        if(disposition){
            const filename = disposition.replace(/.*filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/, '$1')
            download(opened.value.responseBlobValue, filename)
            opened.value.responseBlob = false;
            return
        }
        return URL.createObjectURL(opened.value.responseBlobValue)
    }
    return undefined
})
const onIframeLoad = ele => {
    const styles = getComputedStyle(elementRoot.value)
    const iframeStyle = iframe.value.contentWindow.document.body.style;
    iframeStyle.color = styles.getPropertyValue('--main-color')
    iframeStyle.backgroundColor = styles.getPropertyValue('--main-background-color')
    
}
</script>
<style scoped>
iframe{
    width: 100%;
    height: 100%;
    border: 0;
}
.magic-monaco-editor{
	position: absolute;
	left: 0;
	right: 0;
	top: 0;
	bottom: 0;
}
</style>
