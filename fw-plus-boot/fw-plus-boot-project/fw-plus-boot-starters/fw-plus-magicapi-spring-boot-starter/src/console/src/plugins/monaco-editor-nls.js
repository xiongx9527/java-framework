let index = 0
const mapLangIdx = {}
const getIndex = en => {
	if(en in mapLangIdx){
		return mapLangIdx[en]
	}
	const idx = index++
	mapLangIdx[en] = idx
	return idx

}
const initLanguage = (rstObj, en, langObj) => {
	for (var key in en) {
		if (en && langObj && typeof(en[key]) === "string" && typeof(langObj[key]) === "string") {
			rstObj[getIndex(en[key])] = langObj[key];
		} else if (en && langObj && en[key] && langObj[key] && typeof(en[key]) === "object" && typeof(langObj[key]) === "object") {
			initLanguage(rstObj, en[key], langObj[key]);
		}
	}
}
const dir = './node_modules/monaco-editor/dev/vs/editor/editor.main.nls'
const language = {}
const fs = require('fs')
const read = path => eval('(' + fs.readFileSync(path, 'utf8').replace(/[\s\S]+define\("[a-zA-Z/\.\-]+", ({[\s\S]+})\);$/g, '$1').replace(/,\s+]/g, ']') + ')');
['zh-cn'].forEach(it => {
	language[`${it}`] = {}
	initLanguage(language[`${it}`], read(`${dir}.js`), read(`${dir}.${it}.js`))
})
export function MonacoEditorNls(esbuild) {
	if(esbuild){
		return {
			name: 'esbuild-plugin-monaco-editor-nls',
			setup: build => {
				build.onLoad({ filter: /esm[\\\/]vs[\\\/]nls\.js/ }, async (args) => {
					return {
						contents: getLocalizeCode(),
						loader: 'js'
					}
				})
			}
		}
	}
	return {
		name: 'rollup-plugin-monaco-editor-nls',
		load: path => {
			if(/esm[\\\/]vs[\\\/]nls\.js/.test(path)){
				return getLocalizeCode()
			}
		}
	}
}
function getLocalizeCode(){
	return `
		const mapLangIdx = ${JSON.stringify(mapLangIdx)};
		const mapNlsLang = ${JSON.stringify(language)};
		const locale = localStorage.getItem('magic-locale') || 'zh-cn'
		function _format(message, args) {
			let result;
			if (args.length === 0) {
				result = message;
			}
			else {
				result = message.replace(/\{(\d+)\}/g, function (match, rest) {
					const index = rest[0];
					return typeof args[index] !== 'undefined' ? args[index] : match;
				});
			}
			return result;
		}
		export function localize(data, message) {
			if (locale !== 'en') {
				let idx = mapLangIdx[message] || -1;
				const lang = mapNlsLang[locale]
				if(idx in lang){
					message = lang[idx]; 
				}	
			}
			const args = []
			for(var i = 0, len = arguments.length; i < len; ++i){
				args.push(arguments[i]);
			}
			return _format(message, args);
		}
	`
}