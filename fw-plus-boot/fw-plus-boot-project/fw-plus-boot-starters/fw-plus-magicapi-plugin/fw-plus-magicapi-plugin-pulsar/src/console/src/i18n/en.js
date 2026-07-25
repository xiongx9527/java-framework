export default {
    datasource: {
        extra: {
            producer: 'Producer',
            consumer: 'Consumer'
        }
    },
    pulsar: {
        title: 'Pulsar Management',
        loading: 'Loading...',
        refresh: 'Refresh',
        storage: 'Storage',
        selectStorage: 'Please select',
        selectTenant: 'Please select tenant',
        selectNamespace: 'Please select namespace',
        pleaseSelect: 'Please select',
        loadStoragesFailed: 'Failed to load storage list',
        loadTenantsFailed: 'Failed to load tenant list',
        loadNamespacesFailed: 'Failed to load namespace list',
        tenants: 'Tenant Management',
        tenant: 'Tenant',
        tenantName: 'Tenant Name',
        createTenant: 'Create Tenant',
        enterTenantName: 'Please enter tenant name',
        tenantNameRequired: 'Tenant name is required',
        viewTenant: 'View Tenant: {0}',
        deleteTenant: 'Delete Tenant',
        deleteTenantConfirm: 'Are you sure you want to delete tenant {0}?',
        namespaces: 'Namespace Management',
        namespace: 'Namespace',
        namespaceName: 'Namespace Name',
        createNamespace: 'Create Namespace',
        enterNamespaceName: 'Please enter namespace name',
        namespaceNameRequired: 'Namespace name is required',
        viewNamespace: 'View Namespace: {0}',
        deleteNamespace: 'Delete Namespace',
        deleteNamespaceConfirm: 'Are you sure you want to delete namespace {0}?',
        createTopic: 'Create Topic',
        topics: 'Topic List',
        topicName: 'Topic Name',
        view: 'View',
        delete: 'Delete',
        enterTopicName: 'Please enter topic name',
        topicNameRequired: 'Topic name is required',
        deleteTopic: 'Delete Topic',
        deleteTopicConfirm: 'Are you sure you want to delete topic {0}?',
        deleteTopicSuccess: 'Delete topic success',
        deleteTopicFailed: 'Failed to delete topic',
        deletingTopic: 'Deleting topic...',
        viewNamespaces: 'View Namespaces: {0}',
        loadTopicsFailed: 'Failed to load topic list',
        loadConsumerGroupsFailed: 'Failed to load consumer groups',
        topicDetails: 'Topic Details',
        loadTopicInfoFailed: 'Failed to load topic information',
        noTopicInfo: 'No topic information available',
        resetOffsets: 'Reset Offsets',
        resetOffsetsSuccess: 'Reset offsets success',
        resetOffsetsFailed: 'Failed to reset offsets',
        confirm: 'Confirm',
        pleaseSelectConsumerGroup: 'Please select consumer group',
        subscribe: {
            title: 'Pulsar Subscribe Info',
            name: 'Pulsar Subscribe',
            form: {
                key: 'DataSource',
                topic: 'Topic',
                broadcast: 'Broadcast',
                batchIndexAcknowledgmentEnabled: 'Batch Index Acknowledgment Enabled',
                name: 'Name',
                path: 'Path',
                placeholder: {
                    key: 'Please Enter DataSource Key',
                    topic: 'Please Enter Subscribe Topic',
                    group: 'Please Enter Subscribe Group',
                    name: 'Please Enter Name',
                    path: 'Please Enter Path',
                    other: 'Please Enter Pulsar Subscribe Other json Config',
                    params: 'Please Enter Pulsar Subscribe Params'
                }
            },
            navbars: {
                other: 'Other',
                params: 'Subscribe Params'
            }
        },
        datasource: {
            title: 'Pulsar DataSource',
            name: 'Pulsar',
            form: {
                name: 'Name',
                key: 'Key',
                servers: 'Brokers',
                serviceUrls: 'Admin',
                username: 'User Name',
                password: 'Password',
                tenant: 'Tenant',
                namespace: 'Namespace',
                other: 'Other',
                placeholder: {
                    name: 'DataSource Name, Only Display Use',
                    key: 'DataSource Key, Required',
                    servers: 'Please Enter Pulsar Server Addresses, eg: 127.0.0.1:6650,...',
                    serviceUrls: 'Please Enter Pulsar Service Http Url, eg: http://127.0.0.1:8080,...',
                    username: 'Please Enter Pulsar username, Optional',
                    password: 'Please Enter Pulsar password, Optional',
                    tenant: 'Please Enter Pulsar Tenant',
                    namespace: 'Please Enter Pulsar Namespace',
                    other: 'Please Enter Pulsar Other Config, eg: {xxxx: "yyyy"}'
                }
            }
        }
    }
}
