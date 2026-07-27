export default {
    datasource: {
        extra: {
            producer: 'Producer',
            consumer: 'Consumer'
        }
    },
    kafka: {
        subscribe: {
            title: 'Kafka Subscribe Info',
            name: 'Kafka Subscribe',
            form: {
                key: 'DataSource',
                topic: 'Topic',
                broadcast: 'Broadcast',
                ackMode: 'Ack Mode',
                concurrency: 'Concurrency',
                name: 'Name',
                path: 'Path',
                placeholder: {
                    key: 'Please Enter DataSource Key',
                    topic: 'Please Enter Subscribe Topic',
                    name: 'Please Enter Name',
                    path: 'Please Enter Path',
                    params: 'Please Enter Kafka Subscribe Params',
                    other: 'Please Enter Kafka Subscribe Other JSON Config'
                }
            },
            navbars: {
                other: 'Other',
                params: 'Subscribe Params'
            }
        },
        datasource: {
            title: 'Kafka DataSource',
            name: 'Kafka',
            form: {
                name: 'Name',
                key: 'Key',
                servers: 'Servers',
                username: 'User Name',
                password: 'Password',
                extra: 'Type',
                group: 'Group Id',
                properties: 'Properties',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    servers: 'Please Enter Kafka Server Addresses, eg: 192.168.2.5:9092,...',
                    username: 'Please Enter Kafka username, Optional',
                    password: 'Please Enter Kafka password, Optional',
                    extra: 'Please Enter Kafka Access Type',
                    properties: 'JSON object, for example: {"key": "value"}'
                }
            }
        },
        title: 'Kafka Management',
        loading: 'Loading...',
        refresh: 'Refresh',
        selectStorage: 'Select Storage',
        loadStoragesFailed: 'Failed to load storage',
        loadTopicsFailed: 'Failed to load topics',
        loadTopicInfoFailed: 'Failed to load topic information',
        loadConsumerGroupsFailed: 'Failed to load consumer groups',
        noTopicInfo: 'No topic information available',
        fullscreen: 'Fullscreen',
        exitFullscreen: 'Exit Fullscreen',
        noPlugin: 'Kafka plugin not installed',
        noPluginSubtext: 'Please install Kafka plugin to use Kafka management features',
        consumerGroupOffset: 'Consumer Group Offset',
        topic: 'Topic',
        consumerGroup: 'Consumer Group',
        selectConsumerGroup: 'Select Consumer Group',
        resetOffsets: 'Reset Offsets',
        resetOffsetsSuccess: 'Reset offsets success',
        resetOffsetsFailed: 'Failed to reset offsets',
        delete: 'Delete',
        deleteTopic: 'Delete Topic',
        deleteTopicConfirm: 'Are you sure you want to delete topic {0}?',
        deleteTopicSuccess: 'Delete topic success',
        deleteTopicFailed: 'Failed to delete topic',
        deletingTopic: 'Deleting topic...',
        cancel: 'Cancel',
        confirm: 'Confirm',
        close: 'Close',
        pleaseSelect: 'Please select',
        pleaseSelectConsumerGroup: 'Please select consumer group'
    }
}
