export default function registerJavaLanguage(monaco) {
    if (!monaco || !monaco.languages) {
        return
    }
    const exists = monaco.languages.getLanguages && monaco.languages.getLanguages().some(language => language.id === 'java')
    if (!exists && monaco.languages.register) {
        monaco.languages.register({ id: 'java' })
    }
    if (monaco.languages.setLanguageConfiguration) {
        monaco.languages.setLanguageConfiguration('java', {
            comments: {
                lineComment: '//',
                blockComment: ['/*', '*/']
            },
            brackets: [
                ['{', '}'],
                ['[', ']'],
                ['(', ')']
            ],
            autoClosingPairs: [
                { open: '{', close: '}' },
                { open: '[', close: ']' },
                { open: '(', close: ')' },
                { open: '"', close: '"' },
                { open: '\'', close: '\'' }
            ]
        })
    }
    if (monaco.languages.setMonarchTokensProvider) {
        monaco.languages.setMonarchTokensProvider('java', {
            keywords: [
                'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char',
                'class', 'const', 'continue', 'default', 'do', 'double', 'else', 'enum',
                'extends', 'final', 'finally', 'float', 'for', 'goto', 'if', 'implements',
                'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new',
                'package', 'private', 'protected', 'public', 'return', 'short', 'static',
                'strictfp', 'super', 'switch', 'synchronized', 'this', 'throw', 'throws',
                'transient', 'try', 'void', 'volatile', 'while'
            ],
            tokenizer: {
                root: [
                    [/[a-zA-Z_$][\w$]*/, { cases: { '@keywords': 'keyword', '@default': 'identifier' } }],
                    [/\/\/.*$/, 'comment'],
                    [/\/\*/, 'comment', '@comment'],
                    [/"([^"\\]|\\.)*$/, 'string.invalid'],
                    [/"/, 'string', '@string'],
                    [/'[^\\']'/, 'string'],
                    [/\d+/, 'number'],
                    [/[{}()[\]]/, '@brackets'],
                    [/[;,.]/, 'delimiter']
                ],
                comment: [
                    [/[^/*]+/, 'comment'],
                    [/\*\//, 'comment', '@pop'],
                    [/[/*]/, 'comment']
                ],
                string: [
                    [/[^\\"]+/, 'string'],
                    [/\\./, 'string.escape'],
                    [/"/, 'string', '@pop']
                ]
            }
        })
    }
}
