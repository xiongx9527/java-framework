export function MonacoExcludeLanguages(esbuild) {
	if(esbuild){
		return {
			name: 'esbuild-plugin-monaco-exclude-languages',
			setup(build){
				build.onLoad({ filter:  /language\/typescript/ }, async () => {
					return {
						contents: ``,
						loader: 'ts'
					}
				})
			}
		}
	}
	return {
		name: 'rollup-plugin-monaco-exclude-languages',
		load(path){
			if(path.indexOf(`/monaco-editor/esm/vs/language/typescript/`) > 0){
				return ``
			}
		}
	}
}
