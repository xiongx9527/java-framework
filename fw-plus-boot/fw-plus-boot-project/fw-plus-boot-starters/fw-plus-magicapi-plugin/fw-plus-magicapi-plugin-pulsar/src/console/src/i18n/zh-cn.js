export default {
    datasource: {
        extra: {
            producer: '生产',
            consumer: '消费'
        }
    },
    pulsar: {
        title: 'Pulsar管理',
        loading: '加载中...',
        refresh: '刷新',
        storage: '存储',
        selectStorage: '请选择',
        selectTenant: '请选择租户',
        selectNamespace: '请选择命名空间',
        pleaseSelect: '请选择',
        loadStoragesFailed: '加载存储列表失败',
        loadTenantsFailed: '加载租户列表失败',
        loadNamespacesFailed: '加载命名空间列表失败',
        tenants: '租户管理',
        tenant: '租户',
        tenantName: '租户名称',
        createTenant: '创建租户',
        enterTenantName: '请输入租户名称',
        tenantNameRequired: '租户名称不能为空',
        viewTenant: '查看租户：{0}',
        deleteTenant: '删除租户',
        deleteTenantConfirm: '确定要删除租户 {0} 吗？',
        namespaces: '命名空间管理',
        namespace: '命名空间',
        namespaceName: '命名空间名称',
        createNamespace: '创建命名空间',
        enterNamespaceName: '请输入命名空间名称',
        namespaceNameRequired: '命名空间名称不能为空',
        viewNamespace: '查看命名空间：{0}',
        deleteNamespace: '删除命名空间',
        deleteNamespaceConfirm: '确定要删除命名空间 {0} 吗？',
        createTopic: '创建主题',
        topics: '主题列表',
        topicName: '主题名称',
        view: '查看',
        delete: '删除',
        enterTopicName: '请输入主题名称',
        topicNameRequired: '主题名称不能为空',
        deleteTopic: '删除主题',
        deleteTopicConfirm: '确定要删除主题 {0} 吗？',
        deleteTopicSuccess: '删除主题成功',
        deleteTopicFailed: '删除主题失败',
        deletingTopic: '正在删除主题...',
        viewNamespaces: '查看命名空间：{0}',
        loadTopicsFailed: '加载主题列表失败',
        loadConsumerGroupsFailed: '加载消费者组失败',
        topicDetails: '主题详情',
        loadTopicInfoFailed: '加载主题信息失败',
        noTopicInfo: '暂无主题信息',
        resetOffsets: '重置偏移量',
        resetOffsetsSuccess: '重置偏移量成功',
        resetOffsetsFailed: '重置偏移量失败',
        confirm: '确认',
        pleaseSelectConsumerGroup: '请选择消费者组',
        subscribe: {
            title: 'Pulsar 订阅信息',
            name: 'Pulsar 订阅',
            form: {
                key: '数据源Key',
                topic: '主题',
                broadcast: '广播',
                batchIndexAcknowledgmentEnabled: '批量确认',
                name: '名称',
                path: '路径',
                placeholder: {
                    key: '请输入数据源Key',
                    topic: '请输入订阅主题',
                    group: '请输入订阅分组',
                    name: '请输入名称',
                    path: '请输入路径',
                    other: '请输入Pulsar其它json配置',
                    params: '请输入Pulsar订阅消息'
                }
            },
            navbars: {
                other: '其它',
                params: '订阅参数'
            }
        },
        datasource: {
            title: 'Pulsar 数据源',
            name: 'Pulsar 数据源',
            form: {
                name: '名称',
                key: 'Key',
                servers: 'broker地址',
                serviceUrls: 'admin地址',
                username: '用户名',
                password: '密码',
                tenant: '租户',
                namespace: '命名空间',
                properties: '连接参数',
                placeholder: {
                    name: '数据源名称，仅作显示用',
                    key: '数据源Key，后续代码中使用',
                    servers: '请输入 Pulsar 服务器地址，如：127.0.0.1:6650,...',
                    serviceUrls: '请输入 Pulsar 服务器地址，如：http://127.0.0.1:8080,...',
                    username: '请输入 Pulsar 访问用户名，可选',
                    password: '请输入 Pulsar 访问密码，可选',
                    tenant: '请选择 Pulsar 租户',
                    namespace: '请输入 Pulsar 命名空间',
                    properties: '请输入 JSON 对象，如： {"key": "value"}'
                }
            }
        }
    }
}
