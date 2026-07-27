export default {
    elasticsearch: {
        datasource: {
            title: 'Elasticsearch 数据源',
            name: 'Elasticsearch 数据源',
            form: {
                name: '名称',
                key: 'Key',
                scheme: '协议',
                hostname: 'IP',
                port: '端口',
                username: '用户名',
                password: '密码',
                database: '数据库',
                mode: '集群模式',
                clientType: '客户端模式',
                properties: '连接参数',
                placeholder: {
                    name: '名称，仅显示使用',
                    key: '数据源Key，必填',
                    scheme: '请选择协议，选填',
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