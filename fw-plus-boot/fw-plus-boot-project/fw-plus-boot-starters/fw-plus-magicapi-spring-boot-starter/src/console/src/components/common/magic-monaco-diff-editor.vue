<template>
	<div class="magic-monaco-editor" ref="editor"></div>
</template>
<script>
import * as monaco from 'monaco-editor'
import constants from '../../scripts/constants.js'
import { watch } from 'vue'
export default {
	props: {
		language: { type: String, required: true },
		editorConfig: { type: Object, default: {} },
		value: Array,
	},
	setup() {
		return {
			instance: null,
			instancePromise: null
		}
	},
	mounted(){
		this.instance = monaco.editor.createDiffEditor(this.$refs.editor, {
			...this.editorConfig,
			enableSplitViewResizing: false,
			language: this.language,
			minimap: {
				enabled: false
			},
			folding: false,
			lineDecorationsWidth: 20,
			wordWrap: 'on',
			fixedOverflowWidgets: false,
			fontFamily: constants.EDITOR_FONT_FAMILY,
			fontSize: constants.EDITOR_FONT_SIZE,
			fontLigatures: constants.FONT_LIGATURES,
			renderWhitespace: 'none',
			theme: constants.THEME,
			readOnly: this.readonly === true,
			value: this.value || '',
			automaticLayout: true
		})
		this.instance.setModel({
			original: monaco.editor.createModel(this.value[0], this.language),
			modified: monaco.editor.createModel(this.value[1], this.language)
		})
		watch(() => this.language, () => {
			nextTick(() => this.instance.setModel({
				original: monaco.editor.createModel(this.value[0], this.language),
				modified: monaco.editor.createModel(this.value[1], this.language)
			}))
		})

	},
	methods: {
		getEditorDom(){
			return this.$refs.editor
		},
		getInstance() {
			return this.instance
		},
	},
	watch: {
		value(newVal){
			this.instance.setModel({
				original: monaco.editor.createModel(newVal[0], this.language),
				modified: monaco.editor.createModel(newVal[1], this.language)
			})
		}
	}
}
</script>
<style scoped>
.magic-monaco-editor{
	flex: 1;
	overflow: hidden;
}
</style>