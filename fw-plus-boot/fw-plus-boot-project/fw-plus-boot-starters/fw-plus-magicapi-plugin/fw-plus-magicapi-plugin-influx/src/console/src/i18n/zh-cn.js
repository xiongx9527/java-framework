export default {
    influx: {
        datasource: {
            title: 'Influx 数据源',
            name: 'Influx 数据源',
            form: {
                name: '名称',
                key: 'Key',
                url: 'URL地址',
                username: '用户名',
                password: '密码',
                token: '访问令牌',
                org: '组织',
                bucket: '桶名称',
                properties: '连接参数',
                placeholder: {
                    name: '名称，仅显示使用',
                    key: '数据源Key，必填',
                    url: '请输入URL地址，必填',
                    username: '请输入用户名，选填',
                    password: '请输入密码，选填',
                    token: '请输入访问令牌，选填',
                    org: '请输入组织名称，选填',
                    bucket: '请输入桶名称，选填',
                    properties: '请输入 JSON 对象，如： {"key": "value"}'
                }
            }
        }
    }
}