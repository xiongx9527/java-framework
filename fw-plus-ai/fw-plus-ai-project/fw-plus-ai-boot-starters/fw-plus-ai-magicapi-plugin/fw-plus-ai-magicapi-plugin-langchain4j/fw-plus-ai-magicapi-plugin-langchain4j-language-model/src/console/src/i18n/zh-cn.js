export default {
    langchain4j: {
        type: {
            'language-model': '语言模型'
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
            'hugging-face': 'Hugging Face',
            'local-ai': 'LocalAI',
            'ollama': 'Ollama',
            'openai': 'OpenAI',
            'qianfan': '百度千帆',
            'qwen': '通义千问',
        },
        field: {
            'access-token': '访问令牌',
            'api-key': 'API Key',
            'base-url': '服务地址',
            'deployment-name': '部署名称',
            'enable-search': '启用联网搜索',
            'endpoint': '接入点',
            'format': '输出格式',
            'log-requests': '记录请求',
            'log-requests-and-responses': '记录请求与响应',
            'log-responses': '记录响应',
            'max-new-tokens': '最大新 Token 数',
            'max-retries': '最大重试次数',
            'max-tokens': '最大 Token 数',
            'model-id': '模型 ID',
            'model-name': '模型名称',
            'num-ctx': '上下文窗口',
            'num-predict': '预测 Token 数',
            'organization-id': '组织 ID',
            'penalty-score': '惩罚分',
            'repeat-penalty': '重复惩罚',
            'repetition-penalty': '重复惩罚',
            'return-full-text': '返回完整文本',
            'secret-key': 'Secret Key',
            'seed': '随机种子',
            'stop': '停止词',
            'stops': '停止词',
            'temperature': '温度',
            'timeout': '超时',
            'top-k': 'Top K',
            'top-p': 'Top P',
            'wait-for-model': '等待模型就绪',
        }
    }
}
