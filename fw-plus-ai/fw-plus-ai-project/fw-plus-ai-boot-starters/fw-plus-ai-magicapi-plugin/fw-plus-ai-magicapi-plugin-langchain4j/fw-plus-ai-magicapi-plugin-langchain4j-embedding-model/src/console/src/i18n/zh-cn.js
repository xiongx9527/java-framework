export default {
    langchain4j: {
        type: {
            'embedding-model': '向量模型'
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
            'cohere': 'Cohere',
            'github-models': 'GitHub Models',
            'google-ai-gemini': 'Google AI Gemini',
            'hugging-face': 'Hugging Face',
            'jina': 'Jina AI',
            'local-ai': 'LocalAI',
            'ollama': 'Ollama',
            'openai': 'OpenAI',
            'qianfan': '百度千帆',
            'qwen': '通义千问',
            'voyage-ai': 'Voyage AI',
            'zhipu-ai': '智谱 AI',
        },
        field: {
            'access-token': '访问令牌',
            'api-key': 'API Key',
            'base-url': '服务地址',
            'call-timeout': '调用超时',
            'connect-timeout': '连接超时',
            'deployment-name': '部署名称',
            'dimensions': '向量维度',
            'encoding-format': '编码格式',
            'endpoint': '接入点',
            'github-token': 'GitHub Token',
            'input-type': '输入类型',
            'log-requests': '记录请求',
            'log-requests-and-responses': '记录请求与响应',
            'log-responses': '记录响应',
            'max-retries': '最大重试次数',
            'max-segments-per-batch': '单批最大分段数',
            'model': '模型名称',
            'model-id': '模型 ID',
            'model-name': '模型名称',
            'organization-id': '组织 ID',
            'read-timeout': '读取超时',
            'secret-key': 'Secret Key',
            'timeout': '超时',
            'truncation': '截断',
            'user': '用户标识',
            'wait-for-model': '等待模型就绪',
            'write-timeout': '写入超时',
        }
    }
}
