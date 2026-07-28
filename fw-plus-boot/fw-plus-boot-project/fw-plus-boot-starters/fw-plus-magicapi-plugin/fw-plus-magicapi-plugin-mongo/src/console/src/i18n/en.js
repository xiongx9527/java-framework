export default {
    mongo: {
        datasource: {
            title: 'Mongo DataSource',
            name: 'Mongo',
            extra: {
                mode: {
                    standalone: 'Standalone',
                    replicaSet: 'Replica Set',
                    cluster: 'Sharded Cluster'
                }
            },
            form: {
                name: 'Name',
                key: 'Key',
                hostname: 'Hostname',
                port: 'Port',
                username: 'Username',
                password: 'Password',
                properties: 'Properties',
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
                    properties: 'JSON object, for example: {"key": "value"}'
                }
            }
        }
    }
}