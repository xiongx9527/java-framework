export default {
    langchain4j: {
        type: {
            'chat-memory-store': 'Chat Memory Store'
        },
        common: {
            name: 'Name',
            key: 'Key',
            type: 'Provider',
            datasourceKey: 'Datasource Key',
            enabled: 'Enabled'
        },
        provider: {
            inMemory: 'In-Memory',
            jdbc: 'JDBC',
            redis: 'Redis',
            mongodb: 'MongoDB',
            elasticsearch: 'Elasticsearch'
        },
        params: {
            tableName: 'Table Name',
            keyPrefix: 'Key Prefix',
            ttl: 'TTL',
            collectionName: 'Collection',
            indexName: 'Index'
        },
        option: {
            yes: 'Yes',
            no: 'No'
        },
        hint: {
            required: 'Required',
            optional: 'Optional',
            datasourceKey: 'Empty for Spring default client; or a magic-api datasource key',
            params: 'Empty for global default',
            ttl: 'Empty for no expiry, e.g. 30m / 12h / 7d'
        }
    }
}
