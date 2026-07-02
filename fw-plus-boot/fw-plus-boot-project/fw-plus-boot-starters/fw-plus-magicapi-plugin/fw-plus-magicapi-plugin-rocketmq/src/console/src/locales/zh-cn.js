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
                topic: '主题',
                tag: '标签',
                name: '名称',
                path: '路径',
                placeholder: {
                    key: '请输入数据源Key',
                    topic: '请输入订阅主题',
                    tag: '请输入订阅标签',
                    name: '请输入名称',
                    path: '请输入路径',
                    description: '请输入 RocketMq 订阅描述'
                }
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
                other: '其他配置',
                placeholder: {
                    name: '数据源名称，仅作显示用',
                    key: '数据源Key，后续代码中使用',
                    nameServer: '请输入服务器地址，如：192.168.2.5:9876,...',
                    accessKey: '请输入 RocketMq 访问用户名，可选',
                    secretKey: '请输入 RocketMq 访问密码，可选',
                    extra: '请选择 RocketMq 访问类型',
                    group: '请输入 RocketMq 访问组，可选',
                    other: '请输入 RocketMq 其他配置，如： {xxxx: "yyyy"}'
                }
            }
        }
    }
};
