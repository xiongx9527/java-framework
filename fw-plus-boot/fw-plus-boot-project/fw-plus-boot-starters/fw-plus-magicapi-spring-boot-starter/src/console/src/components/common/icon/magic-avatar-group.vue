<template>
    <div class="magic-avatar-group" :style="style" v-if="props.users && props.users.length > 0">
        <magic-avatar v-for="(item, key) in userList" :key="key" :user="item" :size="size"/>
        <magic-avatar v-if="props.users.length > userList.length" :text="`${max}+`" :size="size"/>
    </div>
</template>
<script setup>
import { computed } from 'vue'

const props = defineProps({ users: Array, max: Number, size: {
    type: Number,
    default: 22
}})
const style = computed(() => {
    return {
        height: props.size + 'px',
        lineHeight: props.size + 'px'
    }
})
const userList = computed(() => props.users.slice(0, props.max || props.users.length))
</script>
<style scoped>
.magic-avatar-group{
    display: inline-block;
    height: 22px;
    line-height: 22px;
}
.magic-avatar-group .magic-avatar{
    position: relative;
    margin-left: -5px;
}
</style>
