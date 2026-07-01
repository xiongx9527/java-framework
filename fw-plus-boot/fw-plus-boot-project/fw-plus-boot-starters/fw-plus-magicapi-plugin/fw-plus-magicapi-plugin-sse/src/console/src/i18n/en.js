export default {
    datasource: {
        extra: {
            producer: 'Producer',
            consumer: 'Consumer'
        }
    },
    sse: {
        subscribe: {
            title: 'SSE Subscribe Info',
            name: 'SSE Subscribe',
            form: {
                key: 'DataSource',
                name: 'Name',
                businessId: 'Business Id',
                placeholder: {
                    key: 'Please Enter DataSource Key, Required',
                    businessId: 'Please Enter Business Id, Required',
                    name: 'Please Enter Name, Required',
                    description: 'Please Enter SSE Description, Option'
                }
            }
        },
        datasource: {
            title: 'SSE DataSource',
            name: 'SSE',
            form: {
                name: 'Name',
                key: 'Key',
                secret: 'Secret Key',
                mcp: 'Enable MCP',
                description: 'Description',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    secret: 'DataSource Secret Key, Required',
                    description: 'Please Enter Description, Option',
                    other: 'Please Enter Other Config'
                }
            }
        }
    }
}