export default {
	editor: {
        base: 'vs-dark',
        rules: [
            { foreground: 'A9B7C6' },
            { token: 'keywords', foreground: 'CC7832', fontStyle: 'bold' },
            { token: 'keyword', foreground: 'CC7832', fontStyle: 'bold' },
            { token: 'directive', foreground: 'BBB529', fontStyle: 'bold' },
            { token: 'number', foreground: '6897BB' },
            { token: 'string', foreground: '6A8759', fontStyle: 'bold' },
            { token: 'string.sql', foreground: '6A8759' },
            { token: 'tag.sql', foreground: 'E8BF6A' },
            { token: 'attribute.name.sql', foreground: 'BABABA' },
            { token: 'attribute.value.sql', foreground: '6A8759' },
            { token: 'predefined.sql', foreground: 'A9B7C6', fontStyle: 'italic' },
            { token: 'predefined.magicscript', foreground: 'A9B7C6', fontStyle: 'italic' },
            { token: 'key', foreground: '9876AA' },
            { token: 'string.key.json', foreground: '9876AA' },
            { token: 'string.value.json', foreground: '6A8759' },
            { token: 'keyword.json', foreground: '6897BB' },
            { token: 'operator.sql', foreground: 'CC7832', fontStyle: 'bold' },
            { token: 'string.invalid', foreground: '008000', background: 'FFCCCC' },
            { token: 'string.escape.invalid', foreground: '008000', background: 'FFCCCC' },
            { token: 'string.escape', foreground: '000080', fontStyle: 'bold' },
            { token: 'comment', foreground: '808080', fontStyle: 'italic' },
            { token: 'comment.doc', foreground: '629755', fontStyle: 'italic' },
            { token: 'comment.todo', foreground: 'A8C023', fontStyle: 'italic' },
            { token: 'string.escape', foreground: 'CC7832' }
        ],
        colors: {
            'editor.background': '#2B2B2B',
            'editorLineNumber.foreground': '#999999',	//行号的颜色
            'editorGutter.background': '#313335',	//行号背景色
            'editor.lineHighlightBackground': '#323232',	//光标所在行的颜色
            'dropdown.background': '#3C3F41',	//右键菜单
            'dropdown.foreground': '#BBBBBB',	//右键菜单文字颜色
            'list.activeSelectionBackground': '#4B6EAF',	//右键菜单悬浮背景色
            'list.activeSelectionForeground': '#FFFFFF',	//右键菜单悬浮文字颜色
            'editorSuggestWidget.selectedBackground': '#113A5C' //代码提示选中行的背景色
        }
    },
    styles: {
		'magic-navbar-vertical-width': '42px',
		'magic-navbar-vertical-title-display': 'none',
		'magic-navbar-vertical-icon-size': '1.8em',
		'magic-navbar-vertical-header-padding': '5.5px 3px',
		'magic-navbar-vertical-header-margin': '5px',
		'magic-navbar-vertical-header-border-radius': '2px',
		'magic-navbar-vertical-header-border-width': '0px',
		'magic-resource-header-icon-display': 'none',
		'magic-header-height': '40px',
		'magic-header-icon-size': '28px',
		'magic-panel-toolbar-width': '32px',
		'magic-panel-toolbar-size': '24px',
		'magic-navbar-horizontal-height': '30px',
		'magic-input-height': '28px',
		'magic-table-row-height': '30px',
		'input-focus-border-width': '2px',
		'input-border-radius': '3px',
		'button-height': '24px',
		'magic-header-logo-background-size': '24px',
		'magic-header-logo-background-position': '10px 7px',
		'magic-header-logo-padding': '40px',
		'context-menu-padding': '4px',
		'context-menu-item-border-radius': '4px',
		'main-background-color': '#3C3F41',
        'main-border-color': '#515151',
        'main-color': '#bbb',
        'main-selected-background-color': '#27292A',
        'main-hover-background-color': '#353739',
        'main-hover-icon-background-color': '#4C5052',
        'main-selected-color': '#fff',
        'main-icon-color': '#AFB1B3',

        'header-title-color': '#bbb',
        'header-version-color': '#999',
        'header-default-color': '#AFB1B3',

        'empty-background-color': '#282828',
        'empty-key-color': '#489DF6',
        'empty-color': '#A0A0A0',

        'button-hover-background-color': '#365880',
        'button-hover-border-color': '#43688C',
        'button-background-color': '#4C5052',
        'button-border-color': '#5E6060',
        'button-disabled-color': '#5a5a5a',

        'navbar-body-background-color': '#3C3F41',
        'navbar-body-border-color': '#555555',
        'resource-label-color': '#bbb',
        'resource-span-color': '#787878',

        'tree-hover-background-color': '#0d293e',
        'tree-icon-color': '#aeb9c0',

        'table-border-color': '#646464',

        'input-border-color': '#646464',
        'input-foucs-color': '#3D6185',
        'input-background-color': '#45494A',

        'select-background-color': '#3C3F41',
        'select-hover-background-color': '#3C3F41',
        'select-option-background-color': '#3C3F41',
        'select-option-hover-background-color': '#4B6EAF',
        'select-option-border-color': '#808080',

        'data-type-default-color': '#a9b7c6',
        'data-type-string-color': '#6a8759',
        'data-type-integer-color': '#6897bb',
        'data-type-byte-color': '#6897bb',
        'data-type-long-color': '#6897bb',
        'data-type-float-color': '#6897bb',
        'data-type-double-color': '#6897bb',
        'data-type-short-color': '#6897bb',
        'data-type-number-color': '#6897bb',
        'data-type-boolean-color': '#cc7832',
        'data-type-class-color': '#9876aa',
        'data-type-key-color': '#FF8E8E',


        'run-log-background-color': '#2b2b2b',
        'log-level-info': '#ABC023',
        'log-level-error': '#CC666E',
        'log-level-debug': '#299999',
        'log-level-warn': 'unset',
        'log-level-trace': '#5394EC',
        'log-color-cyan': '#009191',
        'log-color-link': '#287BDE',

        'todo-color': '#A8C023',

        'debug-line-background-color': '#2D6099',
        'breakpoints-background-color': '#C75450',
        'breakpoint-line-background-color': '#3a2323',

        'select-inputable-background-color': '#45494a',
        'select-inputable-border': 'transparent',

        'tab-selected-background-color': '#4E5254',

        'message-em-color': '#68dd9a',

        'checkbox-background-color': '#43494A',
        'checkbox-border-color': '#6B6B6B',
        'checkbox-text-color': '#bbb',
        'checkbox-selected-background-color': '#43494A',
        'checkbox-selected-border-color': '#6B6B6B',

        'toolbox-list-label-color': '#bbb',
        'toolbox-list-span-color': '#787878',
        'toolbox-border-color': '#323232',
        'toolbox-list-hover-background': '#0D293E',
        'toolbox-border-right-color': '#555555',
        'footer-border-color': '#323232',
        'tab-bar-border-color': '#323232',
        'dialog-border-color': '#282828',
        'dialog-shadow-color': '#151515',
        'table-col-border-color': '#333638',
        'table-row-border-color': '#333638',
        'table-hover-background': '#4B6EAF',
        'debug-line-background': '#2D6099',
        'breakpoints-background': '#C75450',
        'breakpoint-line-background': '#3a2323',
        'table-even-background': '#414547',
        'button-disabled-background': '#5A5A5A',
        'toolbox-list-header-icon-color': '#AFB1B3',
        'log-error-color': '#CC666E',
        'text-string-color': '#6A8759',
        'text-number-color': '#6897BB',
        'text-boolean-color': '#CC7832',
        'text-property-color': '#9876aa',
        'text-key-color': '#9876aa',
        'suggest-hover-background': '#113A5C',
        'suggest-hover-color': '#fff',
        'statusbar-em-color': '#68dd9a',

		'tab-selected-border-color': '#4A88C7',
		'tooltip-background-color': '#4B4D4D',
		'tooltip-color': '#bbb',
		'tooltip-border-color': '#636569'
	}
};