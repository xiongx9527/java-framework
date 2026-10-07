export default {
    redis: {
        datasource: {
            common: {
                title: 'Redis DataSource',
                name: 'Redis'
            },
            hint: {
                required: 'Required',
                optional: 'Optional',
                select: 'Type-in allowed',
                json: 'JSON object, for example: {"key": "value"}',
                jsonInvalid: 'Properties is not a valid JSON object',
                requiredField: '{0} is required'
            },
            field: {
                name: 'Name',
                key: 'Key',
                hostname: 'Hostname',
                port: 'Port',
                username: 'Username',
                password: 'Password',
                database: 'Database',
                mode: 'Mode',
                clientType: 'Client',
                properties: 'Properties'
            },
            extra: {
                mode: {
                    standalone: 'Standalone',
                    sentinel: 'Sentinel',
                    cluster: 'Cluster'
                },
                clientType: {
                    lettuce: 'Lettuce',
                    jedis: 'Jedis'
                }
            }
        }
    }
}
