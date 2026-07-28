export default {
    mongo: {
        datasource: {
            title: 'Mongo 数据源',
            name: 'Mongo 数据源',
            extra: {
                mode: {
                    standalone: '单机模式',
                    replicaSet: '副本集',
                    cluster: '分片集群'
                }
            },
            form: {
                name: '名称',
                key: 'Key',
                hostname: 'IP',
                port: '端口',
                username: '用户名',
                password: '密码',
                database: '数据库',
                mode: '集群模式',
                properties: '连接参数',
                placeholder: {
                    name: '名称，仅显示使用',
                    key: '数据源Key，必填',
                    hostname: '请输入IP，必填',
                    port: '请输入端口，必填',
                    username: '请输入用户名，选填',
                    password: '请输入密码，选填',
                    database: '请输入数据库，必填',
                    mode: '集群模式，选填',
                    clientType: '客户端模式，选填',
                    properties: '请输入 JSON 对象，如： {"key": "value"}'
                }
            }
        }
    }
}