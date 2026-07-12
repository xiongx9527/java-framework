export default {
    dtp: {
        datasource: {
            title: 'Dtp 数据源',
            name: 'Dtp 数据源',
            form: {
                name: '名称',
                key: 'Key',
                corePoolSize: '核心线程数',
                maximumPoolSize: '最大线程数',
                keepAliveTime: '空闲时间',
                queueType: '队列类型',
                queueCapacity: '队列容量',
                threadFactory: '线程工厂',
                rejectedPolicy: '拒绝策略',
                other: '其它',
                placeholder: {
                    name: '名称，仅显示使用',
                    key: '数据源Key，必填',
                    corePoolSize: '核心线程数，默认1',
                    maximumPoolSize: '最大线程数，默认10',
                    queueType: '队列类型，默认LinkedBlockingQueue',
                    queueCapacity: '队列容量，默认1000',
                    threadFactory: '线程工厂，默认DefaultThreadFactory',
                    rejectedPolicy: '拒绝策略，默认CallerRunsPolicy',
                    other: ''
                }
            }
        }
    }
}
