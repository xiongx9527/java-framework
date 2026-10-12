export default {
    langchain4j: {
        type: {
            skill: 'Agent 技能'
        },
        common: {
            name: '名称',
            key: 'Key',
            enabled: '启用'
        },
        'description-hint': '进入技能目录（初始上下文），LLM 据此决定是否激活',
        content: '指令正文',
        'content-hint': '激活后载入上下文的完整指令（SKILL.md 的 body）',
        resources: '参考文档',
        'resources-hint': 'JSON 数组 [{"path":"references/guide.md","content":"..."}]，激活后经 read_skill_resource 按需读取',
        hint: {
            required: '必填',
            optional: '选填'
        },
        option: {
            yes: '是',
            no: '否'
        }
    }
}
