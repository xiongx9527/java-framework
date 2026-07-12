import $i from '../i18n.js'
export default {
	getIcon: item => 'function',
	name: $i('fn.name'),
	runnable: false,
	requirePath: true,
	merge: item => {
		item.parameters = item.parameters || []
		return item
	}
}