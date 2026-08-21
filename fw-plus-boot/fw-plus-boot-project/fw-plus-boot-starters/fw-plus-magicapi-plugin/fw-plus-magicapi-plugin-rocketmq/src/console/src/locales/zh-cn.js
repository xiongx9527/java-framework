export default {
    datasource: {
        extra: {
            producer: '生产',
            consumer: '消费'
        }
    },
    rocketmq: {
        subscribe: {
            title: 'RocketMq 订阅信息',
            name: 'RocketMq 订阅',
            form: {
                key: '数据源Key',
                broadcast: '广播',
                topic: '主题',
                tags: '标签',
                group: '组名',
                name: '名称',
                path: '路径',
                placeholder: {
                    key: '请输入数据源Key',
                    topic: '请输入订阅主题',
                    tags: '请输入订阅标签',
                    group: '请输入订阅组名',
                    name: '请输入名称',
                    path: '请输入路径',
                    other: '请输入 RocketMq 其它配置',
                    params: '请输入 RocketMq 订阅参数'
                }
            },
            navbars: {
                other: '其它',
                params: '订阅参数'
            }
        },
        datasource: {
            title: 'RocketMq 数据源',
            name: 'RocketMq 数据源',
            form: {
                name: '名称',
                key: 'Key',
                nameServer: '服务器地址',
                accessKey: '用户名',
                secretKey: '密码',
                extra: '类型',
                group: '组名',
                properties: '连接参数',
                placeholder: {
                    name: '数据源名称，仅作显示用',
                    key: '数据源Key，后续代码中使用',
                    nameServer: '请输入服务器地址，如：192.168.2.5:9876,...',
                    accessKey: '请输入 RocketMq 访问用户名，可选',
                    secretKey: '请输入 RocketMq 访问密码，可选',
                    extra: '请选择 RocketMq 访问类型',
                    group: '请输入 RocketMq 访问组，可选',
                    properties: '请输入 JSON 对象，如： {"sendMessageTimeout": 3000}'
                }
            }
        }
    }
};
