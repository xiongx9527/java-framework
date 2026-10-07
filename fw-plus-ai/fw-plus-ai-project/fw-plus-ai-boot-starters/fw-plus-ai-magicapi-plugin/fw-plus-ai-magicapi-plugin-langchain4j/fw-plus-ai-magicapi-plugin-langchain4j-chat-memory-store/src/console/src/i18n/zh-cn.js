export default {
    langchain4j: {
        type: {
            'chat-memory-store': '对话记忆存储'
        },
        common: {
            name: '名称',
            key: 'Key',
            type: '提供商',
            datasourceKey: '数据源 Key',
            enabled: '启用'
        },
        provider: {
            inMemory: '内存',
            jdbc: 'JDBC',
            redis: 'Redis',
            mongodb: 'MongoDB',
            elasticsearch: 'Elasticsearch'
        },
        params: {
            tableName: '表名',
            keyPrefix: '键前缀',
            ttl: '过期时间',
            collectionName: '集合名',
            indexName: '索引名'
        },
        option: {
            yes: '是',
            no: '否'
        },
        hint: {
            required: '必填',
            optional: '选填',
            datasourceKey: '留空使用 Spring 默认客户端；或填 magic-api 数据源 Key，如 redis 数据源',
            params: '留空使用全局配置默认值',
            ttl: '留空永不过期，如 30m / 12h / 7d'
        }
    }
}
