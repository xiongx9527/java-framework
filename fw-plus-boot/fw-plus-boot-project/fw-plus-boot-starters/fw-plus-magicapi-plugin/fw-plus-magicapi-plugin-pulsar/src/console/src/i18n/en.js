export default {
    datasource: {
        extra: {
            producer: 'Producer',
            consumer: 'Consumer'
        }
    },
    pulsar: {
        subscribe: {
            title: 'Pulsar Subscribe Info',
            name: 'Pulsar Subscribe',
            form: {
                key: 'DataSource',
                topic: 'Topic',
                broadcast: 'Broadcast',
                batchIndexAcknowledgmentEnabled: 'Batch Index Acknowledgment Enabled',
                name: 'Name',
                path: 'Path',
                placeholder: {
                    key: 'Please Enter DataSource Key',
                    topic: 'Please Enter Subscribe Topic',
                    group: 'Please Enter Subscribe Group',
                    name: 'Please Enter Name',
                    path: 'Please Enter Path',
                    other: 'Please Enter Pulsar Subscribe Other json Config',
                    params: 'Please Enter Pulsar Subscribe Params'
                }
            },
            navbars: {
                other: 'Other',
                params: 'Subscribe Params'
            }
        },
        datasource: {
            title: 'Pulsar DataSource',
            name: 'Pulsar',
            form: {
                name: 'Name',
                key: 'Key',
                servers: 'Brokers',
                serviceUrls: 'Admin',
                username: 'User Name',
                password: 'Password',
                tenant: 'Tenant',
                namespace: 'Namespace',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    servers: 'Please Enter Pulsar Server Addresses, eg: 127.0.0.1:6650,...',
                    serviceUrls: 'Please Enter Pulsar Service Http Url, eg: http://127.0.0.1:8080,...',
                    username: 'Please Enter Pulsar username, Optional',
                    password: 'Please Enter Pulsar password, Optional',
                    tenant: 'Please Enter Pulsar Tenant',
                    namespace: 'Please Enter Pulsar Namespace',
                    other: 'Please Enter Pulsar Other Config, eg: {xxxx: "yyyy"}'
                }
            }
        }
    }
}