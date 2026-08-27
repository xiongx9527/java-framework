export default {
    mybatisPlusXml: {
        name: 'MyBatis-Plus XML',
        title: 'MyBatis-Plus XML 信息',
        form: {
            enabled: '启用',
            name: '名称',
            path: '路径',
            dbKey: '数据源',
            tableName: '表名',
            description: '说明',
            uniqueTip: 'dbKey + 表名唯一，dbKey 创建后不可修改',
            defaultDbKeyTip: '默认数据源 key 固定写 default；dbKey 留空保存时也会按 default 处理。',
            placeholder: {
                name: '如 UserMapper',
                path: '资源路径，如 mapper/user',
                dbKey: '默认 default',
                tableName: '如 sys_user',
                description: 'XML 用途、namespace、主要 statement 说明'
            }
        }
    }
}
