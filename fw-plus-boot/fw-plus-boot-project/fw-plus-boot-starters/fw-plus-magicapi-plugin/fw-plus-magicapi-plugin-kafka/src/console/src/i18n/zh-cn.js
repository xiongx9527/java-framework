export default {
    datasource: {
        extra: {
            producer: '生产',
            consumer: '消费'
        }
    },
    kafka: {
        subscribe: {
            title: 'Kafka 订阅信息',
            name: 'Kafka 订阅',
            form: {
                key: '数据源Key',
                topic: '主题',
                broadcast: '广播',
                ackMode: '确认方式',
                concurrency: '并发数',
                name: '名称',
                path: '路径',
                placeholder: {
                    key: '请输入数据源Key',
                    topic: '请输入订阅主题',
                    name: '请输入名称',
                    path: '请输入路径',
                    params: '请输入 Kafka 订阅消息',
                    other: '请输入Kafka 订阅其他JSON配置'
                }
            },
            navbars: {
                other: '其它',
                params: '订阅参数'
            }
        },
        datasource: {
            title: 'Kafka 数据源',
            name: 'Kafka 数据源',
            form: {
                name: '名称',
                key: 'Key',
                servers: '服务地址',
                username: '用户名',
                password: '密码',
                extra: '类型',
                group: '访问组',
                other: '其它',
                placeholder: {
                    name: '数据源名称，仅作显示用',
                    key: '数据源Key，后续代码中使用',
                    servers: '请输入 Kafka 服务器地址，如：192.168.2.5:9092,...',
                    username: '请输入 Kafka 访问用户名，可选',
                    password: '请输入 Kafka 访问密码，可选',
                    extra: '请选择 Kafka 访问类型',
                    group: '请输入 Kafka 访问组， 可选',
                    other: '请输入 Kafka 访问相关配置，如： {xxxx: "yyyy"}'
                }
            }
        },
        title: 'Kafka管理',
        loading: '加载中...',
        refresh: '刷新',
        selectStorage: '选择存储',
        loadStoragesFailed: '加载存储失败',
        loadTopicsFailed: '加载主题失败',
        loadTopicInfoFailed: '加载主题信息失败',
        loadConsumerGroupsFailed: '加载消费者组失败',
        noTopicInfo: '暂无主题信息',
        fullscreen: '全屏',
        exitFullscreen: '退出全屏',
        noPlugin: '未安装Kafka插件',
        noPluginSubtext: '请安装Kafka插件以使用Kafka管理功能',
        consumerGroupOffset: '消费者组偏移量',
        topic: '主题',
        consumerGroup: '消费者组',
        selectConsumerGroup: '选择消费者组',
        resetOffsets: '重置偏移量',
        resetOffsetsSuccess: '重置偏移量成功',
        resetOffsetsFailed: '重置偏移量失败',
        delete: '删除',
        deleteTopic: '删除主题',
        deleteTopicConfirm: '确定要删除主题 {0} 吗？',
        deleteTopicSuccess: '删除主题成功',
        deleteTopicFailed: '删除主题失败',
        deletingTopic: '正在删除主题...',
        cancel: '取消',
        confirm: '确认',
        close: '关闭',
        pleaseSelect: '请选择',
        pleaseSelectConsumerGroup: '请选择消费者组'
    }
}
