export default {
    datasource: {
        extra: {
            producer: '生产',
            consumer: '消费'
        }
    },
    kafka: {
        subscribe: {
            title: 'Kafka 订阅信息',
            name: 'Kafka 订阅',
            form: {
                key: '数据源Key',
                topic: '主题',
                broadcast: '广播',
                ackMode: '确认方式',
                concurrency: '并发数',
                name: '名称',
                path: '路径',
                placeholder: {
                    key: '请输入数据源Key',
                    topic: '请输入订阅主题',
                    name: '请输入名称',
                    path: '请输入路径',
                    params: '请输入 Kafka 订阅消息',
                    other: '请输入Kafka 订阅其他JSON配置'
                }
            },
            navbars: {
                other: '其它',
                params: '订阅参数'
            }
        },
        datasource: {
            title: 'Kafka 数据源',
            name: 'Kafka 数据源',
            form: {
                name: '名称',
                key: 'Key',
                servers: '服务地址',
                username: '用户名',
                password: '密码',
                extra: '类型',
                group: '访问组',
                other: '其它',
                placeholder: {
                    name: '数据源名称，仅作显示用',
                    key: '数据源Key，后续代码中使用',
                    servers: '请输入 Kafka 服务器地址，如：192.168.2.5:9092,...',
                    username: '请输入 Kafka 访问用户名，可选',
                    password: '请输入 Kafka 访问密码，可选',
                    extra: '请选择 Kafka 访问类型',
                    group: '请输入 Kafka 访问组， 可选',
                    other: '请输入 Kafka 访问相关配置，如： {xxxx: "yyyy"}'
                }
            }
        }
    }
}