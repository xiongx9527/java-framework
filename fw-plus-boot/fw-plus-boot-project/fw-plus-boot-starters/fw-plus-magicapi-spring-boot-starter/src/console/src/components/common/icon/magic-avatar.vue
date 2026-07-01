<template>
    <div class="magic-avatar" :style="style" :title="displayTitle">{{ displayText }}</div>
</template>
<script setup>
import { computed } from 'vue'
import { hash } from '../../../scripts/utils.js'
import $i from '../../../scripts/i18n.js'
const props = defineProps({ user: Object, text: String, size: {
    type: Number,
    default: 22
} })
const colors = ["#1abc9c", "#2ecc71", "#3498db", "#9b59b6", "#16a085", "#27ae60", "#2980b9", "#8e44ad", "#f1c40f", "#e67e22", "#e74c3c", "#eca0f1", "#95a5a6", "#f39c12", "#d35400", "#c0392b", "#bdc3c7", "#7f8c8d"]
const style = computed(() => {
    return {
        width: props.size + 'px',
        height: props.size + 'px',
        lineHeight: props.size + 'px',
        borderRadius: props.size / 2 + 'px',
        backgroundColor: props.text && '#bdc3c7' || colors[hash(`${props.user.cid}`) % colors.length]
    }
})
const displayText = computed(() => props.text || props.user.username.substring(0, 1))
const displayTitle = computed(() => props.user ? `${$i('message.username')}：${props.user.username}\nIP：${props.user.ip || 'unknown'}` : undefined)
</script>

<style scoped>
.magic-avatar{
   color: #fff;
   display: inline-block;
   text-align: center;
}
</style>