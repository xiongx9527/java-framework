import * as monaco from 'monaco-editor'

export const initializeVue = () => {
    const language = 'vue'
    
    // 注册Vue语言
    monaco.languages.register({ 
        id: language,
        extensions: ['.vue'],
        aliases: ['Vue', 'vue'],
        mimetypes: ['text/x-vue']
    })
    
    // 注册HTML语言（如果尚未注册）
    if (!monaco.languages.getLanguages().some(lang => lang.id === 'html')) {
        monaco.languages.register({
            id: 'html',
            extensions: ['.html', '.htm'],
            aliases: ['HTML', 'html'],
            mimetypes: ['text/html']
        });
    }
    
    // 设置语言配置
    monaco.languages.setLanguageConfiguration(language, {
        wordPattern: /(-?\d*\.\d\w*)|([^`~!#%^&*()\-=+[{\]}\\|;:'",.<>/?\s]+)/g,
        brackets: [
            ['{', '}'],
            ['[', ']'],
            ['(', ')'],
            ['<', '>']
        ],
        autoClosingPairs: [
            { open: '{', close: '}' },
            { open: '[', close: ']' },
            { open: '(', close: ')' },
            { open: '<', close: '>' },
            { open: '"', close: '"', notIn: ['string'] },
            { open: '\'', close: '\'', notIn: ['string'] },
            { open: '`', close: '`', notIn: ['string'] },
            { open: '<!--', close: '-->' },
            { open: '<template', close: '</template>' },
            { open: '<script', close: '</script>' },
            { open: '<style', close: '</style>' }
        ],
        surroundingPairs: [
            { open: '{', close: '}' },
            { open: '[', close: ']' },
            { open: '(', close: ')' },
            { open: '<', close: '>' },
            { open: '"', close: '"' },
            { open: '\'', close: '\'' },
            { open: '`', close: '`' }
        ],
        comments: {
            lineComment: '//',
            blockComment: ['/*', '*/']
        },
        folding: {
            markers: {
                start: new RegExp("^\\s*//\\s*#?region\\b"),
                end: new RegExp("^\\s*//\\s*#?endregion\\b")
            }
        }
    })

    // 设置HTML语言配置
    monaco.languages.setLanguageConfiguration('html', {
        wordPattern: /(-?\d*\.\d\w*)|([^`~!@#$%^&*()\-=+[{\]}\\|;:'",.<>\/?\s]+)/g,
        comments: {
            blockComment: ['<!--', '-->']
        },
        brackets: [
            ['<!--', '-->'],
            ['<', '>'],
            ['{', '}'],
            ['(', ')']
        ],
        autoClosingPairs: [
            { open: '{', close: '}' },
            { open: '[', close: ']' },
            { open: '(', close: ')' },
            { open: '"', close: '"' },
            { open: '\'', close: '\'' }
        ],
        surroundingPairs: [
            { open: '"', close: '"' },
            { open: '\'', close: '\'' },
            { open: '{', close: '}' },
            { open: '[', close: ']' },
            { open: '(', close: ')' },
            { open: '<', close: '>' }
        ],
        colorizedBracketPairs: [
            ['{', '}'],
            ['(', ')']
        ],
        folding: {
            markers: {
                start: new RegExp("^\\s*<!--\\s*#region\\b.*-->"),
                end: new RegExp("^\\s*<!--\\s*#endregion\\b.*-->")
            }
        }
    });

    // 设置语法高亮规则
    monaco.languages.setMonarchTokensProvider(language, {
        defaultToken: '',
        tokenPostfix: '.vue',
        
        // 关键字
        keywords: [
            'template', 'script', 'style', 'lang', 'scoped', 'setup', 'defineProps',
            'defineEmits', 'ref', 'reactive', 'computed', 'watch', 'watchEffect',
            'onMounted', 'onUnmounted', 'onUpdated', 'onBeforeMount', 'onBeforeUnmount',
            'onBeforeUpdate', 'provide', 'inject', 'nextTick', 'const', 'let', 'var', 'function', 'return',
            'class', 'extends', 'implements', 'interface', 'type', 'typeof', 'instanceof',
            'new', 'this', 'super', 'true', 'false', 'null', 'undefined', 'NaN',
            'Infinity', 'void', 'delete', 'in', 'of', 'try', 'catch', 'finally',
            'throw', 'switch', 'case', 'default', 'break', 'continue', 'do', 'while',
            'for', 'if', 'else', 'async', 'await', 'yield', 'get', 'set', 'static',
            'public', 'private', 'protected', 'readonly', 'abstract', 'enum',
            'declare', 'module', 'namespace', 'require', 'global', 'window', 'document',
            'console', 'localStorage', 'sessionStorage', 'JSON', 'Math', 'Date',
            'RegExp', 'Error', 'Promise', 'Array', 'Object', 'String', 'Number',
            'Boolean', 'Symbol', 'Map', 'Set', 'WeakMap', 'WeakSet', 'Proxy',
            'Reflect', 'Intl', 'URL', 'URLSearchParams', 'Headers', 'Request',
            'Response', 'fetch', 'Event', 'CustomEvent', 'FormData', 'File', 'Blob'
        ],
        
        // 指令
        directives: [
            'v-model', 'v-for', 'v-if', 'v-else', 'v-else-if', 'v-show', 'v-bind',
            'v-on', 'v-slot', 'v-pre', 'v-once', 'v-text', 'v-html', 'v-cloak'
        ],
        
        // Vue特殊标签
        specialTags: [
            'component', 'template', 'slot', 'transition', 'transition-group',
            'keep-alive', 'teleport', 'suspense'
        ],

        // 操作符
        operators: [
            '=', '>', '<', '!', '~', '?', ':', '==', '<=', '>=', '!=',
            '&&', '||', '++', '++', '--', '+', '-', '*', '/', '&', '|', '^', '%',
            '<<', '>>', '>>>', '+=', '-=', '*=', '/=', '&=', '|=', '^=',
            '<<=', '>>=', '>>>=', '%='
        ],

        // 符号
        symbols: /[=><!~?:&|+\-*\/\^%]+/,

        // 数字
        digits: /\d+/,

        // 转义序列
        escapes: /\\(?:[abfnrtv\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,

        tokenizer: {
            root: [
                // 导入语句特殊处理
                [/^\s*import\s+/, { token: 'keyword', next: '@import' }],
                
                // Vue模板标签
                [/<template/, { token: 'tag', next: '@template' }],
                [/<script/, { token: 'tag', next: '@script' }],
                [/<style/, { token: 'tag', next: '@style' }],
                
                // 特殊Vue标签
                [/<(\w+)/, {
                    cases: {
                        '@specialTags': 'tag.name',
                        '@default': 'tag'
                    },
                    next: '@tag_content'
                }],
                
                // 注释
                [/<!--/, { token: 'comment', next: '@comment' }],
                [/\/\//, 'comment'],
                [/\/\*/, { token: 'comment', next: '@comment_block' }],
                
                // 字符串
                [/"/, { token: 'string', next: '@string_double' }],
                [/'/, { token: 'string', next: '@string_single' }],
                [/`/, { token: 'string', next: '@string_backtick' }],
                
                // 数字
                [/\d*\.\d+([eE][\-+]?\d+)?/, 'number.float'],
                [/0[xX][0-9a-fA-F]+/, 'number.hex'],
                [/0[oO][0-7]+/, 'number.octal'],
                [/0[bB][01]+/, 'number.binary'],
                [/\d+/, 'number'],
                
                // Vue指令
                [/(v-\w+(?:-\w+)*)/, {
                    cases: {
                        '@directives': 'directive',
                        '@default': 'attribute.name'
                    }
                }],
                [/@(\w+)/, 'directive'],
                [/:(\w+(?:-\w+)*)/, 'directive'],
                
                // 符号和运算符
                [/[{}()\[\]]/, '@brackets'],
                [/[<>](?!\w)/, '@brackets'],
                [/@symbols/, { cases: { '@operators': 'operator', '@default': '' } }],
                
                // 导入语句处理（非行首的情况）
                [/\bimport\b/, 'keyword'],
                [/\bfrom\b/, 'keyword'],
                [/\bexport\b/, 'keyword'],
                [/\bdefault\b/, 'keyword'],
                
                // 标识符和关键字
                [/[a-zA-Z_$][\w$]*/, {
                    cases: {
                        '@keywords': 'keyword',
                        '@default': 'identifier'
                    }
                }],
                
                // 空白
                [/\s+/, 'white']
            ],

            // HTML模板
            template: [
                [/\/>/, { token: 'tag', next: '@pop' }],
                [/<\/template>/, { token: 'tag', next: '@pop' }],
                { include: '@html' }
            ],

            // HTML嵌入
            html: [
                [/<\w+/, { token: 'tag', next: '@tag_content' }],
                [/<\/\w+/, { token: 'tag', next: '@closing_tag' }],
                [/<!--/, { token: 'comment', next: '@comment' }],
                [/[^<]+/, ''],
                [/</, '']
            ],

            // Script标签
            script: [
                [/\/>/, { token: 'tag', next: '@pop' }],
                [/<\/script>/, { token: 'tag', next: '@pop' }],
                [/lang=\s*["']ts["']/, 'keyword'],
                [/setup/, 'keyword'],
                { include: '@javascript' }
            ],

            // JavaScript嵌入
            javascript: [
                [/\b(import|from|export|default)\b/, 'keyword'],
                [/\b(var|let|const|function|return|if|for|while|else|do|break|continue|switch|case|default|throw|try|catch|finally|new|delete|typeof|instanceof|void|yield|await|async|class|extends|super|import|from|export|default|static|get|set|constructor)\b/, 'keyword'],
                [/\b(true|false|null|undefined|NaN|Infinity)\b/, 'keyword'],
                [/\b(console|window|document|Array|Object|String|Number|Boolean|Function|RegExp|Date|Error|Math|JSON|Promise|Map|Set|WeakMap|WeakSet)\b/, 'type'],
                [/\b(this|super)\b/, 'variable.predefined'],
                [/[a-zA-Z_$][\w$]*/, 'identifier'],
                [/[$][a-zA-Z_$][\w$]*/, 'variable.predefined'],
                [/[0-9]+\.[0-9]+([eE][\-+]?[0-9]+)?/, 'number.float'],
                [/0[xX][0-9a-fA-F]+/, 'number.hex'],
                [/0[0-7]+(?!\d)/, 'number.octal'],
                [/[0-9]+/, 'number'],
                [/"([^"\\]|\\.)*$/, 'string.invalid'],
                [/'([^'\\]|\\.)*$/, 'string.invalid'],
                [/"([^"\\]|\\.)*"/, 'string'],
                [/'([^'\\]|\\.)*'/, 'string'],
                [/`([^`\\]|\\.)*`/, 'string'],
                [/[{}()\[\]]/, '@brackets'],
                [/[;,]/, 'delimiter'],
                [/[\/*=<>!]+/, 'operator'],
                [/\/\/.*$/, 'comment'],
                [/\/\*/, 'comment', '@comment_js'],
                [/\s+/, 'white']
            ],

            // JavaScript注释
            comment_js: [
                [/[^\/*]+/, 'comment'],
                [/\*\//, 'comment', '@pop'],
                [/[\/*]/, 'comment']
            ],

            // Style标签
            style: [
                [/\/>/, { token: 'tag', next: '@pop' }],
                [/<\/style>/, { token: 'tag', next: '@pop' }],
                [/lang=\s*["'](css|scss|sass|less)["']/, 'keyword'],
                [/scoped/, 'keyword'],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/>/, { token: 'delimiter', next: '@styleEmbedded', nextEmbedded: 'text/css' }],
                [/[ \t\r\n]+/],
                [/(<\/)(style\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]]
            ],

            // 注释
            comment: [
                [/-->/, { token: 'comment', next: '@pop' }],
                [/[^-]+/, 'comment'],
                [/./, 'comment']
            ],
            
            comment_block: [
                [/\*\//, { token: 'comment', next: '@pop' }],
                [/[^*]+/, 'comment'],
                [/./, 'comment']
            ],

            // 字符串
            string_double: [
                [/[^\\"\$]+/, 'string'],
                [/@escapes/, 'string.escape'],
                [/\./, 'string.escape.invalid'],
                [/\$\(/, { token: 'delimiter.bracket', next: '@bracketCounting' }],
                [/"/, { token: 'string', next: '@pop' }]
            ],

            string_single: [
                [/[^\\'\$]+/, 'string'],
                [/@escapes/, 'string.escape'],
                [/\./, 'string.escape.invalid'],
                [/\$\(/, { token: 'delimiter.bracket', next: '@bracketCounting' }],
                [/'/, { token: 'string', next: '@pop' }]
            ],

            string_backtick: [
                [/[^\\`\$]+/, 'string'],
                [/@escapes/, 'string.escape'],
                [/\./, 'string.escape.invalid'],
                [/\$\{/, { token: 'delimiter.bracket', next: '@bracketCounting' }],
                [/`/, { token: 'string', next: '@pop' }]
            ],

            bracketCounting: [
                [/{/, 'delimiter.bracket', '@bracketCounting'],
                [/}/, 'delimiter.bracket', '@pop'],
                { include: 'root' }
            ],
            
            // 导入语句处理
            import: [
                [/[a-zA-Z_$][\w$]*/, 'identifier'],
                [/,/, 'punctuation'],
                [/\bfrom\b/, 'keyword'],
                [/'/, { token: 'string', next: '@string_single' }],
                [/"/, { token: 'string', next: '@string_double' }],
                [/;/, 'punctuation', '@pop'],
                [/$/, '', '@pop'],
                { include: 'root' }
            ],
            
            // 标签内容处理
            tag_content: [
                // Vue指令
                [/(v-\w+(?:-\w+)*)/, {
                    cases: {
                        '@directives': 'directive',
                        '@default': 'attribute.name'
                    }
                }],
                // :绑定指令和@事件指令
                [/:(\w+(?:-\w+)*)/, 'directive'],
                [/@(\w+)/, 'directive'],
                // 普通属性
                [/([a-zA-Z\-:]+)=/, 'attribute.name'],
                // 字符串属性值
                [/'/, { token: 'string', next: '@string_single_attr' }],
                [/"/, { token: 'string', next: '@string_double_attr' }],
                // 结束标签
                [/>/, { token: 'tag', next: '@pop' }],
                [/\/>/, { token: 'tag', next: '@pop' }],
                // 空白
                [/\s+/, 'white']
            ],
            
            // 属性值处理
            attribute_value: [
                [/'/, { token: 'string', next: '@string_single_attr' }],
                [/"/, { token: 'string', next: '@string_double_attr' }],
                [/[^{}\s]+/, 'string'],
                [/\s+/, 'white', '@pop'],
                [/[<>]/, '', '@pop']
            ],
            
            string_single_attr: [
                [/[^']+/, 'string'],
                [/'/, { token: 'string', next: '@pop' }]
            ],
            
            string_double_attr: [
                [/[^"]+/, 'string'],
                [/"/, { token: 'string', next: '@pop' }]
            ],
            
            // 结束标签处理
            closing_tag: [
                [/[a-zA-Z][a-zA-Z0-9\-]*/, 'tag'],
                [/>/, { token: 'tag', next: '@pop' }],
                [/\s+/, 'white']
            ],
            
            // CSS嵌入内容
            styleEmbedded: [
                [/<\/style/, { token: '@rematch', next: '@pop', nextEmbedded: '@pop' }],
                [/[^<]+/, '']
            ]
        }
    })

    // 设置HTML语言的语法规则
    monaco.languages.setMonarchTokensProvider('html', {
        defaultToken: '',
        tokenPostfix: '.html',
        ignoreCase: true,

        // HTML标签
        tags: [
            'html', 'head', 'title', 'base', 'link', 'meta', 'style', 'script', 'noscript',
            'body', 'section', 'nav', 'article', 'aside', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
            'header', 'footer', 'address', 'main', 'p', 'hr', 'pre', 'blockquote', 'ol', 'ul',
            'li', 'dl', 'dt', 'dd', 'figure', 'figcaption', 'div', 'a', 'em', 'strong', 'small',
            's', 'cite', 'q', 'dfn', 'abbr', 'data', 'time', 'code', 'var', 'samp', 'kbd', 'sub',
            'sup', 'i', 'b', 'u', 'mark', 'ruby', 'rt', 'rp', 'bdi', 'bdo', 'span', 'br', 'wbr',
            'ins', 'del', 'picture', 'source', 'img', 'iframe', 'embed', 'object', 'param',
            'video', 'audio', 'track', 'canvas', 'map', 'area', 'svg', 'math', 'table', 'caption',
            'colgroup', 'col', 'tbody', 'thead', 'tfoot', 'tr', 'td', 'th', 'form', 'label',
            'input', 'button', 'select', 'datalist', 'optgroup', 'option', 'textarea', 'keygen',
            'output', 'progress', 'meter', 'fieldset', 'legend', 'details', 'summary', 'menuitem',
            'menu'
        ],

        // HTML属性
        attributes: [
            'accept', 'accept-charset', 'accesskey', 'action', 'align', 'alt', 'async',
            'autocomplete', 'autofocus', 'autoplay', 'autosave', 'background', 'bgcolor',
            'border', 'buffered', 'challenge', 'charset', 'checked', 'cite', 'class', 'code',
            'codebase', 'color', 'cols', 'colspan', 'content', 'contenteditable', 'contextmenu',
            'controls', 'coords', 'data', 'datetime', 'default', 'defer', 'dir', 'dirname',
            'disabled', 'download', 'draggable', 'dropzone', 'enctype', 'for', 'form', 'formaction',
            'headers', 'height', 'hidden', 'high', 'href', 'hreflang', 'http-equiv', 'icon',
            'id', 'ismap', 'itemprop', 'keytype', 'kind', 'label', 'lang', 'language',
            'list', 'loop', 'low', 'manifest', 'max', 'maxlength', 'media', 'method',
            'min', 'multiple', 'muted', 'name', 'novalidate', 'open', 'optimum', 'pattern',
            'ping', 'placeholder', 'poster', 'preload', 'radiogroup', 'readonly', 'rel',
            'required', 'reversed', 'rows', 'rowspan', 'sandbox', 'scope', 'scoped',
            'seamless', 'selected', 'shape', 'size', 'sizes', 'span', 'spellcheck', 'src',
            'srcdoc', 'srclang', 'start', 'step', 'style', 'summary', 'tabindex', 'target',
            'title', 'type', 'usemap', 'value', 'width', 'wrap'
        ],

        // HTML事件
        events: [
            'onabort', 'onblur', 'oncanplay', 'oncanplaythrough', 'onchange', 'onclick',
            'oncontextmenu', 'ondblclick', 'ondrag', 'ondragend', 'ondragenter', 'ondragleave',
            'ondragover', 'ondragstart', 'ondrop', 'ondurationchange', 'onemptied', 'onended',
            'onerror', 'onfocus', 'onformchange', 'onforminput', 'oninput', 'oninvalid',
            'onkeydown', 'onkeypress', 'onkeyup', 'onload', 'onloadeddata', 'onloadedmetadata',
            'onloadstart', 'onmousedown', 'onmousemove', 'onmouseout', 'onmouseover',
            'onmouseup', 'onmousewheel', 'onpause', 'onplay', 'onplaying', 'onprogress',
            'onratechange', 'onreset', 'onresize', 'onscroll', 'onseeked', 'onseeking',
            'onselect', 'onshow', 'onstalled', 'onsubmit', 'onsuspend', 'ontimeupdate',
            'onvolumechange', 'onwaiting'
        ],

        tokenizer: {
            root: [
                [/<!DOCTYPE/, 'metatag', '@doctype'],
                [/<!--/, 'comment', '@comment'],
                [/(<)((?:[\w\-]+:)?[\w\-]+)(\s*)(\/>)/, ['delimiter', 'tag', '', 'delimiter']],
                [/(<)(script)/, ['delimiter', { token: 'tag', next: '@script' }]],
                [/(<)(style)/, ['delimiter', { token: 'tag', next: '@style' }]],
                [/(<)((?:[\w\-]+:)?[\w\-]+)/, ['delimiter', { token: 'tag', next: '@otherTag' }]],
                [/(<\/)((?:[\w\-]+:)?[\w\-]+)/, ['delimiter', { token: 'tag', next: '@otherTag' }]],
                [/</, 'delimiter'],
                [/[^<]+/]
            ],

            doctype: [
                [/[^>]+/, 'metatag.content'],
                [/>/, 'metatag', '@pop']
            ],

            comment: [
                [/-->/, 'comment', '@pop'],
                [/[^-]+/, 'comment.content'],
                [/./, 'comment.content']
            ],

            otherTag: [
                [/\/?>/, 'delimiter', '@pop'],
                // Vue指令处理
                [/(v-\w+(?:-\w+)*)/, 'directive'],
                [/:(\w+(?:-\w+)*)/, 'directive'],
                [/@(\w+)/, 'directive'],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/[ \t\r\n]+/]
            ],

            // JavaScript嵌入
            script: [
                [/type/, 'attribute.name', '@scriptAfterType'],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/>/, { token: 'delimiter', next: '@scriptEmbedded', nextEmbedded: 'text/javascript' }],
                [/[ \t\r\n]+/],
                [/(<\/)(script\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]]
            ],

            // CSS嵌入
            style: [
                [/type/, 'attribute.name', '@styleAfterType'],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/>/, { token: 'delimiter', next: '@styleEmbedded', nextEmbedded: 'text/css' }],
                [/[ \t\r\n]+/],
                [/(<\/)(style\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]]
            ],

            scriptAfterType: [
                [/=/, 'delimiter', '@scriptAfterTypeEquals'],
                [/>/, { token: 'delimiter', next: '@scriptEmbedded', nextEmbedded: 'text/javascript' }],
                [/[ \t\r\n]+/],
                [/<\/script\s*>/, { token: '@rematch', next: '@pop' }]
            ],

            scriptAfterTypeEquals: [
                [/"([^"]*)"/, { token: 'attribute.value', switchTo: '@scriptWithCustomType.$1' }],
                [/'([^']*)'/, { token: 'attribute.value', switchTo: '@scriptWithCustomType.$1' }],
                [/>/, { token: 'delimiter', next: '@scriptEmbedded', nextEmbedded: 'text/javascript' }],
                [/[ \t\r\n]+/],
                [/<\/script\s*>/, { token: '@rematch', next: '@pop' }]
            ],

            scriptWithCustomType: [
                [/>/, { token: 'delimiter', next: '@scriptEmbedded.$S2', nextEmbedded: '$S2' }],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/[ \t\r\n]+/],
                [/<\/script\s*>/, { token: '@rematch', next: '@pop' }]
            ],

            scriptEmbedded: [
                [/<\/script/, { token: '@rematch', next: '@pop', nextEmbedded: '@pop' }],
                [/[^<]+/, '']
            ],

            styleAfterType: [
                [/=/, 'delimiter', '@styleAfterTypeEquals'],
                [/>/, { token: 'delimiter', next: '@styleEmbedded', nextEmbedded: 'text/css' }],
                [/[ \t\r\n]+/],
                [/<\/style\s*>/, { token: '@rematch', next: '@pop' }]
            ],

            styleAfterTypeEquals: [
                [/"([^"]*)"/, { token: 'attribute.value', switchTo: '@styleWithCustomType.$1' }],
                [/'([^']*)'/, { token: 'attribute.value', switchTo: '@styleWithCustomType.$1' }],
                [/>/, { token: 'delimiter', next: '@styleEmbedded', nextEmbedded: 'text/css' }],
                [/[ \t\r\n]+/],
                [/<\/style\s*>/, { token: '@rematch', next: '@pop' }]
            ],

            styleWithCustomType: [
                [/>/, { token: 'delimiter', next: '@styleEmbedded.$S2', nextEmbedded: '$S2' }],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/[ \t\r\n]+/],
                [/<\/style\s*>/, { token: '@rematch', next: '@pop' }]
            ],

            styleEmbedded: [
                [/<\/style/, { token: '@rematch', next: '@pop', nextEmbedded: '@pop' }],
                [/[^<]+/, '']
            ]
        }
    });
}