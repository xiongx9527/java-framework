export default {
    redis: {
        datasource: {
            title: 'Redis DataSource',
            name: 'Redis',
            extra: {
                mode: {
                    standalone: 'Standalone',
                    sentinel: 'Sentinel',
                    cluster: 'Cluster'
                },
                clientType: {
                    lettuce: 'Lettuce',
                    jedis: 'Jedis'
                }
            },
            form: {
                name: 'Name',
                key: 'Key',
                hostname: 'Hostname',
                port: 'Port',
                username: 'Username',
                password: 'Password',
                database: 'Database',
                mode: 'Mode',
                clientType: 'Client',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    hostname: 'Please Enter Server ip, Required',
                    port: 'Please Enter Server port, Required',
                    username: 'Please Enter username, Option',
                    password: 'Please Enter password, Option',
                    database: 'Please Enter database, Required',
                    mode: 'Please Enter mode, Option',
                    clientType: 'Please Enter clientType, Option',
                    other: 'Please Enter Redis Other Config, eg: {xxxx: "yyyy"}'
                }
            }
        }
    }
}