export default {
    datasource: {
        extra: {
            producer: '生产',
            consumer: '消费'
        }
    },
    debezium: {
        kafka: {
            form: {
                kafkaServers: 'Kafka',
                kafkaTopic: 'Topic',
                placeholder: {
                    kafkaServers: '请输入Kafka地址，必填',
                    kafkaTopic: '请输入Kafka主题，必填'
                }
            }
        },
        datasource: {
            title: 'Debezium 数据源',
            name: 'Debezium 数据源',
            form: {
                name: '名称',
                key: 'Key',
                connectorClass: '连接器',
                hostname: '主机',
                port: '端口',
                username: '用户',
                password: '密码',
                database: '数据库',
                includeTables: '包含表',
                excludeTables: '排除表',
                serverIdType: 'ID策略',
                serverIdTypeOptions: {
                    random: '随机（默认）',
                    fixed: '固定'
                },
                other: '其它',
                enabled: '启用',
                placeholder: {
                    name: '数据源名称，仅显示使用',
                    key: '数据源Key，必填',
                    connectorClass: '请输入 Debezium 连接器类，必填',
                    hostname: '请输入数据库IP，必填',
                    port: '请输入数据库端口，必填',
                    database: '请输入数据库名称，必填',
                    username: '请输入数据库用户名，必填',
                    password: '请输入数据库密码，必填',
                    includeTables: '包含表,例如：user,order',
                    excludeTables: '排除表,例如：user,order',
                    enabled: '是否启用'
                }
            }
        }
    }
}