<template>
    <magic-dialog v-model:value="show" :title="title" ref="dialog" @close="onClose()">
        <pre v-html="message" style="white-space: pre-wrap;"></pre>
        <magic-button-group align="right" style="padding: 5px 0">
            <magic-button :value="ok" type="active" @onClick="() => { success(); $refs.dialog.close(); }"/>
            <magic-button :value="cancel" @onClick="$refs.dialog.close()"/>
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
    cancel: {
        type: String,
        default: $i('message.cancel')
    },
    message: {
        type: String,
        required: true
    },
    success: {
        type: Function,
        default: () => {}
    },
    onClose: {
        type: Function,
        default: ()=>{}
    }
})
const show = ref(true)
</script>