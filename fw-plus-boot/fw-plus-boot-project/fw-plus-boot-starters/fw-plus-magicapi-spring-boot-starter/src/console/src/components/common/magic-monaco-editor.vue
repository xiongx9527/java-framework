<template>
  <div class="magic-monaco-editor" ref="editor"></div>
</template>
<script>
import * as monaco from 'monaco-editor'
import constants from '../../scripts/constants.js'
import $i from '../../scripts/i18n.js'
import { CommandsRegistry } from 'monaco-editor/esm/vs/platform/commands/common/commands'
import { KeybindingsRegistry } from 'monaco-editor/esm/vs/platform/keybinding/common/keybindingsRegistry.js'
import { ContextKeyExpr } from 'monaco-editor/esm/vs/platform/contextkey/common/contextkey.js'
import { nextTick, watch } from 'vue'
import { Parser } from '../../scripts/parsing/parser.js'
import { TokenStream } from '../../scripts/parsing/index.js'
import tokenizer from '../../scripts/parsing/tokenizer.js'
export default {
  props: {
    language: { type: String, required: true },
    editorConfig: { type: Object, default: {} },
    value: String,
    decorations: Array,
    readonly: { type: Boolean, default: false },
    supportBreakpoint: { type: Boolean, default: false },
    matches: String
  },
  setup() {
    return {
      instance: null,
      instancePromise: null
    }
  },
  emits: ['update:value', 'change', 'update:decorations'],
  mounted(){
    this.instance = monaco.editor.create(this.$refs.editor, {
      ...this.editorConfig,
      language: this.language,
      minimap: {
        enabled: false
      },
      folding: true,
      lineDecorationsWidth: this.supportBreakpoint ? 35 : undefined,
      wordWrap: 'on',
      fontFamily: constants.EDITOR_FONT_FAMILY,
      fontSize: constants.EDITOR_FONT_SIZE,
      fontLigatures: constants.FONT_LIGATURES,
      renderWhitespace: 'none',
      theme: constants.THEME,
      readOnly: this.readonly === true,
      value: this.value || '',
      automaticLayout: true
    })
    watch(() => this.language, () => {
      nextTick(() => this.instance.setModel(monaco.editor.createModel(this.instance.getModel().getValue(), this.language)))
    })
    this.instance.onDidChangeModelContent((e) => {
      this.$emit('update:value', this.instance.getValue())
      this.$emit('change', e)
      this.doValidate()

    })
    this.instance.addAction({
      id: 'editor.action.triggerSuggest.extension',
      label: $i('editor.triggerSuggest'),
      precondition: '!suggestWidgetVisible && !markersNavigationVisible && !parameterHintsVisible && !findWidgetVisible',
      run: () => {
        this.instance.trigger(null, 'editor.action.triggerSuggest', {})
      }
    })
    CommandsRegistry.registerCommand('editor.action.appendHead', (_c, text) => {
      if(this.value?.indexOf(text) > -1){
        return
      }
      this.instance.executeEdits('command', [{
        forceMoveMarkers: true,
        text,
        range: new monaco.Range(1, 0, 1, 0)
      }])
      this.instance.setScrollTop(this.instance.getScrollTop() - 22)
    })
    this.instance.addCommand(monaco.KeyMod.Alt | monaco.KeyCode.US_SLASH, () => {
          let triggerParameterHints = this.instance.getAction('editor.action.triggerParameterHints')
          let triggerSuggest = this.instance.getAction('editor.action.triggerSuggest.extension')
          triggerParameterHints.run().then(() => {
            setTimeout(() => {
              if (triggerSuggest.isSupported()) {
                triggerSuggest.run()
              }
            }, 0)
          })
        },
        '!findWidgetVisible && !inreferenceSearchEditor && !editorHasSelection')
    this.initKeys()
    this.initDecorations()

  },
  methods: {
    doValidate(){
      if(this.instance){
        monaco.editor.setModelMarkers(this.instance.getModel(), 'validate', [{}])
      }
      if(this.language === 'magicscript'){
        try{
          new Parser(new TokenStream(tokenizer(this.instance.getValue()))).parse()
        } catch(e){
          if(e.span){
            let line = e.span.getLine()
            monaco.editor.setModelMarkers(this.instance.getModel(), 'validate', [
              {
                startLineNumber: line.lineNumber,
                endLineNumber: line.endLineNumber,
                startColumn: line.startCol,
                endColumn: line.endCol,
                message: e.message,
                severity: monaco.MarkerSeverity.Error
              }
            ])
          }
        }
      }
    },
    getEditorDom(){
      return this.$refs.editor
    },
    getScrollTop(){
      return this.instance && this.instance.getScrollTop()
    },
    setScrollTop(val){
      this.instance && this.instance.setScrollTop(val)
    },
    match(){
      if(this.matches){
        nextTick(() => {
          let mchs = this.instance.getModel().findMatches(this.matches)
          if(mchs && mchs.length > 0){
            this.instance.setSelections(mchs.map(({range}) => {
              return {
                positionColumn: range.endColumn,
                positionLineNumber: range.endLineNumber,
                selectionStartColumn: range.startColumn,
                selectionStartLineNumber: range.startLineNumber
              }
            }))
          }
        })
      }
    },
    initKeys(){
      const updateKeys = [
        ['editor.action.triggerParameterHints', monaco.KeyMod.Alt | monaco.KeyCode.US_SLASH],
        ['editor.action.triggerSuggest', monaco.KeyMod.Alt | monaco.KeyCode.US_SLASH],
        ['toggleSuggestionDetails', monaco.KeyMod.Alt | monaco.KeyCode.US_SLASH, ContextKeyExpr.deserialize('suggestWidgetVisible && textInputFocus')],
        ['editor.action.formatDocument', monaco.KeyMod.CtrlCmd | monaco.KeyMod.Alt | monaco.KeyCode.KEY_L],
        ['editor.action.marker.nextInFiles', monaco.KeyMod.CtrlCmd | monaco.KeyCode.F8]
      ]
      updateKeys.forEach(item => {
        let action = item[0]
        const { handler, when } = CommandsRegistry.getCommand(action) ?? {}
        if (handler) {
          let index = KeybindingsRegistry._coreKeybindings.findIndex(it => it.command === action);
          if(index > 0){
            KeybindingsRegistry._coreKeybindings.splice(index, 1)
          }
          this.instance._standaloneKeybindingService.addDynamicKeybinding(action, item[1], handler, when || item[2])
        }
      })
      KeybindingsRegistry._cachedMergedKeybindings = null
    },
    getInstance() {
      return this.instance
    },
    initDecorations(){
      this.instance.onMouseDown(e => {
        if (e.target.element.classList.contains('codicon')) {
          return
        }
        if (e.target.detail && e.target.detail.offsetX && e.target.detail.offsetX >= 0 && e.target.detail.offsetX <= 65) {
          var line = e.target.position.lineNumber
          if (this.instance.getModel().getLineContent(line).trim() === '') {
            return
          }
          let decorations = this.instance.getLineDecorations(line)
          let decoration = decorations.filter(it => it.options.linesDecorationsClassName === 'breakpoints')
          if (decoration && decoration.length > 0) {
            this.instance.getModel().deltaDecorations([decoration[0].id], [])
          } else {
            this.instance.getModel().deltaDecorations([],[{
              range: new monaco.Range(line, 1, line, 1),
              options: {
                isWholeLine: true,
                linesDecorationsClassName: 'breakpoints',
                className: 'breakpoint-line'
              }
            }])
          }
          this.updateDecoration()

        }
      })
    },
    appendDecoration(decoration){
      const ret = this.instance.deltaDecorations([], decoration)
      this.updateDecoration()
      return ret
    },
    removedDecorations(decorations){
      this.instance.deltaDecorations(decorations, [])
      this.updateDecoration()
    },
    updateDecoration(){
      this.$emit('update:decorations', this.instance.getModel().getAllDecorations())
    }
  },
  watch: {
    value(newVal){
      if(newVal !== this.instance.getValue()){
        this.instance.setValue(newVal || '')
        this.match()
      }
    },
    matches(){
      this.match()
    }
  }
}
</script>
<style scoped>
.magic-monaco-editor{
  flex: 1;
  overflow: hidden;
}
.magic-monaco-editor :deep(.breakpoints){
  background: var(--breakpoints-background-color);
  width: 10px !important;
  height: 10px !important;
  right: 0px !important;
  margin-left: 12px;
  top: 5px;
  border-radius: 5px;
}
.magic-monaco-editor :deep(.debug-line) {
  background: var(--debug-line-background-color);
  color: #fff !important;
}
.magic-monaco-editor :deep(.breakpoint-line) {
  background: var(--breakpoint-line-background-color);
}
</style>
