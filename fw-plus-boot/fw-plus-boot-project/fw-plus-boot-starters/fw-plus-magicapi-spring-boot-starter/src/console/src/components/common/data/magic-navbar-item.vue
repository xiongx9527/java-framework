<template>
    <div class="magic-navbar-item">
        <slot/>
    </div>
</template>
<script setup>
// 父级（magic-navbar / magic-main / magic-toolbar / 各插件页面）普遍用 v-bind="配置对象" 的方式
// 传递条目数据，配置对象里带有 component（组件定义）、componentInstance（组件实例代理）等
// 非字符串字段。这些字段只供 magic-navbar 通过 vnode.props 读取，绝不能落到根元素上，
// 否则 Vue 会走 patchAttr -> setAttribute，而组件实例代理的 toString/valueOf 为 undefined，
// 浏览器会抛 "Cannot convert object to primitive value"。
defineOptions({
    inheritAttrs: false
})
const props = defineProps({
    title: String,
    style: Object,
    show: {
        type: Boolean,
        default: true
    },
    to: HTMLElement
})
</script>
<style scoped>
.magic-navbar-item{
    height: 100%;
    width: 100%;
}
</style>