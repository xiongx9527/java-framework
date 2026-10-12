export default {
    langchain4j: {
        type: {
            'language-model': 'Language Model'
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
            'openai': 'OpenAI',
            'qwen': 'Qwen',
        },
        field: {
            'access-token': 'Access Token',
            'api-key': 'API Key',
            'base-url': 'Base URL',
            'deployment-name': 'Deployment Name',
            'enable-search': 'Enable Web Search',
            'endpoint': 'Endpoint',
            'format': 'Output Format',
            'log-requests': 'Log Requests',
            'log-requests-and-responses': 'Log Requests And Responses',
            'log-responses': 'Log Responses',
            'max-new-tokens': 'Max New Tokens',
            'max-retries': 'Max Retries',
            'max-tokens': 'Max Tokens',
            'model-id': 'Model ID',
            'model-name': 'Model Name',
            'num-ctx': 'Context Window',
            'num-predict': 'Predict Tokens',
            'organization-id': 'Organization ID',
            'penalty-score': 'Penalty Score',
            'repeat-penalty': 'Repeat Penalty',
            'repetition-penalty': 'Repetition Penalty',
            'return-full-text': 'Return Full Text',
            'secret-key': 'Secret Key',
            'seed': 'Seed',
            'stop': 'Stop',
            'stops': 'Stop',
            'temperature': 'Temperature',
            'timeout': 'Timeout',
            'top-k': 'Top K',
            'top-p': 'Top P',
            'wait-for-model': 'Wait For Model',
        }
    }
}
