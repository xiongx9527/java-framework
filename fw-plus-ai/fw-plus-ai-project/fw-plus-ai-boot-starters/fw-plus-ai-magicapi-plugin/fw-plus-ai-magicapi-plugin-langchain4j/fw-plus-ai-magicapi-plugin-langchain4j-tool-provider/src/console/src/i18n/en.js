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
            requireOptIn: 'Require Opt-in'
        },
        option: {
            yes: 'Yes',
            no: 'No'
        },
        hint: {
            required: 'Required',
            optional: 'Optional',
            groupSelect: 'Check groups, empty means no filter',
            groupEmpty: 'No groups available'
        }
    }
}
