export default {
    rabbitmq: {
        subscribe: {
            title: 'RabbitMq Subscribe Info',
            name: 'RabbitMq Subscribe',
            form: {
                key: 'DataSource',
                queueName: 'Queue Name',
                name: 'Name',
                path: 'Path',
                broadcast: 'Broadcast',
                placeholder: {
                    key: 'Please Enter DataSource Key',
                    queueName: 'Please Enter Subscribe Queue Name',
                    name: 'Please Enter Name',
                    path: 'Please Enter Path',
                    other: 'Please Enter RabbitMq other config',
                    params: 'Please Enter Subscribe Params'
                }
            },
            navbars: {
                other: 'Other',
                params: 'Subscribe Params'
            }
        },
        datasource: {
            title: 'RabbitMq DataSource',
            name: 'RabbitMq',
            form: {
                name: 'Name',
                key: 'Key',
                host: 'Host',
                port: 'Port',
                username: 'User Name',
                password: 'Password',
                extra: 'Type',
                virtualHost: 'Virtual Host',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    host: 'Please Enter Server Host, eg: 192.168.2.5,...',
                    port: 'Please Enter Server Port, eg: 5672,...',
                    username: 'Please Enter Datasource username, Optional',
                    password: 'Please Enter Datasource password, Optional',
                    extra: 'Please Enter RabbitMq Access Type',
                    virtualHost: 'Please Enter RabbitMq Virtual Host, Optional',
                    other: 'Please Enter RabbitMq Other Config, eg: {xxxx: "yyyy"}'
                }
            }
        }
    }
}