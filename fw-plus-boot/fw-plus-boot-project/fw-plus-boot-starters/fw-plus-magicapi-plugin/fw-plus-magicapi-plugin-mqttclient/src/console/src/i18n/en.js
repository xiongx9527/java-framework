export default {
    mqttclient: {
        subscribe: {
            title: 'Mqtt Client Subscribe Info',
            name: 'Mqtt Client Subscribe',
            form: {
                key: 'DataSource',
                topic: 'Topic',
                qos: 'QoS',
                name: 'Name',
                description: 'Description',
                params: 'Initial Message Params',
                other: 'Other',
                placeholder: {
                    key: 'Please Enter DataSource Key',
                    topic: 'Please Enter Subscribe Topic',
                    qos: 'Please Select QoS',
                    name: 'Please Enter Name',
                    description: 'Please Enter Description',
                    params: 'Please Enter Subscribe Message Params',
                    other: 'Please Enter Subscribe Other JSON Config'
                }
            }
        },
        datasource: {
            title: 'Mqtt Client DataSource',
            name: 'Mqtt Client',
            form: {
                name: 'Name',
                key: 'Key',
                ip: 'IP',
                port: 'Port',
                clientId: 'Client ID',
                username: 'Username',
                password: 'Password',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    ip: 'Please Enter Mqtt IP, Required, For example: 0.0.0.0',
                    port: 'Please Enter Mqtt Port, Required, For example: 1883',
                    clientId: 'Please Enter Client ID, Required',
                    username: 'Please Enter Username, Optional',
                    password: 'Please Enter Password, Optional',
                    other: 'Please Enter Mqtt Client Other Config, eg: {xxxx: "yyyy"}'
                }
            }
        }
    }
}
