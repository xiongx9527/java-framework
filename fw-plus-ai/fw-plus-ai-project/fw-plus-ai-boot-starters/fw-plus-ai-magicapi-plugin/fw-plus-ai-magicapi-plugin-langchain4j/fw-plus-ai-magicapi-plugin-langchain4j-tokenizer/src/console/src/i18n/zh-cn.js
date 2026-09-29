export default {
    langchain4j: {
        type: {
            'tokenizer': '分词器'
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
            'azure-openai': 'Azure OpenAI',
            'openai': 'OpenAI',
            'qwen': '通义千问',
        },
        field: {
            'api-key': 'API Key',
            'model-name': '模型名称',
        }
    }
}
