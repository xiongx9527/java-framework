export default {
    datasource: {
        extra: {
            producer: 'Producer',
            consumer: 'Consumer'
        }
    },
    debezium: {
        kafka: {
            form: {
                kafkaServers: 'Kafka',
                kafkaTopic: 'Topic',
                placeholder: {
                    kafkaServers: 'Please Enter Kafka Servers',
                    kafkaTopic: 'Please Enter Kafka Topic'
                }
            }
        },
        datasource: {
            title: 'Debezium DataSource',
            name: 'Debezium',
            form: {
                name: 'Name',
                key: 'Key',
                connectorClass: 'Connector',
                hostname: 'Host',
                port: 'Port',
                username: 'User',
                password: 'Password',
                database: 'Database',
                includeTables: 'Include',
                excludeTables: 'Exclude',
                serverIdType: 'IDType',
                serverIdTypeOptions: {
                    random: 'Random',
                    fixed: 'Fixed'
                },
                properties: 'Properties',
                enabled: 'Enabled',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    connectorClass: 'Please Enter Debezium Connector Class',
                    hostname: 'Please Enter database Server ip',
                    port: 'Please Enter database Server port',
                    username: 'Please Enter database username',
                    password: 'Please Enter database password',
                    database: 'Please Enter database Name',
                    includeTables: 'Please Enter Include Tables, eg: user,order',
                    excludeTables: 'Please Enter Exclude Tables, eg: user,order',
                    serverIdType: 'random / fixed, default: random',
                    properties: 'JSON object, for example: {"key": "value"}',
                    enabled: 'Enabled'
                }
            }
        }
    }
}