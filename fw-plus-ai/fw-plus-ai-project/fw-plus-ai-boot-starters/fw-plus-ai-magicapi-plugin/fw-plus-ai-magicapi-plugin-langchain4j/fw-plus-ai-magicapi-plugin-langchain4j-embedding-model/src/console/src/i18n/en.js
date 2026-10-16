export default {
    langchain4j: {
        type: {
            'embedding-model': 'Embedding Model'
        },
        common: {
            name: 'Name',
            key: 'Key',
            provider: 'Provider',
            enabled: 'Enabled',
            embeddingModelKey: 'Embedding Model',
            options: 'Provider Options'
        },
        option: {
            yes: 'Yes',
            no: 'No',
            true: 'Yes',
            false: 'No'
        },
        hint: {
            required: 'Required',
            optional: 'Optional',
            duration: 'e.g. 60s / 2m / PT1M, empty uses default',
            tags: 'Separate multiple values with commas',
            switch: 'Leave empty to skip',
            select: 'Free text allowed',
            empty: 'No extra options for this provider',
            embeddingModelKey: 'Pick or type an enabled embedding-model key'
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
            'qianfan': 'Qianfan',
            'qwen': 'Qwen',
            'voyage-ai': 'Voyage AI',
            'zhipu-ai': 'Zhipu AI',
        },
        field: {
            'access-token': 'Access Token',
            'api-key': 'API Key',
            'base-url': 'Base URL',
            'call-timeout': 'Call Timeout',
            'connect-timeout': 'Connect Timeout',
            'deployment-name': 'Deployment Name',
            'dimensions': 'Dimensions',
            'encoding-format': 'Encoding Format',
            'endpoint': 'Endpoint',
            'github-token': 'GitHub Token',
            'input-type': 'Input Type',
            'log-requests': 'Log Requests',
            'log-requests-and-responses': 'Log Requests And Responses',
            'log-responses': 'Log Responses',
            'max-retries': 'Max Retries',
            'max-segments-per-batch': 'Max Segments Per Batch',
            'model': 'Model Name',
            'model-id': 'Model ID',
            'model-name': 'Model Name',
            'organization-id': 'Organization ID',
            'read-timeout': 'Read Timeout',
            'secret-key': 'Secret Key',
            'timeout': 'Timeout',
            'truncation': 'Truncation',
            'user': 'User',
            'wait-for-model': 'Wait For Model',
            'write-timeout': 'Write Timeout',
        }
    }
}
