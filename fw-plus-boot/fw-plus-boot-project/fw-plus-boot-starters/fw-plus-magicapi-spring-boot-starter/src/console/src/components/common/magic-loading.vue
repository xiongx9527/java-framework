<template>
    <div v-if="loading" class="magic-loading" :style="style">
        <p>
            <span class="icon">
                <magic-icon icon="refresh" size="20px"></magic-icon>
            </span>
            {{ loadingText }}
        </p>
    </div>
    <slot v-else />
</template>
<script setup>
import $i from '../../scripts/i18n.js'
const props = defineProps({
    loading: Boolean,
    loadingText: {
        type: String,
        default: $i('message.loading')
    },
    style: Object
})
</script>
<style scoped>
.magic-loading{
    width: 100%;
    height: 100%;
    position: relative;
    top:0;
    left:0;
}
.magic-loading p{
    position: absolute;
    text-align: center;
    width: 100%;
    top: 50%;
    margin-top: -20px;
}
.magic-loading .icon {
    width: 20px;
    margin: 0 auto;
    animation: rotate 1s linear infinite;
    display: block;
}
@keyframes rotate {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}
</style>