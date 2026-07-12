import * as monaco from 'monaco-editor';
import { HighLightOptions } from './high-light.js';
import CompletionItemProvider from './completion.js';
import FoldingRangeProvider from './folding.js';
import SignatureHelpProvider from './signature.js';
import HoverProvider from './hover.js';
import { initMybatis } from './mybatis.js'
import Beautifier from '../beautifier/javascript/beautifier.js'

export const initializeMagicScript = () => {
    initMybatis();
    const language = 'magicscript';
    // 注册语言
    monaco.languages.register({ id: language });
    // 设置语言选项
    monaco.languages.setLanguageConfiguration(language, {
        wordPattern: /(-?\d*\.\d\w*)|([^`~!#%^&*()\-=+[{\]}\\|;:'",.<>/?\s]+)/g,
        brackets: [
            ['{', '}'],
            ['[', ']'],
            ['(', ')'],
        ],
        onEnterRules: [
            {
                // e.g. /** | */
                beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
                afterText: /^\s*\*\/$/,
                action: {
                    indentAction: monaco.languages.IndentAction.IndentOutdent,
                    appendText: ' * '
                }
            },
            {
                // e.g. /** ...|
                beforeText: /^\s*\/\*\*(?!\/)([^\*]|\*(?!\/))*$/,
                action: {
                    indentAction: monaco.languages.IndentAction.None,
                    appendText: ' * '
                }
            },
            {
                // e.g.  * ...|
                beforeText: /^(\t|(\ \ ))*\ \*(\ ([^\*]|\*(?!\/))*)?$/,
                action: {
                    indentAction: monaco.languages.IndentAction.None,
                    appendText: '* '
                }
            },
            {
                // e.g.  */|
                beforeText: /^(\t|(\ \ ))*\ \*\/\s*$/,
                action: {
                    indentAction: monaco.languages.IndentAction.None,
                    removeText: 1
                }
            }
        ],
        comments: {
            lineComment: '//',
            blockComment: ['/*', '*/'],
        },
        operators: ['<=', '>=', '==', '!=', '+', '-', '*', '/', '%', '&', '|', '!', '&&', '||', '?', ':', '++', '--', '+=', '-=', '*=', '/='],
        autoClosingPairs: [
            { open: '{', close: '}' },
            { open: '[', close: ']' },
            { open: '(', close: ')' },
            { open: '"""', close: '"""', notIn: ['string.multi'] },
            { open: '<where>', close: '</where>' },
            { open: '<if', close: ' test=""></if>' },
            { open: '<elseif', close: ' test=""></elseif>' },
            { open: '<else', close: '></else>' },
            { open: '<set>', close: '</set>' },
            { open: '<trim>', close: '</trim>' },
            { open: '<foreach', close: ' collection=""></foreach>' },
            { open: '"', close: '"', notIn: ['string'] },
            { open: '\'', close: '\'', notIn: ['string'] },
            { open: '/**', close: ' */', notIn: ['string'] }
        ],
    })

    // 设置高亮
    monaco.languages.setMonarchTokensProvider(language, HighLightOptions);
    // 设置代码提示
    monaco.languages.registerCompletionItemProvider(language, CompletionItemProvider);
    // 设置折叠
    monaco.languages.registerFoldingRangeProvider(language, FoldingRangeProvider);
    // 设置参数提示
    monaco.languages.registerSignatureHelpProvider(language, SignatureHelpProvider);
    // 设置悬浮提示
    monaco.languages.registerHoverProvider(language, HoverProvider);
    // 设置代码格式化
    monaco.languages.registerDocumentFormattingEditProvider(language, {
        provideDocumentFormattingEdits(model, options, token) {
            return [{
                text: new Beautifier(model.getValue()).beautify(),
                range: model.getFullModelRange()
            }]
        }
    })
    // 设置html闭合标签提示
    monaco.languages.registerCompletionItemProvider('html', {
        triggerCharacters: ['>'],
        provideCompletionItems: (model, position) => {
            const codePre = model.getValueInRange({
                startLineNumber: position.lineNumber,
                startColumn: 1,
                endLineNumber: position.lineNumber,
                endColumn: position.column,
            })
            const tag = codePre.match(/.*<(\w+)>$/)?.[1];
            if (!tag) {
                return;
            }
            const word = model.getWordUntilPosition(position);
            return {
                suggestions: [{
                    label: `</${tag}>`,
                    kind: monaco.languages.CompletionItemKind.EnumMember,
                    insertText: `$1</${tag}>`,
                    insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                    range:  {
                        startLineNumber: position.lineNumber,
                        endLineNumber: position.lineNumber,
                        startColumn: word.startColumn,
                        endColumn: word.endColumn,
                    }
                }]
            }
        }
    })
}
