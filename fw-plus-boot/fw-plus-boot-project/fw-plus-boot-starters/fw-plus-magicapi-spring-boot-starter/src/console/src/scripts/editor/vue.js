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
            { open: '"', close: '"', notIn: ['string'] },
            { open: '\'', close: '\'', notIn: ['string'] },
            { open: '`', close: '`', notIn: ['string'] },
            { open: '<!--', close: '-->', notIn: ['string', 'comment'] }
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

        // Vue 内置全局/组件 API
        vueGlobals: [
            'ref', 'reactive', 'computed', 'watch', 'watchEffect', 'watchPostEffect',
            'watchSyncEffect', 'onMounted', 'onUnmounted', 'onUpdated', 'onBeforeMount',
            'onBeforeUnmount', 'onBeforeUpdate', 'onActivated', 'onDeactivated',
            'provide', 'inject', 'nextTick', 'defineComponent', 'defineAsyncComponent',
            'h', 'createVNode', 'resolveComponent', 'resolveDirective', 'withDirectives',
            'withModifiers', 'toRef', 'toRefs', 'toValue', 'isRef', 'isReactive',
            'isReadonly', 'isProxy', 'unref', 'shallowRef', 'triggerRef', 'customRef',
            'shallowReactive', 'shallowReadonly', 'markRaw', 'toRaw', 'readonly',
            'useSlots', 'useAttrs', 'useCssModule', 'useTemplateRef',
            'defineProps', 'defineEmits', 'defineExpose', 'defineOptions', 'defineModel', 'defineSlots'
        ],

        // Vue 指令
        directives: [
            'v-model', 'v-for', 'v-if', 'v-else', 'v-else-if', 'v-show', 'v-bind',
            'v-on', 'v-slot', 'v-pre', 'v-once', 'v-text', 'v-html', 'v-cloak', 'v-memo'
        ],

        // Vue 内置组件
        builtInComponents: [
            'component', 'slot', 'template', 'transition', 'transition-group',
            'keep-alive', 'teleport', 'suspense', 'router-view', 'router-link'
        ],

        // 内置对象/类型
        builtInTypes: [
            'console', 'window', 'document', 'localStorage', 'sessionStorage',
            'JSON', 'Math', 'Date', 'RegExp', 'Error', 'Promise',
            'Array', 'Object', 'String', 'Number', 'Boolean', 'Symbol',
            'Map', 'Set', 'WeakMap', 'WeakSet', 'Proxy', 'Reflect',
            'Intl', 'URL', 'URLSearchParams', 'Headers', 'Request', 'Response',
            'FormData', 'File', 'Blob', 'Event', 'CustomEvent', 'fetch'
        ],

        escapes: /\\(?:[abfnrtv\\"']|x[0-9A-Fa-f]{1,4}|u[0-9A-Fa-f]{4}|U[0-9A-Fa-f]{8})/,

        tokenizer: {
            root: [
                // SFC 顶级块 —— 使用 Monaco 内置语言嵌入高亮
                [/<template/, { token: 'tag', next: '@templateTag' }],

                [/<script(\s+setup)?/, { token: 'tag', next: '@scriptTag' }],

                [/<style/, { token: 'tag', next: '@styleTag' }],

                // HTML 标签（Vue 内置组件用不同颜色）
                [/<\/?(\w+)/, {
                    cases: {
                        '@builtInComponents': 'tag',
                        '@default': 'delimiter.html'
                    },
                    next: '@tagAttributes'
                }],

                // 注释
                [/<!--/, { token: 'comment', next: '@htmlComment' }],

                // 字符串
                [/"/, { token: 'string', next: '@stringDouble' }],
                [/'/, { token: 'string', next: '@stringSingle' }],
                [/`/, { token: 'string', next: '@stringBacktick' }],

                // 数字
                [/\d*\.\d+([eE][\-+]?\d+)?/, 'number.float'],
                [/0[xX][0-9a-fA-F]+/, 'number.hex'],
                [/0[oO][0-7]+/, 'number.octal'],
                [/0[bB][01]+/, 'number.binary'],
                [/\d+/, 'number'],

                [/\s+/, 'white']
            ],

            // ── template 标签属性 ──
            templateTag: [
                [/lang=\s*["'](html|pug)["']/, 'keyword'],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/>/, { token: 'delimiter', next: '@templateContent', nextEmbedded: 'text/html' }],
                [/(<\/)(template\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]],
                [/[ \t\r\n]+/]
            ],

            // ── template 内容（委托给 HTML tokenizer，补充 Vue 插值）──
            templateContent: [
                [/<\/template/, { token: '@rematch', next: '@pop', nextEmbedded: '@pop' }],
                [/\{\{/, { token: 'delimiter.curly', next: '@interpolation' }],
                [/[^<{]+/, '']
            ],

            // ── Vue 模板插值 {{ expr }} ──
            interpolation: [
                [/\}\}/, { token: 'delimiter.curly', next: '@pop' }],
                [/\b(true|false|null|undefined)\b/, 'keyword'],
                [/[a-zA-Z_$][\w$]*/, {
                    cases: {
                        '@vueGlobals': 'type',
                        '@builtInTypes': 'type.identifier',
                        '@default': 'identifier'
                    }
                }],
                [/\d+/, 'number'],
                [/"([^"\\]|\\.)*"/, 'string'],
                [/'([^'\\]|\\.)*'/, 'string'],
                [/[{}()\[\]]/, '@brackets'],
                [/[;,]/, 'delimiter'],
                [/[=><!~?:&|+\-*\/^%]+/, 'operator'],
                [/\s+/, 'white']
            ],

            // ── script 标签属性（自动识别 lang=ts/tsx）──
            scriptTag: [
                [/lang=\s*["']ts["']/, { token: 'keyword', next: '@scriptTagTs' }],
                [/lang=\s*["']jsx["']/, { token: 'keyword', next: '@scriptTagTsx' }],
                [/lang=\s*["']tsx["']/, { token: 'keyword', next: '@scriptTagTsx' }],
                [/setup/, 'keyword'],
                [/generic=\s*["'][^"']*["']/, 'attribute'],
                [/name=\s*["'][^"']*["']/, 'attribute'],
                [/compilerOptions=/, { token: 'attribute.name', next: '@compilerOptionsValue' }],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/>/, { token: 'delimiter', next: '@scriptContent', nextEmbedded: 'text/javascript' }],
                [/(<\/)(script\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]],
                [/[ \t\r\n]+/]
            ],

            scriptTagTs: [
                [/setup/, 'keyword'],
                [/generic=\s*["'][^"']*["']/, 'attribute'],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/>/, { token: 'delimiter', next: '@scriptContent', nextEmbedded: 'text/typescript' }],
                [/(<\/)(script\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]],
                [/[ \t\r\n]+/]
            ],

            scriptTagTsx: [
                [/setup/, 'keyword'],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/>/, { token: 'delimiter', next: '@scriptContent', nextEmbedded: 'text/typescript' }],
                [/(<\/)(script\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]],
                [/[ \t\r\n]+/]
            ],

            compilerOptionsValue: [
                [/"([^"]*)"/, 'attribute.value', '@pop'],
                [/'([^']*)'/, 'attribute.value', '@pop'],
                [/\{/, 'delimiter', '@compilerOptionsJson'],
                [/[^>]+/, 'attribute.value', '@pop']
            ],

            compilerOptionsJson: [
                [/}/, 'delimiter', '@pop'],
                [/\{/, 'delimiter', '@compilerOptionsJson'],
                [/"([^"\\]|\\.)*"/, 'attribute.value'],
                [/\b(true|false|null)\b/, 'keyword'],
                [/\d+/, 'number'],
                [/[a-zA-Z_$][\w$]*/, 'attribute.name'],
                [/[,:]/, 'delimiter'],
                [/\s+/, 'white']
            ],

            // ── script 内容（委托 JS/TS tokenizer）──
            scriptContent: [
                [/<\/script/, { token: '@rematch', next: '@pop', nextEmbedded: '@pop' }],
                [/[^<]+/, '']
            ],

            // ── style 标签属性 ──
            styleTag: [
                [/lang=\s*["'](css|scss|sass|less|stylus)["']/, 'keyword'],
                [/scoped/, 'keyword'],
                [/module(\s*=\s*["'][\w\-]+["'])?/, 'keyword'],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/>/, { token: 'delimiter', next: '@styleContent', nextEmbedded: 'text/css' }],
                [/(<\/)(style\s*)(>)/, ['delimiter', 'tag', { token: 'delimiter', next: '@pop' }]],
                [/[ \t\r\n]+/]
            ],

            // ── style 内容（委托 CSS tokenizer）──
            styleContent: [
                [/<\/style/, { token: '@rematch', next: '@pop', nextEmbedded: '@pop' }],
                [/[^<]+/, '']
            ],

            // ── 普通 HTML 标签属性 ──
            tagAttributes: [
                [/\/?>/, { token: 'tag', next: '@pop' }],
                [/(v-\w+(?:-\w+)*)/, {
                    cases: { '@directives': 'directive', '@default': 'attribute.name' }
                }],
                [/@(\w+(?:\.\w+)*)/, 'directive'],
                [/:(\w+(?:-\w+)*(?:\.\w+)*)/, 'directive'],
                [/#(\w+)/, 'directive'],
                [/"([^"]*)"/, 'attribute.value'],
                [/'([^']*)'/, 'attribute.value'],
                [/[\w\-:]+/, 'attribute.name'],
                [/=/, 'delimiter'],
                [/\s+/, 'white']
            ],

            // ── HTML 注释 ──
            htmlComment: [
                [/-->/, { token: 'comment', next: '@pop' }],
                [/[^-]+/, 'comment'],
                [/./, 'comment']
            ],

            // ── 字符串 ──
            stringDouble: [
                [/[^\\"\$]+/, 'string'],
                [/@escapes/, 'string.escape'],
                [/\\./, 'string.escape.invalid'],
                [/\$\{/, { token: 'delimiter.bracket', next: '@templateExpr' }],
                [/"/, { token: 'string', next: '@pop' }]
            ],

            stringSingle: [
                [/[^\\'\$]+/, 'string'],
                [/@escapes/, 'string.escape'],
                [/\\./, 'string.escape.invalid'],
                [/'/, { token: 'string', next: '@pop' }]
            ],

            stringBacktick: [
                [/[^\\`\$]+/, 'string'],
                [/@escapes/, 'string.escape'],
                [/\\./, 'string.escape.invalid'],
                [/\$\{/, { token: 'delimiter.bracket', next: '@templateExpr' }],
                [/`/, { token: 'string', next: '@pop' }]
            ],

            // ── 模板字符串内嵌表达式 ──
            templateExpr: [
                [/}/, 'delimiter.bracket', '@pop'],
                [/\b(true|false|null|undefined)\b/, 'keyword'],
                [/[a-zA-Z_$][\w$]*/, {
                    cases: {
                        '@vueGlobals': 'type',
                        '@builtInTypes': 'type.identifier',
                        '@default': 'identifier'
                    }
                }],
                [/\d+/, 'number'],
                [/"([^"\\]|\\.)*"/, 'string'],
                [/'([^'\\]|\\.)*'/, 'string'],
                [/[{}()\[\]]/, '@brackets'],
                [/[;,]/, 'delimiter'],
                [/[=><!~?:&|+\-*\/^%]+/, 'operator'],
                [/\s+/, 'white']
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