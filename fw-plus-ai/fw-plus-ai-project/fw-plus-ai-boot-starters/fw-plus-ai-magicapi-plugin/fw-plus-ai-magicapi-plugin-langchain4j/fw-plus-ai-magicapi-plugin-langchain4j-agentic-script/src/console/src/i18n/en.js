export default {
    langchain4j: {
        type: {
            'agentic-script': 'Agentic Script'
        },
        common: {
            name: 'Name',
            key: 'Key',
            enabled: 'Enabled'
        },
        'script-type': {
            label: 'Type',
            node: 'Orchestration node',
            tool: 'Orchestration tool'
        },
        description: 'Description',
        'description-hint': 'Purpose of this node/tool',
        'input-keys': 'Input variables',
        'input-keys-hint': 'Variable names injected into the script context, comma separated',
        'output-key': 'Output key',
        'output-key-hint': 'Variable name the node output is written back to (node form, optional)',
        script: 'Script',
        'script-hint': 'magic-script body; input variables are injected into the context, return value is the output',
        hint: {
            required: 'required',
            optional: 'optional'
        },
        option: {
            yes: 'Yes',
            no: 'No'
        }
    }
}
