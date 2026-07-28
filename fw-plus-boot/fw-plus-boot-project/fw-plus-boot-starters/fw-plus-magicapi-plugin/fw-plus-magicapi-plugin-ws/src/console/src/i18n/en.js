export default {
    datasource: {
        extra: {
            producer: 'Producer',
            consumer: 'Consumer'
        }
    },
    ws: {
        subscribe: {
            title: 'WS Subscribe Info',
            name: 'WS Subscribe',
            form: {
                key: 'DataSource',
                name: 'Name',
                businessId: 'Business Id',
                placeholder: {
                    key: 'Please Enter DataSource Key, Required',
                    businessId: 'Please Enter Business Id, Required',
                    name: 'Please Enter Name, Required',
                    description: 'Please Enter WS Description, Option'
                }
            }
        },
        datasource: {
            title: 'WS DataSource',
            name: 'WS',
            form: {
                name: 'Name',
                key: 'Key',
                secret: 'Secret Key',
                description: 'Description',
                properties: 'Properties',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    secret: 'DataSource Secret Key, Required',
                    description: 'Please Enter WS Description, Option',
                    properties: 'JSON object, for example: {"key": "value"}'
                }
            }
        }
    }
}