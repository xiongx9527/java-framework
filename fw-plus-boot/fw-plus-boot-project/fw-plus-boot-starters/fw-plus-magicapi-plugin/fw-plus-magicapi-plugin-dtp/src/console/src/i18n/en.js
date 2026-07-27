export default {
    dtp: {
        datasource: {
            title: 'Dtp DataSource',
            name: 'Dtp',
            form: {
                name: 'Name',
                key: 'Key',
                corePoolSize: 'CoreSize',
                maximumPoolSize: 'MaxSize',
                keepAliveTime: 'KeepAlive',
                queueType: 'QueueType',
                queueCapacity: 'Capacity',
                threadFactory: 'Factory',
                rejectedPolicy: 'Rejected',
                properties: 'Properties',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    corePoolSize: 'Core Pool Size, Default 1',
                    maximumPoolSize: 'Maximum Pool Size, Default 10',
                    queueType: 'Queue Type, Default LinkedBlockingQueue',
                    queueCapacity: 'Queue Capacity, Default 1000',
                    threadFactory: 'Thread Factory, Default DefaultThreadFactory',
                    rejectedPolicy: 'Rejected Policy, Default CallerRunsPolicy',
                    properties: 'JSON object, for example: {"key": "value"}'
                }
            }
        }
    }
}
