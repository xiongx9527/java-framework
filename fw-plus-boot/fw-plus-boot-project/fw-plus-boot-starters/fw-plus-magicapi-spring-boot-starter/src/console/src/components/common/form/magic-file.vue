<template>
	<div class="magic-file">
		<input type="file" style="display: none" ref="file" @change="onFileSelected" :accept="accept" :multiple="multiple">
		<magic-input :readonly="true" style="width: 100%" :placeholder="placeholder" :onClick="() => $refs.file.click()" :value="filename" :border="border"/>
		<magic-icon icon="upload"/>
	</div>
</template>
<script setup>
import { ref } from 'vue'
import $i from '../../../scripts/i18n.js'
const props = defineProps({
	value: [Object, String],
	placeholder: {
		type: String,
		default: $i('message.chooseFile')
	},
	accept: String,
	multiple: {
		type: Boolean,
		default: false
	},
	border: {
		type: Boolean,
		default: true
	}
})
const emit = defineEmits(['update:value'])
const file = ref(null)
const filename = ref(null)
const onFileSelected = () => {
	if (file.value.files[0]) {
		filename.value = Array.from(file.value.files).map(it => it.name).join(',');
	}
	emit('update:value', props.multiple ? file.value.files : file.value.files[0])
}
</script>
<style scoped>
.magic-file{
	width: 100%;
	position: relative;
}
.magic-file .magic-icon{
	position: absolute;
	right: 4px;
	top: 0;
	height: var(--magic-input-height);
	line-height: var(--magic-input-height);
}
</style>