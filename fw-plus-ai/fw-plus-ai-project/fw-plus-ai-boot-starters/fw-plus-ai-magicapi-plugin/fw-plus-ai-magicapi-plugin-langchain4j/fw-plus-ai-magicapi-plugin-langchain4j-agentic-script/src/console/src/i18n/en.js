export default {
    langchain4j: {
        form: {
            title: 'Agentic Script',
        },
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
            tool: 'Orchestration tool',
            graph: 'Graph orchestration'
        },
        description: 'Description',
        'description-hint': 'Purpose of this node/tool/graph',
        'input-keys': 'Input variables',
        'input-keys-hint': 'Variable names injected into the script context, comma separated',
        'output-key': 'Output key',
        'output-key-hint': 'Variable name the node output is written back to (node form, optional)',
        script: 'Script',
        'script-hint': 'magic-script body; input variables are injected into the context, return value is the output',
        params: 'Test parameters',
        'params-hint': 'JSON object, e.g. {"question":"test"}; injected into the script context for nodes, used as graph input for graphs',
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
