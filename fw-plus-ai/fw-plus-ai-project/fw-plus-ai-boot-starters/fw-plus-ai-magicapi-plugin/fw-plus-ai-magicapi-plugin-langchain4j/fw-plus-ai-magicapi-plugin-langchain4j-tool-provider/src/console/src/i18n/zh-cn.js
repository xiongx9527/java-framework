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
            excludeGroups: '排除分组',
            includePaths: '包含路径',
            excludePaths: '排除路径',
            methods: 'HTTP 方法',
            requireOptIn: '接口需显式开启'
        },
        option: {
            yes: '是',
            no: '否'
        },
        hint: {
            required: '必填',
            optional: '选填',
            list: '多个值用英文逗号分隔，支持 * 通配，留空不限',
            methods: '如 GET,POST，留空不限'
        }
    }
}
