export default {
    mqtt: {
        datasource: {
            title: 'Mqtt 数据源',
            name: 'Mqtt 数据源',
            form: {
                name: '名称',
                key: 'Key',
                ip: '主机IP',
                port: '端口',
                webPort: '管理端口',
                username: '用户名',
                password: '密码',
                other: '其它',
                placeholder: {
                    name: '数据源名称，仅作显示用',
                    key: '数据源Key，后续代码中使用',
                    ip: '请输入Mqtt服务ip，必填，例如：0.0.0.0',
                    port: '请输入Mqtt服务端口，必填，例如：1883',
                    webPort: '请输入Mqtt服务web端口，必填，例如：8083',
                    username: '请输入用户名,选填',
                    password: '请输入密码,选填',
                    other: '请输入 Mqtt 访问相关配置，如： {xxxx: "yyyy"}'
                }
            }
        }
    }
}