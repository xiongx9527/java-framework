import config from '../../package.json'
import $i from './i18n.js'
let MAGIC_API_VERSION_TEXT = config.version
let MAGIC_API_VERSION = 'V' + MAGIC_API_VERSION_TEXT.replace(/\./g, '_')
const constants = {
		BASE_URL: '', //UI 对应的接口路径
		WEBSOCKET_SERVER: '', //WebSocket服务地址
		SERVER_URL: '', //接口对应的路径
		AUTO_SAVE: true, // 是否自动保存
		user: null,
		DECORATION_TIMEOUT: 10000,
		CHECK_UPDATE: true,
		BLOCK_CLOSE: true,
		MAGIC_API_VERSION_TEXT,
		MAGIC_API_VERSION,
		API_DEFAULT_METHOD: 'GET',
		CLIENT_ID: 'none',
		HEADER_REQUEST_CLIENT_ID: 'Magic-Request-Client-Id',
		HEADER_REQUEST_SCRIPT_ID: 'Magic-Request-Script-Id',
		HEADER_REQUEST_BREAKPOINTS: 'Magic-Request-Breakpoints',
		HEADER_RESPONSE_MAGIC_CONTENT_TYPE: 'ma-content-type',
		HEADER_APPLICATION_STREAM: 'application/octet-stream',
		HEADER_CONTENT_DISPOSITION: 'ma-content-disposition',
		HEADER_MAGIC_TOKEN: 'magic-token',
		HEADER_MAGIC_TOKEN_VALUE: 'unauthorization',
		IGNORE_VERSION: 'ignore-version',
		RECENT_OPENED_TAB: 'recent_opened_tab',
		RECENT_OPENED: 'recent_opened',
		RESPONSE_CODE_DEBUG: 1000,
		RESPONSE_CODE_SCRIPT_ERROR: -1000,
		RESPONSE_NO_PERMISSION: -10,
		DEFAULT_EXPAND: true,
		LOGINED: false,
		LOG_MAX_ROWS: Infinity,
		LOCKED: '1',
		UNLOCK: '0',
		STORE: {
			theme: 'theme',
			token: 'token'
		},
		PLUGINS_DATA: [],
	    PLUGINS: [],
		GLOBAL: {
			parameters: [],
			headers: []
		},
		THEME: 'default',
		JDBC_DRIVERS: [
			'com.mysql.jdbc.Driver',
			'com.mysql.cj.jdbc.Driver',
			'oracle.jdbc.driver.OracleDriver',
			'org.postgresql.Driver',
			'com.microsoft.sqlserver.jdbc.SQLServerDriver',
			'com.ibm.db2.jcc.DB2Driver'
		],
		DATASOURCE_TYPES: [
			'com.zaxxer.hikari.HikariDataSource',
			'com.alibaba.druid.pool.DruidDataSource',
			'org.apache.tomcat.jdbc.pool.DataSource',
			'org.apache.commons.dbcp2.BasicDataSource'
		],
		OPTIONS: [],
		EDITOR_FONT_FAMILY: 'JetBrainsMono, Consolas, "Courier New",monospace, 微软雅黑',
		EDITOR_FONT_SIZE: 14,
		FONT_LIGATURES: true,
		VALIDATE_TYPES: [
			{ value: 'pass', text: $i('message.noValidate')},
			{ value: 'expression', text: $i('message.validateExpression')},
			{ value: 'pattern', text: $i('message.validatePattern')}
		],
		DEFAULT_VALIDATE_TYPE: 'pass',
		REQUEST_PARAMETER_TYPES: [
			{value: 'String', text: 'String'},
			{value: 'Boolean', text: 'Boolean'},
			{value: 'Integer', text: 'Integer'},
			{value: 'Date', text: 'Date'},
			{value: 'Double', text: 'Double'},
			{value: 'Long', text: 'Long'},
			{value: 'Short', text: 'Short'},
			{value: 'Float', text: 'Float'},
			{value: 'Byte', text: 'Byte'},
			{value: 'MultipartFile', text: 'MultipartFile'},
			{value: 'MultipartFiles', text: 'MultipartFiles'}
		],
		GLOBAL_PARAMETER_TYPES: [
			{value: 'String', text: 'String'},
			{value: 'Boolean', text: 'Boolean'},
			{value: 'Integer', text: 'Integer'},
			{value: 'Date', text: 'Date'},
			{value: 'Double', text: 'Double'},
			{value: 'Long', text: 'Long'},
			{value: 'Short', text: 'Short'},
			{value: 'Float', text: 'Float'},
			{value: 'Byte', text: 'Byte'},
		],
		DEFAULT_REQUEST_PARAMETER_TYPE: 'String',

		REQUEST_SIMPLE_TYPES: [
			{value: 'String', text: 'String'},
			{value: 'Boolean', text: 'Boolean'},
			{value: 'Integer', text: 'Integer'},
			{value: 'Date', text: 'Date'},
			{value: 'Double', text: 'Double'},
			{value: 'Long', text: 'Long'},
			{value: 'Short', text: 'Short'},
			{value: 'Float', text: 'Float'},
			{value: 'Byte', text: 'Byte'},
		],
		DEFAULT_REQUEST_SIMPLE_TYPE: 'String',
		REQUEST_METHODS: [
			{value: 'GET', text: 'GET'},
			{value: 'POST', text: 'POST'},
			{value: 'PUT', text: 'PUT'},
			{value: 'DELETE', text: 'DELETE'},
			{value: 'HEAD', text: 'HEAD'},
			{value: 'PATCH', text: 'PATCH'}
		],
		DEFAULT_REQUEST_METHOD: 'GET',
		FUNCTION_RETURN_TYPES: [
			{value: 'java.lang.Number', text: $i('fn.number')},
			{value: 'java.lang.String', text: $i('fn.string')},
			{value: 'java.util.Collection', text: $i('fn.collection')},
			{value: 'java.util.Map', text: 'Map'},
			{value: 'java.lang.Object', text: 'Object'}
		],
		BODY_DATA_TYPES: [
			{value: 'String', text: 'String'},
			{value: 'Integer', text: 'Integer'},
			{value: 'Double', text: 'Double'},
			{value: 'Long', text: 'Long'},
			{value: 'Short', text: 'Short'},
			{value: 'Float', text: 'Float'},
			{value: 'Byte', text: 'Byte'},
			{value: 'Boolean', text: 'Boolean'},
			{value: 'Date', text: 'Date'},
			{value: 'Object', text: 'Object'},
			{value: 'Array', text: 'Array'},
			{value: 'Any', text: 'Any'}
		],
		DEFAULT_FUNCTION_RETURN_TYPE: 'java.lang.Object',
		config: {}
	}
	
	export default constants
