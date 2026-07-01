export default {
    mqtt: {
        datasource: {
            title: 'Mqtt DataSource',
            name: 'Mqtt',
            form: {
                name: 'Name',
                key: 'Key',
                ip: 'IP',
                port: 'Port',
                webPort: 'WebPort',
                username: 'Username',
                password: 'Password',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    ip: 'Please Enter Mqtt IP, Required, For example: 0.0.0.0',
                    port: 'Please Enter Mqtt Port, Required, For example: 1883',
                    webPort: 'Please Enter Mqtt Web Port, Required, For example: 8083',
                    username: 'Please Enter Username, Optional',
                    password: 'Please Enter Password, Optional',
                    other: 'Please Enter Mqtt Other Config, eg: {xxxx: "yyyy"}'
                }
            }
        }
    }
}