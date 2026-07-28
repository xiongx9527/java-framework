export default {
    mqttclient: {
        subscribe: {
            title: 'Mqtt Client 订阅信息',
            name: 'Mqtt Client 订阅',
            form: {
                key: '数据源Key',
                topic: '主题',
                qos: 'QoS',
                name: '名称',
                description: '描述',
                params: '消息',
                other: '其它',
                placeholder: {
                    key: '请输入数据源Key',
                    topic: '请输入订阅主题',
                    qos: '请选择服务质量',
                    name: '请输入名称',
                    description: '请输入描述',
                    params: '请输入订阅消息参数',
                    other: '请输入订阅其他JSON配置'
                }
            }
        },
        datasource: {
            title: 'Mqtt Client 数据源',
            name: 'Mqtt Client 数据源',
            form: {
                name: '名称',
                key: 'Key',
                ip: 'IP',
                port: '端口',
                clientId: '客户端ID',
                username: '用户名',
                password: '密码',
                properties: '连接参数',
                placeholder: {
                    name: '数据源名称，仅作显示用',
                    key: '数据源Key，后续代码中使用',
                    ip: '请输入Mqtt服务ip，必填，例如：0.0.0.0',
                    port: '请输入Mqtt服务端口，必填，例如：1883',
                    clientId: '请输入客户端ID，必填',
                    username: '请输入用户名,选填',
                    password: '请输入密码,选填',
                    properties: '请输入 JSON 对象，如： {"key": "value"}'
                }
            }
        }
    }
}
