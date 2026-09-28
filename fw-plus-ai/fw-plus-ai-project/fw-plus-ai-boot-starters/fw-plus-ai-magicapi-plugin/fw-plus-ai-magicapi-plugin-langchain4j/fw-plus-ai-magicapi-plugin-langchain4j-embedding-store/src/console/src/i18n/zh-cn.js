export default {
    langchain4j: {
        type: {
            'embedding-store': '向量存储'
        },
        common: {
            name: '名称',
            key: 'Key',
            provider: 'Provider',
            enabled: '启用',
            embeddingModelKey: '关联向量模型',
            options: 'Provider 参数'
        },
        option: {
            yes: '是',
            no: '否',
            true: '是',
            false: '否'
        },
        hint: {
            required: '必填',
            optional: '选填',
            duration: '如 60s / 2m / PT1M，留空使用默认值',
            tags: '多个值用英文逗号分隔',
            switch: '不选表示不设置',
            select: '可直接输入',
            empty: '当前 provider 无额外参数',
            embeddingModelKey: '选择或输入已启用的向量模型 key'
        },
        provider: {
            'elasticsearch': 'Elasticsearch',
            'in-memory': '内存',
            'milvus': 'Milvus',
            'neo4j': 'Neo4j',
            'qdrant': 'Qdrant',
            'redis': 'Redis',
            'weaviate': 'Weaviate',
        },
        field: {
            'api-key': 'API Key',
            'auto-flush-on-insert': '插入后自动刷新',
            'avoid-dups': '避免重复',
            'collection-name': '集合名称',
            'consistency-level': '一致性级别',
            'database-name': '数据库名称',
            'dimension': '向量维度',
            'embedding-property': '向量属性名',
            'grpc-port': 'gRPC 端口',
            'host': '主机',
            'id-property': '主键属性名',
            'index-name': '索引名称',
            'index-type': '索引类型',
            'label': '标签',
            'metadata-keys': '元数据字段',
            'metadata-prefix': '元数据前缀',
            'metric-type': '相似度度量',
            'object-class': '对象类名',
            'password': '密码',
            'payload-text-key': '载荷文本字段',
            'port': '端口',
            'retrieve-embeddings-on-search': '检索时返回向量',
            'scheme': '协议',
            'secured-grpc': 'gRPC 加密',
            'server-url': '服务地址',
            'text-property': '文本属性名',
            'token': 'Token',
            'uri': '连接 URI',
            'use-grpc-for-inserts': '插入使用 gRPC',
            'use-tls': '使用 TLS',
            'user': '用户标识',
            'username': '用户名',
        }
    }
}
