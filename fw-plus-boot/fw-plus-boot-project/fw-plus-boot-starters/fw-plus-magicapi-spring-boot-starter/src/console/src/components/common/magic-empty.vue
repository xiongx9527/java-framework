<template>
    <div class="magic-empty">
        <p>
            {{ text }}
        </p>
    </div>
</template>
<script setup>
import $i from '../../scripts/i18n.js'
defineProps({
    text: {
        type: String,
        default: $i('message.nodata')
    }
})
</script>
<style scoped>
.magic-empty{
    width: 100%;
    height: 100%;
    position: relative;
}
.magic-empty p{
    text-align: center;
    width: 100%;
    height: 24px;
    line-height: 24px;
    top: 50%;
    position: relative;
    margin-top: -12px;
}
</style>