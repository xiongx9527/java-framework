export default {
    langchain4j: {
        form: {
            title: 'Agentic 脚本',
        },
        type: {
            'agentic-script': 'Agentic 脚本'
        },
        common: {
            name: '名称',
            key: 'Key',
            enabled: '启用'
        },
        'script-type': {
            label: '形态',
            node: '编排节点',
            llm: 'LLM 节点',
            tool: '编排工具',
            graph: '图编排'
        },
        description: '描述',
        'description-hint': '该节点/工具/图的用途说明',
        'input-keys': '输入变量',
        'input-keys-hint': '注入脚本上下文/模板的变量名，英文逗号分隔',
        'output-key': '输出 key',
        'output-key-hint': '节点输出写回的变量名（编排/LLM 节点形态，选填）',
        'model-key': '模型 key',
        'model-key-hint': 'langchain4jChatModel 资源 key，如 deepseek；解析失败回落容器模型 bean',
        'system-message': '系统提示',
        'system-message-hint': 'LLM 节点的系统提示（人设/规则），选填',
        script: '脚本',
        'script-hint': 'magic-script 正文；输入变量注入上下文，返回值即输出',
        'llm-script-hint': 'user message 模板；{{var}} 按输入变量插值',
        params: '测试参数',
        'params-hint': 'JSON 对象，如 {"question":"测试问题"}；节点形态注入脚本上下文，图形态作为图的执行输入',
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
