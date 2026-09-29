export default {
    langchain4j: {
        type: {
            'tokenizer': 'Tokenizer'
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
            'api-key': 'API Key',
            'model-name': 'Model Name',
        }
    }
}
