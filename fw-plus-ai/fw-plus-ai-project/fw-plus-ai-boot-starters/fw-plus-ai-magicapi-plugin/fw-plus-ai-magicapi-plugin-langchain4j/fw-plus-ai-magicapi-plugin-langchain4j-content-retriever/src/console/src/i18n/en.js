export default {
    langchain4j: {
        type: {
            'content-retriever': 'Content Retriever'
        },
        common: {
            name: 'Name',
            key: 'Key',
            provider: 'Provider',
            enabled: 'Enabled',
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
            reference: 'Pick or type an enabled datasource key',
            map: 'Key-value map, e.g. category=faq,lang=zh or {"category":"faq"}'
        },
        provider: {
            'aggregating': 'Aggregating',
            'azure-ai-search': 'Azure AI Search',
            'embedding-store': 'Embedding Store',
            'filtering': 'Filtering Decorator',
            'nebula': 'NebulaGraph',
            'neo4j': 'Neo4j Graph',
            'web-search': 'Web Search'
        },
        field: {
            'addresses': 'Graphd Addresses',
            'api-key': 'API Key',
            'chat-model-key': 'Chat Model',
            'content-retriever-key': 'Wrapped Retriever',
            'content-retriever-keys': 'Aggregated Retrievers',
            'create-or-update-index': 'Create Or Update Index',
            'deduplicate': 'Deduplicate By Text',
            'dimensions': 'Dimensions',
            'embedding-model-key': 'Embedding Model',
            'embedding-store-key': 'Embedding Store',
            'endpoint': 'Endpoint',
            'filters': 'Metadata Filters',
            'index-name': 'Index Name',
            'max-results': 'Max Results',
            'max-session-size': 'Max Session Size',
            'min-score': 'Min Score',
            'password': 'Password',
            'prompt-template': 'Prompt Template',
            'query-type': 'Query Type',
            'search-function': 'Search Function',
            'space-name': 'Space Name',
            'timeout': 'Timeout (ms)',
            'uri': 'URI',
            'username': 'Username'
        }
    }
}
