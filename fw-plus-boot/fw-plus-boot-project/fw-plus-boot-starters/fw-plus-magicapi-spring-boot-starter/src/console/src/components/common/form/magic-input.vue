<template>
	<input class="magic-input" :class="{'magic-input__border': border}" :value="value" :placeholder="placeholder" :readonly="readonly" :style="style" :type="type"
		   @input="e => $emit('update:value', e.target.value)" :autocomplete="autocomplete"
		   @keypress.enter.stop="onEnter"/>
</template>
<script setup>
import { computed } from "@vue/reactivity"

const props = defineProps({
	value: [String, Number, Object],
	placeholder: String,
	defaultValue: [String, Number, Object],
	readonly: {
		type: Boolean,
		default: false
	},
	type: {
		type: String,
		default: 'text'
	},
	width: {
		type: String,
		default: '100%'
	},
	border: {
		type: Boolean,
		default: true
	},
	onEnter: {
		type: Function,
		default: () => {}
	}
})
const autocomplete = computed(() => props.type === 'password' ? 'new-password' : null)
const emit = defineEmits(['update:value'])
if(!props.value && props.defaultValue){
	emit('update:value', props.defaultValue)
}
const style = {
	width: props.width
}
</script>
<style scoped>
.magic-input{
	outline: 0;
	height: var(--magic-input-height);
	line-height: var(--magic-input-height);
	border-radius: 0;
	outline: 0;
	border: 1px solid transparent;
	border-radius: var(--input-border-radius);
	padding:0;
	margin:0;
	padding-left: 5px;
	background-color: var(--input-background-color);
	color: var(--main-color);
}
.magic-input:autofill {
	background: var(--input-background-color);
}
.magic-input:-webkit-autofill,
.magic-input:-webkit-autofill:hover,
.magic-input:-webkit-autofill:focus,
.magic-input:-webkit-autofill:active {
	transition: background-color 5000s;
	-webkit-text-fill-color: var(--main-color) !important;
}
.magic-input:focus{
	border-color: var(--input-focus-color);
	border-width: var(--input-focus-border-width);
}
.magic-input__border{
	border-color: var(--input-border-color);
}
.magic-input::-webkit-outer-spin-button,
.magic-input::-webkit-inner-spin-button {
	-webkit-appearance: none;
}
.magic-input[type="number"] {
	-moz-appearance: textfield;
}
</style>
