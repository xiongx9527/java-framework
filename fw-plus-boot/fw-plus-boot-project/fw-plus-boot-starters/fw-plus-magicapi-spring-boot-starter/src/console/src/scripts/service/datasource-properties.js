const PROPERTIES_DATASOURCE_TYPES = new Set([
	'langchain4j-datasource',
	'debezium-datasource',
	'dtp-datasource',
	'elasticsearch-datasource',
	'influx-datasource',
	'kafka-datasource',
	'mongo-datasource',
	'mqtt-datasource',
	'mqttclient-datasource',
	'pulsar-datasource',
	'rabbitmq-datasource',
	'redis-datasource',
	'rocketmq-datasource',
	's3-datasource',
	'ws-datasource'
])

const isJsonObject = value => value !== null && typeof value === 'object' && !Array.isArray(value)

export const usesDatasourceProperties = type => PROPERTIES_DATASOURCE_TYPES.has(type)

export const prepareDatasourceProperties = (type, info = {}) => {
	const prepared = { ...info }
	if (!usesDatasourceProperties(type)) {
		return prepared
	}
	if (prepared.properties == null) {
		prepared.properties = {}
	}
	prepared.propertiesText = JSON.stringify(prepared.properties, null, 2)
	return prepared
}

export const normalizeDatasourceProperties = (type, info = {}) => {
	const normalized = { ...info }
	if (!usesDatasourceProperties(type)) {
		return normalized
	}

	let properties = normalized.properties
	if (typeof normalized.propertiesText === 'string') {
		const text = normalized.propertiesText.trim()
		if (!text) {
			properties = {}
		} else {
			try {
				properties = JSON.parse(text)
			} catch (error) {
				const invalidJson = new Error('datasource.properties.invalidJson')
				invalidJson.cause = error
				throw invalidJson
			}
		}
	}

	if (properties == null) {
		properties = {}
	}
	if (!isJsonObject(properties)) {
		throw new Error('datasource.properties.objectRequired')
	}

	normalized.properties = properties
	delete normalized.propertiesText
	return normalized
}
