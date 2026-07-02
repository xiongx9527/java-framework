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
                key: 'DataSource Key',
                topic: 'Topic',
                tag: 'Tag',
                name: 'Name',
                path: 'Path',
                placeholder: {
                    key: 'Please Enter DataSource Key',
                    topic: 'Please Enter Subscribe Topic',
                    tag: 'Please Enter Subscribe Tag Expression',
                    name: 'Please Enter Name',
                    path: 'Please Enter Path',
                    description: 'Please Enter RocketMq Subscribe Description'
                }
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
                other: 'Other Config',
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
