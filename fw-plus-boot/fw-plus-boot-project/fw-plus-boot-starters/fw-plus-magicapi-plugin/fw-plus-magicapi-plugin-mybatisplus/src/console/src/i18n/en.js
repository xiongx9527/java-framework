export default {
    mybatisPlusXml: {
        name: 'MyBatis-Plus XML',
        title: 'MyBatis-Plus XML Info',
        form: {
            enabled: 'Enabled',
            name: 'Name',
            path: 'Path',
            dbKey: 'DB Key',
            tableName: 'Table',
            description: 'Description',
            uniqueTip: 'dbKey + table is unique; dbKey cannot be changed after creation',
            defaultDbKeyTip: 'Default datasource key is default. Blank dbKey will also be saved as default.',
            placeholder: {
                name: 'e.g. UserMapper',
                path: 'Resource path, e.g. mapper/user',
                dbKey: 'default',
                tableName: 'e.g. sys_user',
                description: 'XML purpose, namespace, and main statements'
            }
        }
    }
}
