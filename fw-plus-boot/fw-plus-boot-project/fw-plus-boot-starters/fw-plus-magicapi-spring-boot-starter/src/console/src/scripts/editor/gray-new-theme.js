export default {
	editor: {
		base: 'vs',
		rules: [
			{ background: '#ffffff' },
			{ token: 'keywords', foreground: '000080', fontStyle: 'bold' },
			{ token: 'number', foreground: '0000FF' },
			{ token: 'keyword', foreground: '000080', fontStyle: 'bold' },
			{ token: 'directive', foreground: '0000FF', fontStyle: 'bold' },
			{ token: 'string.sql', foreground: '008000' },
			{ token: 'tag.sql', foreground: '0033B3' },
			{ token: 'attribute.name.sql', foreground: '174AD4' },
			{ token: 'attribute.value.sql', foreground: '067D17' },
			{ token: 'predefined', foreground: '000000', fontStyle: 'italic' },
			{ token: 'operator.sql', foreground: '000080', fontStyle: 'bold' },
			{ token: 'key', foreground: '660E7A' },
			{ token: 'string.key.json', foreground: '660E7A' },
			{ token: 'string.value.json', foreground: '008000' },
			{ token: 'keyword.json', foreground: '0000FF' },
			{ token: 'string', foreground: '008000', fontStyle: 'bold' },
			{ token: 'string.invalid', foreground: '008000', background: 'FFCCCC' },
			{ token: 'string.escape.invalid', foreground: '008000', background: 'FFCCCC' },
			{ token: 'string.escape', foreground: '000080', fontStyle: 'bold' },
			{ token: 'comment', foreground: '808080', fontStyle: 'italic' },
			{ token: 'comment.doc', foreground: '808080', fontStyle: 'italic' },
			{ token: 'comment.todo', foreground: '008DDE', fontStyle: 'italic' },
			{ token: 'string.escape', foreground: '000080' }
		],
		colors: {
			'editor.foreground': '#000000',
			'editor.background': '#ffffff',
			'editorLineNumber.foreground': '#999999',	//行号的颜色
			'editorGutter.background': '#F7F8FA',	//行号背景色
			'editor.lineHighlightBackground': '#F5F8FE',	//光标所在行的颜色
			'dropdown.background': '#F2F2F2',	//右键菜单
			'dropdown.foreground': '#000000',	//右键菜单文字颜色
			'list.activeSelectionBackground': '#1A7DC4',	//右键菜单悬浮背景色
			'list.activeSelectionForeground': '#ffffff',	//右键菜单悬浮文字颜色
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
		'main-hover-icon-background-color': '#EBECF0',
		'main-background-color': '#F7F8FA',
		'main-selected-background-color': '#DFE1E5',
		'main-hover-background-color': '#EBECF0',
		'magic-panel-toolbar-width': '32px',
		'magic-panel-toolbar-size': '24px',
		'tab-selected-border-color': '#3574F0',
		'magic-navbar-horizontal-height': '30px',
		'magic-input-height': '28px',
		'magic-table-row-height': '30px',
		'input-focus-color': '#3574F0',
		'input-focus-border-width': '2px',
		'input-border-radius': '3px',
		'select-background-color': '#fff',
		'select-hover-background-color': '#fff',
		'select-option-hover-background-color': '#CFDEFC',
		'select-option-hover-color': '#000',
		'tree-hover-background-color': '#CFDEFC',
		'button-hover-background-color': '#3574F0',
		'button-hover-color': '#fff',
		'button-border-hover-color': '#3574F0',
		'button-background-color': '#fff',
		'button-height': '24px',
		'checkbox-selected-background-color':'#3574F0',
		'empty-background-color':'#F7F8FA',
		'navbar-body-background-color':'#F7F8FA',
		'magic-header-logo-background-size': '24px',
		'magic-header-logo-background-position': '10px 7px',
		'magic-header-logo-padding': '40px',
		'context-menu-background': '#fff',
		'context-menu-padding': '4px',
		'context-menu-item-border-radius': '4px'
	}
};