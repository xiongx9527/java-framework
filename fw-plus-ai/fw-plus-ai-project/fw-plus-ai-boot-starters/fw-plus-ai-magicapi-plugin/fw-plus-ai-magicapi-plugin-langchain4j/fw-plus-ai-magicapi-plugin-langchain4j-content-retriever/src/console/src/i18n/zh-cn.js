export default {
    langchain4j: {
        type: {
            'content-retriever': '内容检索器'
        },
        common: {
            name: '名称',
            key: 'Key',
            provider: 'Provider',
            enabled: '启用',
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
            reference: '选择或输入已启用的同类数据源 key',
            map: '键值表，如 category=faq,lang=zh 或 {"category":"faq"}'
        },
        provider: {
            'aggregating': '组合检索',
            'azure-ai-search': 'Azure AI Search',
            'embedding-store': '向量库检索',
            'filtering': '过滤装饰器',
            'nebula': 'NebulaGraph 图库',
            'neo4j': 'Neo4j 图库',
            'web-search': '联网搜索'
        },
        field: {
            'addresses': 'graphd 地址',
            'api-key': 'API Key',
            'chat-model-key': '关联对话模型',
            'content-retriever-key': '被包装的检索器',
            'content-retriever-keys': '被组合的检索器',
            'create-or-update-index': '自动创建索引',
            'deduplicate': '按正文去重',
            'dimensions': '向量维度',
            'embedding-model-key': '关联向量模型',
            'embedding-store-key': '关联向量库',
            'endpoint': '服务地址',
            'filters': '元数据过滤',
            'index-name': '索引名称',
            'max-results': '最大条数',
            'max-session-size': '最大会话数',
            'min-score': '最低分数',
            'password': '密码',
            'prompt-template': '提示词模板',
            'query-type': '查询类型',
            'search-function': '搜索函数',
            'space-name': '图空间',
            'timeout': '超时（毫秒）',
            'uri': '连接 URI',
            'username': '用户名'
        }
    }
}
