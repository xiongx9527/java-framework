export default {
    datasource: {
        extra: {
            producer: '生产',
            consumer: '消费'
        }
    },
    pulsar: {
        subscribe: {
            title: 'Pulsar 订阅信息',
            name: 'Pulsar 订阅',
            form: {
                key: '数据源Key',
                topic: '主题',
                broadcast: '广播',
                batchIndexAcknowledgmentEnabled: '批量确认',
                name: '名称',
                path: '路径',
                placeholder: {
                    key: '请输入数据源Key',
                    topic: '请输入订阅主题',
                    group: '请输入订阅分组',
                    name: '请输入名称',
                    path: '请输入路径',
                    other: '请输入Pulsar其它json配置',
                    params: '请输入Pulsar订阅消息'
                }
            },
            navbars: {
                other: '其它',
                params: '订阅参数'
            }
        },
        datasource: {
            title: 'Pulsar 数据源',
            name: 'Pulsar 数据源',
            form: {
                name: '名称',
                key: 'Key',
                servers: 'broker地址',
                serviceUrls: 'admin地址',
                username: '用户名',
                password: '密码',
                tenant: '租户',
                namespace: '命名空间',
                other: '其它',
                placeholder: {
                    name: '数据源名称，仅作显示用',
                    key: '数据源Key，后续代码中使用',
                    servers: '请输入 Pulsar 服务器地址，如：127.0.0.1:6650,...',
                    serviceUrls: '请输入 Pulsar 服务器地址，如：http://127.0.0.1:8080,...',
                    username: '请输入 Pulsar 访问用户名，可选',
                    password: '请输入 Pulsar 访问密码，可选',
                    tenant: '请选择 Pulsar 租户',
                    namespace: '请输入 Pulsar 命名空间',
                    other: '请输入 Pulsar 访问相关配置，如： {xxxx: "yyyy"}'
                }
            }
        }
    }
}