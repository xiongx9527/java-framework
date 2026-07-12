export default {
    datasource: {
        extra: {
            producer: 'Producer',
            consumer: 'Consumer'
        }
    },
    kafka: {
        subscribe: {
            title: 'Kafka Subscribe Info',
            name: 'Kafka Subscribe',
            form: {
                key: 'DataSource',
                topic: 'Topic',
                broadcast: 'Broadcast',
                ackMode: 'Ack Mode',
                concurrency: 'Concurrency',
                name: 'Name',
                path: 'Path',
                placeholder: {
                    key: 'Please Enter DataSource Key',
                    topic: 'Please Enter Subscribe Topic',
                    name: 'Please Enter Name',
                    path: 'Please Enter Path',
                    params: 'Please Enter Kafka Subscribe Params',
                    other: 'Please Enter Kafka Subscribe Other JSON Config'
                }
            },
            navbars: {
                other: 'Other',
                params: 'Subscribe Params'
            }
        },
        datasource: {
            title: 'Kafka DataSource',
            name: 'Kafka',
            form: {
                name: 'Name',
                key: 'Key',
                servers: 'Servers',
                username: 'User Name',
                password: 'Password',
                extra: 'Type',
                group: 'Group Id',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    servers: 'Please Enter Kafka Server Addresses, eg: 192.168.2.5:9092,...',
                    username: 'Please Enter Kafka username, Optional',
                    password: 'Please Enter Kafka password, Optional',
                    extra: 'Please Enter Kafka Access Type',
                    other: 'Please Enter Kafka Other Config, eg: {xxxx: "yyyy"}'
                }
            }
        }
    }
}