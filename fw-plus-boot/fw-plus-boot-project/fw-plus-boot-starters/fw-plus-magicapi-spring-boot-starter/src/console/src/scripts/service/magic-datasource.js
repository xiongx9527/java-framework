import request from '../request.js'
import $i from '../i18n.js'
import modal from '../../components/common/dialog/magic-modal.js'
import JavaClass from '../editor/java-class.js'
let findResources
JavaClass.setExtensionAttribute('org.ssssssss.magicapi.modules.db.SQLModule', () => {
	return findResources && (findResources('datasource')[0]?.children || []).filter(it => it.key).map(it => {
		return {
			name: it.key,
			type: 'org.ssssssss.magicapi.modules.db.SQLModule',
			comment: it.name
		}
	}) || []
})
export default {
	injectResources: fn => findResources = fn,
	requireScript: false,
	doTest: info => {
		request.sendJson('/datasource/myjdbc/test', info).success(res => {
			if(res === 'ok'){
				modal.alert($i('datasource.connected'), $i('datasource.test'))
			}else {
				modal.alert($i('datasource.connectFailed', res), $i('datasource.test'))
			}
		})
	}
}
