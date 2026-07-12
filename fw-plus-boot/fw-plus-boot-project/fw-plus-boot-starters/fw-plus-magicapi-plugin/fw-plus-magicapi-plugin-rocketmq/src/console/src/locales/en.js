export default {
    datasource: {
        extra: {
            producer: 'Producer',
            consumer: 'Consumer'
        }
    },
    rocketmq: {
        subscribe: {
            title: 'RocketMq Subscribe Info',
            name: 'RocketMq Subscribe',
            form: {
                key: 'DataSource',
                broadcast: 'Broadcast',
                topic: 'Topic',
                tags: 'Tags',
                group: 'Group',
                name: 'Name',
                path: 'Path',
                placeholder: {
                    key: 'Please Enter DataSource Key',
                    topic: 'Please Enter Subscribe Topic',
                    tags: 'Please Enter Subscribe Tags',
                    group: 'Please Enter Subscribe Group',
                    name: 'Please Enter Name',
                    path: 'Please Enter Path',
                    other: 'Please Enter RocketMq Other Config',
                    params: 'Please Enter RocketMq Subscribe Params'
                }
            },
            navbars: {
                other: 'Other',
                params: 'Subscribe Params'
            }
        },
        datasource: {
            title: 'RocketMq DataSource',
            name: 'RocketMq',
            form: {
                name: 'Name',
                key: 'Key',
                nameServer: 'Name Server',
                accessKey: 'Access Key',
                secretKey: 'Secret Key',
                extra: 'Type',
                group: 'Group Name',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    nameServer: 'Please Enter Server Address，eg：192.168.2.5:9876,...',
                    accessKey: 'Please Enter Database username, Optional',
                    secretKey: 'Please Enter Database password, Optional',
                    extra: 'Please Enter RocketMq Access Type',
                    group: 'Please Enter RocketMq Access Group, Optional',
                    other: 'Please Enter RocketMq Other Config，eg： {xxxx: "yyyy"}'
                }
            }
        }
    }
};
