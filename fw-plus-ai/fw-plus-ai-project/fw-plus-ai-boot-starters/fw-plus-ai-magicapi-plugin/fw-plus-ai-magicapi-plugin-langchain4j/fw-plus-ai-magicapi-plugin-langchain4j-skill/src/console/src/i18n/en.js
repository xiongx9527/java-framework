export default {
    langchain4j: {
        type: {
            skill: 'Agent Skill'
        },
        common: {
            name: 'Name',
            key: 'Key',
            enabled: 'Enabled'
        },
        'description-hint': 'Shown in the skill catalogue (initial context), LLM decides whether to activate',
        content: 'Instructions',
        'content-hint': 'Full instructions loaded when the skill is activated (SKILL.md body)',
        resources: 'Resources',
        'resources-hint': 'JSON array of {path, content}, read on demand via read_skill_resource',
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
