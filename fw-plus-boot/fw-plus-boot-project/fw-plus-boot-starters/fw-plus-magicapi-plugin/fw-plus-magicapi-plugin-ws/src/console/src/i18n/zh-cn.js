export default {
    datasource: {
        extra: {
            producer: '生产',
            consumer: '消费'
        }
    },
    ws: {
        subscribe: {
            title: 'WS 订阅信息',
            name: 'WS 订阅',
            form: {
                key: '数据源Key',
                name: '名称',
                businessId: '业务标识',
                placeholder: {
                    key: '请输入数据源Key,必填',
                    name: '请输入名称,必填',
                    businessId: '请输入业务标识,必填',
                    description: '描述'
                }
            }
        },
        datasource: {
            title: 'WS 数据源',
            name: 'WS 数据源',
            form: {
                name: '名称',
                key: 'Key',
                secret: '密钥',
                path: '端点路径',
                description: '描述',
                properties: '连接参数',
                placeholder: {
                    name: '请输入名称,必填',
                    key: '请输入Key,必填',
                    secret: '请输入密钥,必填',
                    path: '请输入绑定的 WS 端点路径,如 /ws/api,留空使用全局配置',
                    description: '请输入描述',
                    properties: '请输入 JSON 对象，如： {"key": "value"}'
                }
            }
        }
    }
}
