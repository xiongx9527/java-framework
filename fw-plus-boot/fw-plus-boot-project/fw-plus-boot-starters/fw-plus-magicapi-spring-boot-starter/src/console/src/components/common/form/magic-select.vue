<template>
    <div :class="{ inputable, border }" :style="{ width }" class="magic-select not-select" @click.stop="showList($refs.container, $refs.selectList)" ref="container">
        <span v-if="!inputable">{{ showText }}</span>
        <input v-if="inputable" ref="input" v-model="inputValue" autocomplete="off" type="text" @input="(e) => triggerSelect(e.target.value)" :placeholder="placeholder"/>
        <ul v-show="visible" :style="{ width: selectWidth, marginTop, marginLeft, bottom}" ref="selectList">
            <li v-for="item in options" :key="item.value" @click.stop="triggerSelect(item.value)">
            {{ item.text }}
            </li>
        </ul>
		<magic-icon icon="arrow-bottom"/>
    </div>
</template>
<script setup>
import { computed, inject, nextTick, onUnmounted, ref } from 'vue'
const props = defineProps({
	value: [ Object, String, Number ],
	options: Array,
	defaultSelect: [ Object, String, Number ],
	width: String,
	border: {
		type: Boolean,
		default: true
	},
	placeholder: String,
	inputable: {
		type: Boolean,
		default: false
	}
})
const bodyClickEventListener = () => visible.value = false
document.body.addEventListener('click', bodyClickEventListener)
onUnmounted(() => document.body.removeEventListener('click',bodyClickEventListener))
const marginTop = ref('-2px')
const visible = ref(false)
const selectWidth = ref('auto')
const bottom = ref('auto')
const marginLeft = ref('0px')
const emit = defineEmits(['update:value', 'select'])
const inputValue = computed({
	get() {
		return props.value
	},
	set(value) {
		emit('update:value', value)
	}
})
const showText = computed(() => {
    const find = props.options.find(it => it.value === props.value) || props.options.find(it => it.value === props.defaultSelect)
    return find && find.text || ''
})
const triggerSelect = value => {
    emit('update:value', value)
	emit('select', value)
    visible.value = false
}
const root = inject('ELEMENT_ROOT')
const getMarginTop = (el, top) => top + el.scrollTop > 0 ? el.scrollTop : el.parentElement ? getMarginTop(el.parentElement, top + el.scrollTop) : el.scrollTop
const showList = (container, selectList) => {
	marginTop.value = -getMarginTop(container, 0) - 1 + 'px';
	bottom.value = 'auto';
	visible.value = true;
	nextTick(() => {
		marginLeft.value = -(window.pageXOffset + 1) + 'px';
		selectWidth.value = container.clientWidth + 'px';
		let height = selectList.offsetHeight;
		let top = selectList.offsetTop;
		
		if (top + height + 20 > root.value.offsetTop + root.value.offsetHeight) {
			marginTop.value = '-2px'
			bottom.value = '0px'
		}
	})
    
}
</script>
<style scoped>
.magic-select {
	position: relative;
	display: inline-block;
	background: var(--select-background-color);
	height: var(--magic-input-height);
	line-height: var(--magic-input-height);;
	width: 80px;
	font-size: 12px;
	text-align: left;
}

.magic-select.inputable {
	background: var(--select-inputable-background-color);
	border-color: var(--select-inputable-border-color);
}
.magic-select.border {
	border: 1px solid var(--input-border-color);
}

.magic-select input {
	background: none;
	border: none;
	height: var(--magic-input-height);;
	line-height: var(--magic-input-height);;
	border-radius: 0;
	outline: 0;
	padding-left: 5px;
	width: 100%;
	color: var(--main-color)
}

.magic-select span {
	height: var(--magic-input-height);;
	line-height: var(--magic-input-height);;
	border-radius: 0;
	outline: 0;
	padding-left: 5px;
}

.magic-select:hover:not(.inputable) {
	background: var(--select-hover-background-color);
}

.magic-select .magic-icon{
	position: absolute;
	right: 5px;
	top: 50%;
	transform: translateY(-50%);
	height: 1.3em;
}

.magic-select ul {
	display: block;
	position: fixed;
	z-index: 10;
	background: var(--select-option-background-color);
	border: 1px solid var(--select-option-border-color);
	margin-top: -2px;
	padding: 0px;
	box-sizing: content-box;
	margin-left: -1px;
}

.magic-select ul li {
	padding: 0 5px;
	text-align: left;
	width: 100% !important;
	height: var(--magic-input-height);;
	text-overflow: ellipsis;
	word-break: keep-all;
	text-align: left;
}

.magic-select ul li:hover {
	background: var(--select-option-hover-background-color);
	color: var(--select-option-hover-color);
}
</style>