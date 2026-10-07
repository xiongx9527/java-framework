export default {
    langchain4j: {
        type: {
            'tool-provider': 'ToolProvider 工具集'
        },
        common: {
            name: '名称',
            key: 'Key',
            enabled: '启用',
            functions: '函数范围',
            api: '接口范围'
        },
        scope: {
            enabled: '启用',
            includeGroups: '包含分组',
            requireOptIn: '接口需显式开启'
        },
        option: {
            yes: '是',
            no: '否'
        },
        hint: {
            required: '必填',
            optional: '选填',
            groupSelect: '勾选分组，留空不过滤',
            groupEmpty: '暂无可用分组'
        }
    }
}
