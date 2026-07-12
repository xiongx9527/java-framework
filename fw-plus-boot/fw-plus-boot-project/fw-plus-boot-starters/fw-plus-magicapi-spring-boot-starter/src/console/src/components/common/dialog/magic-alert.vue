<template>
    <magic-dialog v-model:value="show" :title="title" ref="dialog" @close="onClose()">
        <pre style="white-space:pre-wrap">{{ message }}</pre>
        <magic-button-group align="center" style="padding: 5px 0">
            <magic-button :value="ok" type="active" @onClick="$refs.dialog.close()"/>
        </magic-button-group>
    </magic-dialog>
</template>
<script setup>
import { ref } from 'vue'
import $i from '../../../scripts/i18n.js'
defineProps({
    title: {
        type: String,
        default: $i('message.tips')
    },
    ok: {
        type: String,
        default: $i('message.ok')
    },
    message: {
        type: String,
        required: true
    },
    onClose: {
        type: Function,
        default: ()=>{}
    }
})
const show = ref(true)
</script>