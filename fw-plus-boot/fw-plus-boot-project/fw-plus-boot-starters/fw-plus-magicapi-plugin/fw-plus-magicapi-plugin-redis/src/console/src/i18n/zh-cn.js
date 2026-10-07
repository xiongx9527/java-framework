export default {
    redis: {
        datasource: {
            common: {
                title: 'Redis 数据源',
                name: 'Redis 数据源'
            },
            hint: {
                required: '必填',
                optional: '选填',
                select: '可直接输入',
                json: '请输入 JSON 对象，如： {"key": "value"}',
                jsonInvalid: '连接参数不是合法的 JSON 对象',
                requiredField: '{0} 为必填字段'
            },
            field: {
                name: '名称',
                key: 'Key',
                hostname: 'IP',
                port: '端口',
                username: '用户名',
                password: '密码',
                database: '数据库',
                mode: '集群模式',
                clientType: '客户端',
                properties: '连接参数'
            },
            extra: {
                mode: {
                    standalone: '单机模式',
                    sentinel: '哨兵模式',
                    cluster: '集群模式'
                },
                clientType: {
                    lettuce: 'Lettuce',
                    jedis: 'Jedis'
                }
            }
        }
    }
}
