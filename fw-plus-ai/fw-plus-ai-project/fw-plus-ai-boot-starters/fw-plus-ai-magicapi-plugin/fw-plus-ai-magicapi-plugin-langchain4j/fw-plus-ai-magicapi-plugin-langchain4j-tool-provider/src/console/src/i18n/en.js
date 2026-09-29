export default {
    langchain4j: {
        type: {
            'tool-provider': 'ToolProvider Tools'
        },
        common: {
            name: 'Name',
            key: 'Key',
            enabled: 'Enabled',
            functions: 'Functions Scope',
            api: 'API Scope'
        },
        scope: {
            enabled: 'Enabled',
            includeGroups: 'Include Groups',
            excludeGroups: 'Exclude Groups',
            includePaths: 'Include Paths',
            excludePaths: 'Exclude Paths',
            methods: 'HTTP Methods',
            requireOptIn: 'Require Opt-in'
        },
        option: {
            yes: 'Yes',
            no: 'No'
        },
        hint: {
            required: 'Required',
            optional: 'Optional',
            list: 'Comma-separated, supports * wildcard, empty means no limit',
            methods: 'e.g. GET,POST, empty means no limit'
        }
    }
}
