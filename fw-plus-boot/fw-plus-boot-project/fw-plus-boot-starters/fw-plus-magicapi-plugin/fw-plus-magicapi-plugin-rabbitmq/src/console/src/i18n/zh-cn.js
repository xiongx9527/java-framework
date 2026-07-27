export default {
    rabbitmq: {
        subscribe: {
            title: 'RabbitMq 订阅信息',
            name: 'RabbitMq 订阅',
            form: {
                key: '数据源Key',
                queueName: '队列',
                name: '名称',
                broadcast: '广播',
                path: '路径',
                placeholder: {
                    key: '请输入数据源Key',
                    queueName: '请输入订阅队列',
                    name: '请输入名称',
                    path: '请输入路径',
                    other: '请输入 RabbitMq 其它配置',
                    params: '请输入 RabbitMq 参数'
                }
            },
            navbars: {
                other: '其它',
                params: '订阅参数'
            }
        },
        datasource: {
            title: 'RabbitMq 数据源',
            name: 'RabbitMq 数据源',
            form: {
                name: '名称',
                key: 'Key',
                host: '服务器地址',
                port: '服务器端口',
                username: '用户名',
                password: '密码',
                extra: '类型',
                virtualHost: '虚拟主机',
                properties: '连接参数',
                placeholder: {
                    name: '数据源名称，仅作显示用',
                    key: '数据源Key，后续代码中使用',
                    host: '请输入服务器地址，如：192.168.2.5',
                    port: '请输入服务器端口，如：5672',
                    username: '请输入 RabbitMq 访问用户名，可选',
                    password: '请输入 RabbitMq 访问密码，可选',
                    extra: '请选择 RabbitMq 访问类型',
                    virtualHost: '请输入 RabbitMq 访问虚拟主机，可选',
                    properties: '请输入 JSON 对象，如： {"key": "value"}'
                }
            }
        }
    }
}
