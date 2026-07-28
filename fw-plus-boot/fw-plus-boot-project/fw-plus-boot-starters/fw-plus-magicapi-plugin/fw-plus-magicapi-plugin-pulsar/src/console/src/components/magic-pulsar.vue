<template>
  <div ref="containerRef" class="magic-pulsar">
    <div v-if="!loading && !hasPulsarPlugin" class="magic-pulsar-no-plugin">
      <magic-icon icon="#magic-pulsar-pulsar" size="48px" />
      <p>{{ $i('pulsar.noPlugin') }}</p>
      <p class="magic-pulsar-no-plugin-subtext">{{ $i('pulsar.noPluginSubtext') }}</p>
    </div>
    <div v-else class="magic-pulsar-wrapper">
      <div class="magic-pulsar-header">
      <div class="magic-pulsar-header-actions">
        <div class="magic-pulsar-header-left">
        </div>
        <div class="magic-pulsar-header-storage">
          <div class="magic-pulsar-storage-selector">
            <button class="magic-pulsar-button" @click="toggleStorageMenu" :disabled="loading">
              <magic-icon icon="#magic-pulsar-pulsar" size="16px" />
              <span class="magic-pulsar-button-text">{{ loading ? $i('pulsar.loading') : (activeStorage ? activeStorage.name : $i('pulsar.selectStorage')) }}</span>
              <magic-icon icon="arrow-bottom" size="12px" />
            </button>
            <div v-if="showStorageMenu && storages && storages.length > 0 && !loading" class="magic-pulsar-dropdown">
              <div 
                v-for="storage in storageOptionsWithDefault" 
                :key="storage.value"
                class="magic-pulsar-dropdown-item"
                :class="{ active: activeStorage && storage.value === activeStorage.key }"
                @click="changeStorage(storage)"
              >
                {{ storage.label }}
              </div>
            </div>
          </div>
          <div class="magic-pulsar-tenant-selector">
            <button class="magic-pulsar-button" @click="toggleTenantMenu" :disabled="loading || !activeStorage">
              <magic-icon icon="view" size="16px" />
              <span class="magic-pulsar-button-text">{{ loading ? $i('pulsar.loading') : (selectedTenant ? selectedTenant : $i('pulsar.selectTenant')) }}</span>
              <magic-icon icon="arrow-bottom" size="12px" />
            </button>
            <div v-if="showTenantMenu && !loading" class="magic-pulsar-dropdown">
              <div 
                v-for="tenant in tenantOptionsWithDefault" 
                :key="tenant.value"
                class="magic-pulsar-dropdown-item"
                :class="{ active: selectedTenant === tenant.value }"
                @click="changeTenant(tenant)"
              >
                {{ tenant.label }}
              </div>
            </div>
          </div>
          <div class="magic-pulsar-namespace-selector">
            <button class="magic-pulsar-button" @click="toggleNamespaceMenu" :disabled="loading || !selectedTenant">
              <magic-icon icon="view" size="16px" />
              <span class="magic-pulsar-button-text">{{ loading ? $i('pulsar.loading') : (selectedNamespace ? selectedNamespace : $i('pulsar.selectNamespace')) }}</span>
              <magic-icon icon="arrow-bottom" size="12px" />
            </button>
            <div v-if="showNamespaceMenu && !loading" class="magic-pulsar-dropdown">
              <div 
                v-for="namespace in namespaceOptionsWithDefault" 
                :key="namespace.value"
                class="magic-pulsar-dropdown-item"
                :class="{ active: selectedNamespace === namespace.value }"
                @click="changeNamespace(namespace)"
              >
                {{ namespace.label }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="magic-pulsar-content">
      <div v-if="loading" class="magic-pulsar-loading">
        <span>{{ $i('pulsar.loading') }}</span>
      </div>
      <div v-else class="magic-pulsar-content-wrapper" :class="{ 'with-sidebar': showTopicSidebar }">
        <div class="magic-pulsar-main">
          <!-- Topics List -->
          <div class="magic-pulsar-topics">
            <div v-if="topicsLoading" class="magic-pulsar-loading">
              <span>{{ $i('pulsar.loading') }}</span>
            </div>
            <div v-else class="magic-pulsar-table-container">
              <magic-table :data="topics" border>
                <magic-table-column :title="''" #default="{ row }">
                  <span class="magic-pulsar-tenant-name-text" @click="viewTopicDetails(row)">{{ row.name }}</span>
                </magic-table-column>
              </magic-table>
            </div>
          </div>

          <!-- Topic Details Panel -->
          <div v-if="showTopicSidebar" class="magic-pulsar-details-panel">
            <div class="magic-pulsar-details-header">
              <h3>{{ selectedTopic?.name }}</h3>
              <div class="magic-pulsar-details-actions">
                <button class="magic-pulsar-details-action-button" @click="refreshTopicDetails" :title="$i('pulsar.refresh')">
                  <magic-icon icon="refresh" size="16px" />
                </button>
                <button class="magic-pulsar-details-action-button" @click="resetConsumerGroupOffsets" :title="$i('pulsar.resetOffsets')">
                  <magic-icon icon="update" size="16px" />
                </button>
                <button class="magic-pulsar-details-action-button" @click="deleteTopic(selectedTopic)" :title="$i('pulsar.delete')">
                  <magic-icon icon="delete" size="16px" />
                </button>
                <button class="magic-pulsar-details-close" @click="showTopicSidebar = false">
                  <magic-icon icon="close" size="16px" />
                </button>
              </div>
            </div>
            <div class="magic-pulsar-details-content">
              <div v-if="topicLoading" class="magic-pulsar-loading">
                <span>{{ $i('pulsar.loading') }}</span>
              </div>
              <div v-else>
                <div v-if="topicDetails">
                  <magic-monaco-editor :value="formatJson(topicDetails)" language="json" :readonly="true" />
                </div>
                <div v-else class="magic-pulsar-no-data">
                  {{ $i('pulsar.noTopicInfo') }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    </div>
    
    <!-- Create Tenant Dialog -->
    <div v-if="showCreateTenantDialog" class="magic-pulsar-dialog-overlay" @click="showCreateTenantDialog = false">
      <div class="magic-pulsar-dialog" @click.stop>
        <div class="magic-pulsar-dialog-header">
          <h3>{{ $i('pulsar.createTenant') }}</h3>
          <button class="magic-pulsar-dialog-close" @click="showCreateTenantDialog = false">
            <magic-icon icon="close" size="16px" />
          </button>
        </div>
        <div class="magic-pulsar-dialog-body">
          <div class="magic-pulsar-form-item">
            <label>{{ $i('pulsar.tenantName') }}:</label>
            <magic-input v-model:value="newTenantName" :placeholder="$i('pulsar.enterTenantName')" />
          </div>
        </div>
        <div class="magic-pulsar-dialog-footer">
          <button class="magic-pulsar-dialog-button" @click="showCreateTenantDialog = false">
            {{ $i('message.cancel') }}
          </button>
          <button class="magic-pulsar-dialog-button primary" @click="submitCreateTenant">
            {{ $i('message.confirm') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Create Namespace Dialog -->
    <div v-if="showCreateNamespaceDialog" class="magic-pulsar-dialog-overlay" @click="showCreateNamespaceDialog = false">
      <div class="magic-pulsar-dialog" @click.stop>
        <div class="magic-pulsar-dialog-header">
          <h3>{{ $i('pulsar.createNamespace') }}</h3>
          <button class="magic-pulsar-dialog-close" @click="showCreateNamespaceDialog = false">
            <magic-icon icon="close" size="16px" />
          </button>
        </div>
        <div class="magic-pulsar-dialog-body">
          <div class="magic-pulsar-form-item">
            <label>{{ $i('pulsar.tenant') }}:</label>
            <magic-select v-model:value="newNamespaceTenant" :options="tenantOptions" />
          </div>
          <div class="magic-pulsar-form-item">
            <label>{{ $i('pulsar.namespaceName') }}:</label>
            <magic-input v-model:value="newNamespaceName" :placeholder="$i('pulsar.enterNamespaceName')" />
          </div>
        </div>
        <div class="magic-pulsar-dialog-footer">
          <button class="magic-pulsar-dialog-button" @click="showCreateNamespaceDialog = false">
            {{ $i('message.cancel') }}
          </button>
          <button class="magic-pulsar-dialog-button primary" @click="submitCreateNamespace">
            {{ $i('message.confirm') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Create Topic Dialog -->
    <div v-if="showCreateDialog" class="magic-pulsar-dialog-overlay" @click="showCreateDialog = false">
      <div class="magic-pulsar-dialog" @click.stop>
        <div class="magic-pulsar-dialog-header">
          <h3>{{ $i('pulsar.createTopic') }}</h3>
          <button class="magic-pulsar-dialog-close" @click="showCreateDialog = false">
            <magic-icon icon="close" size="16px" />
          </button>
        </div>
        <div class="magic-pulsar-dialog-body">
          <div class="magic-pulsar-form-item">
            <label>{{ $i('pulsar.namespace') }}:</label>
            <magic-select v-model:value="newTopicNamespace" :options="namespaceOptions" />
          </div>
          <div class="magic-pulsar-form-item">
            <label>{{ $i('pulsar.topicName') }}:</label>
            <magic-input v-model:value="newTopicName" :placeholder="$i('pulsar.enterTopicName')" />
          </div>
        </div>
        <div class="magic-pulsar-dialog-footer">
          <button class="magic-pulsar-dialog-button" @click="showCreateDialog = false">
            {{ $i('message.cancel') }}
          </button>
          <button class="magic-pulsar-dialog-button primary" @click="submitCreateTopic">
            {{ $i('message.confirm') }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- Consumer Group Selection Modal -->
  <div v-if="showConsumerGroupModal" class="magic-pulsar-modal-overlay" @click="showConsumerGroupModal = false">
    <div class="magic-pulsar-modal" @click.stop>
      <div class="magic-pulsar-modal-header">
        <h3>{{ $i('pulsar.resetOffsets') }}</h3>
        <button class="magic-pulsar-modal-close" @click="showConsumerGroupModal = false">
          <magic-icon icon="close" size="16px" />
        </button>
      </div>
      <div class="magic-pulsar-modal-body">
        <div v-if="loadingConsumerGroups" class="magic-pulsar-loading">
          <span>{{ $i('pulsar.loading') }}</span>
        </div>
        <div v-else>
          <div class="magic-pulsar-form-group">
              <select v-model="selectedConsumerGroup" class="magic-pulsar-select">
                <option value="">{{ $i('pulsar.pleaseSelectConsumerGroup') }}</option>
                <option v-for="group in consumerGroups" :key="group" :value="group">
                  {{ group }}
                </option>
              </select>
            </div>
        </div>
      </div>
      <div class="magic-pulsar-modal-footer">
        <button class="magic-pulsar-button magic-pulsar-button-primary" @click="confirmResetOffsets" :disabled="!selectedConsumerGroup" :title="$i('pulsar.confirm')">
          <magic-icon icon="save" size="16px" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getPulsarRuntime } from '../service/magic-pulsar-runtime.js'

const { request, modal, bus, Message, format: $i } = getPulsarRuntime()

// Global debug log
console.log('MagicPulsar component loaded')

// State
const loading = ref(false)
const hasPulsarPlugin = ref(true)
const activeTab = ref('tenants')
const storages = ref([])
const selectedStorage = ref('')
const activeStorage = ref(null)
const showStorageMenu = ref(false)
const selectedTenant = ref('')
const showTenantMenu = ref(false)
const selectedNamespace = ref('')
const showNamespaceMenu = ref(false)
const tenants = ref([
  { name: 'camel-poc' },
  { name: 'cura_dev' },
  { name: 'dynamic-approval-dev' },
  { name: 'dynamic-approval-dev2' },
  { name: 'em-dev' },
  { name: 'em-dev2' },
  { name: 'em-dev3' },
  { name: 'em-fat' },
  { name: 'em-test' },
  { name: 'jbp_dev' }
])
const namespaces = ref([])
const topics = ref([])
const containerRef = ref(null)
const showTopicSidebar = ref(false)
const selectedTopic = ref(null)
const topicDetails = ref(null)
const topicLoading = ref(false)

// Consumer group reset variables
const showConsumerGroupModal = ref(false)
const consumerGroups = ref([])
const selectedConsumerGroup = ref('')
const loadingConsumerGroups = ref(false)
const topicsLoading = ref(false)

// Set mock data immediately
console.log('Setting mock data immediately')
console.log('Tenants:', tenants.value)
console.log('Tenants length:', tenants.value.length)

// Dialog states
const showCreateTenantDialog = ref(false)
const showCreateNamespaceDialog = ref(false)
const showCreateDialog = ref(false)

// Form data
const newTenantName = ref('')
const newNamespaceTenant = ref('')
const newNamespaceName = ref('')
const newTopicNamespace = ref('')
const newTopicName = ref('')
const newTopicPartitions = ref(1)

// Tabs configuration
const tabs = [
  { key: 'tenants', label: $i('pulsar.tenants') },
  { key: 'namespaces', label: $i('pulsar.namespaces') },
  { key: 'topics', label: $i('pulsar.topics') }
]

// Computed options for select components
const storageOptions = computed(() => {
  console.log('Storages value:', storages.value)
  if (!storages.value) return []
  return storages.value.map(storage => ({
    label: storage.name || storage.key || 'Unknown',
    value: storage.key || storage.name || ''
  }))
})

const storageOptionsWithDefault = computed(() => {
  return [{ label: $i('pulsar.selectStorage'), value: '' }, ...storageOptions.value]
})

const tenantOptions = computed(() => {
  if (!tenants.value) return []
  return tenants.value.map(tenant => ({
    label: tenant.name,
    value: tenant.name
  }))
})

const tenantOptionsWithDefault = computed(() => {
  return [{ label: $i('pulsar.selectTenant'), value: '' }, ...tenantOptions.value]
})

const namespaceOptions = computed(() => {
  if (!namespaces.value) return []
  return namespaces.value.map(namespace => ({
    label: namespace.name,
    value: namespace.name
  }))
})

const namespaceOptionsWithDefault = computed(() => {
  return [{ label: $i('pulsar.selectNamespace'), value: '' }, ...namespaceOptions.value]
})

// Methods
const loadStorages = () => {
  // 检查是否有pulsar插件
  // 我们通过尝试调用API来判断是否有pulsar插件
  loading.value = true
  console.log('Starting to load storages')
  
  // Use real API call
  request.sendGet('/pulsar/availableStorages')
    .success(data => {
      // Handle different response formats
      console.log('Storages API response data:', data)
      storages.value = data || []
      console.log('Storages value after assignment:', storages.value)
      console.log('Storages length:', storages.value.length)
      hasPulsarPlugin.value = true
      
      // Do not auto select first storage
      // Wait for user to select
    })
    .error((error) => {
      console.error('Error loading storages:', error)
      // If API call fails (e.g., no pulsar plugin), set hasPulsarPlugin to false
      hasPulsarPlugin.value = false
      storages.value = []
      console.log('No pulsar plugin detected, setting hasPulsarPlugin to false')
    })
    .exception((code, message) => {
      console.warn('API returned error:', code, message)
      // If API returns error (e.g., no pulsar plugin), set hasPulsarPlugin to false
      hasPulsarPlugin.value = false
      storages.value = []
      console.log('No pulsar plugin detected, setting hasPulsarPlugin to false')
    })
    .end(() => {
      loading.value = false
      console.log('Load storages completed')
    })
}

const toggleStorageMenu = () => {
  showStorageMenu.value = !showStorageMenu.value
  showTenantMenu.value = false
  showNamespaceMenu.value = false
}

const changeStorage = (storage) => {
  selectedStorage.value = storage.value
  console.log('Selected storage:', storage)
  console.log('Storages:', storages.value)
  // Find the storage object by matching either key or name
  activeStorage.value = storage.value ? storages.value.find(s => s.key === storage.value || s.name === storage.value) : null
  console.log('Active storage:', activeStorage.value)
  showStorageMenu.value = false
  // Close details panel when storage changes
  topicDetails.value = null
  showTopicSidebar.value = false
  
  // Only load data if a storage is selected (not "请选择")
  if (storage.value) {
    // Use tenant and namespace from the storage object if available
    if (activeStorage.value) {
      selectedTenant.value = activeStorage.value.tenant || ''
      selectedNamespace.value = activeStorage.value.namespace || ''
      console.log('Auto-selected tenant from storage:', selectedTenant.value)
      console.log('Auto-selected namespace from storage:', selectedNamespace.value)
      
      // Load topics if both tenant and namespace are available
      if (selectedTenant.value && selectedNamespace.value) {
        refreshTopics()
      }
    }
  } else {
    // Clear data when "请选择" is selected
    console.log('Clearing data for "请选择"')
    tenants.value = []
    namespaces.value = []
    topics.value = []
    selectedTenant.value = ''
    selectedNamespace.value = ''
    showTenantMenu.value = false
    showNamespaceMenu.value = false
  }
}

const toggleTenantMenu = () => {
  showTenantMenu.value = !showTenantMenu.value
  showStorageMenu.value = false
  showNamespaceMenu.value = false
}

const changeTenant = (tenant) => {
  selectedTenant.value = tenant.value
  console.log('Selected tenant:', tenant)
  showTenantMenu.value = false
  // Close details panel when tenant changes
  topicDetails.value = null
  showTopicSidebar.value = false
  
  // Only load data if a tenant is selected
  if (tenant.value) {
    console.log('Calling refreshNamespaces for tenant:', tenant.value)
    // Reset namespace selection
    selectedNamespace.value = ''
    showNamespaceMenu.value = false
    topics.value = []
    refreshNamespaces(tenant.value)
  } else {
    // Clear data when no tenant is selected
    console.log('Clearing namespaces for no tenant')
    namespaces.value = []
    topics.value = []
    selectedNamespace.value = ''
    showNamespaceMenu.value = false
  }
}

const toggleNamespaceMenu = () => {
  showNamespaceMenu.value = !showNamespaceMenu.value
  showStorageMenu.value = false
  showTenantMenu.value = false
}

const changeNamespace = (namespace) => {
  selectedNamespace.value = namespace.value
  console.log('Selected namespace:', namespace)
  showNamespaceMenu.value = false
  // Close details panel when namespace changes
  topicDetails.value = null
  showTopicSidebar.value = false
  
  // Load topics when namespace is selected
  if (namespace.value) {
    console.log('Namespace selected, loading topics')
    refreshTopics()
  } else {
    // Clear topics when no namespace is selected
    console.log('No namespace selected, clearing topics')
    topics.value = []
  }
}

const onStorageChange = () => {
  if (selectedStorage.value) {
    refreshTenants()
  }
}

// Tab switching is no longer needed since we removed tabs
// const switchTab = (tabKey) => {
//   activeTab.value = tabKey
//   if (selectedStorage.value) {
//     if (tabKey === 'tenants') {
//       refreshTenants()
//     } else if (tabKey === 'namespaces') {
//       refreshNamespaces()
//     } else if (tabKey === 'topics') {
//       refreshTopics()
//     }
//   }
// }

const refreshTenants = () => {
  if (!selectedStorage.value || !hasPulsarPlugin.value) return
  
  loading.value = true
  console.log('Starting to refresh tenants for storage:', selectedStorage.value)
  
  // Use real API call
  request.sendPost(`/pulsar/${selectedStorage.value}/tenants`)
    .success(data => {
      // Handle different response formats
      console.log('Tenants API response data:', data)
      if (Array.isArray(data)) {
        // Check if data elements are objects with name property
        if (data.length > 0 && typeof data[0] === 'object' && data[0] !== null && 'name' in data[0]) {
          // Data is already in the correct format
          console.log('Data is already in correct format with name properties')
          tenants.value = data
        } else {
          // Data is an array of strings
          console.log('Data is array of strings, converting to objects')
          tenants.value = data.map(name => ({ name }))
        }
      } else {
        console.log('Data is not an array, setting empty array')
        tenants.value = []
      }
      console.log('Tenants value after processing:', tenants.value)
      console.log('Tenants length:', tenants.value.length)
    })
    .error((error) => {
      console.error('Error loading tenants:', error)
      modal.alert($i('pulsar.loadTenantsFailed'))
    })
    .end(() => {
      loading.value = false
      console.log('Refresh tenants completed')
    })
}

const refreshNamespaces = (tenant) => {
  if (!selectedStorage.value || !tenant || !hasPulsarPlugin.value) return
  
  loading.value = true
  console.log('Starting to refresh namespaces for tenant:', tenant, 'in storage:', selectedStorage.value)
  
  // Use real API call
  request.sendPost(`/pulsar/${selectedStorage.value}/namespaces`, { tenant })
    .success(namespaceData => {
      // Handle different response formats
      console.log('Namespace data for tenant', tenant, ':', namespaceData)
      if (Array.isArray(namespaceData)) {
        // Check if namespace data elements are objects with name property
        if (namespaceData.length > 0 && typeof namespaceData[0] === 'object' && namespaceData[0] !== null && 'name' in namespaceData[0]) {
          // Data is already in the correct format, add tenant property
          console.log('Data is already in correct format with name properties')
          namespaces.value = namespaceData.map(item => ({ ...item, tenant }))
        } else {
          // Data is an array of strings
          console.log('Data is array of strings, converting to objects')
          namespaces.value = namespaceData.map(name => ({ name, tenant }))
        }
      } else {
        console.log('Data is not an array, setting empty array')
        namespaces.value = []
      }
      console.log('Namespaces value after processing:', namespaces.value)
      console.log('Namespaces length:', namespaces.value.length)
    })
    .error((error) => {
      console.error('Error loading namespaces:', error)
      modal.alert($i('pulsar.loadNamespacesFailed'))
    })
    .end(() => {
      loading.value = false
      console.log('Refresh namespaces completed')
    })
}

const refreshTopics = () => {
  if (!selectedStorage.value || !selectedNamespace.value || !hasPulsarPlugin.value) return
  
  // Close details panel when refreshing topics
  topicDetails.value = null
  showTopicSidebar.value = false
  topicsLoading.value = true
  console.log('Starting to refresh topics for storage:', selectedStorage.value, 'and namespace:', selectedNamespace.value)
  
  // Use real API call
  request.sendPost(`/pulsar/${selectedStorage.value}/topics`, { namespace: selectedNamespace.value })
    .success(topicData => {
      // Handle different response formats
      console.log('Topic data:', topicData)
      if (Array.isArray(topicData)) {
        // Check if topic data elements are objects with name property
        if (topicData.length > 0 && typeof topicData[0] === 'object' && topicData[0] !== null && 'name' in topicData[0]) {
          // Data is already in the correct format
          console.log('Data is already in correct format with name properties')
          topics.value = topicData
        } else {
          // Data is an array of strings
          console.log('Data is array of strings, converting to objects')
          topics.value = topicData.map(name => ({ name }))
        }
      } else {
        console.log('Data is not an array, setting empty array')
        topics.value = []
      }
      console.log('Topics value after processing:', topics.value)
      console.log('Topics length:', topics.value.length)
    })
    .error((error) => {
      console.error('Error loading topics:', error)
      modal.alert($i('pulsar.loadTopicsFailed'))
    })
    .end(() => {
      topicsLoading.value = false
      console.log('Refresh topics completed')
    })
}

const createTenant = () => {
  newTenantName.value = ''
  showCreateTenantDialog.value = true
}

const submitCreateTenant = () => {
  if (!newTenantName.value) {
    modal.alert($i('pulsar.tenantNameRequired'))
    return
  }
  
  loading.value = true
  // Replace with actual API call when available
  setTimeout(() => {
    tenants.value.push({
      name: newTenantName.value
    })
    showCreateTenantDialog.value = false
    loading.value = false
  }, 1000)
}

const viewNamespaces = (tenant) => {
  modal.alert($i('pulsar.viewNamespaces', tenant.name))
}

const viewTopicDetails = (topic) => {
  if (!selectedStorage.value || !hasPulsarPlugin.value) return
  
  selectedTopic.value = topic
  showTopicSidebar.value = true
  topicLoading.value = true
  
  console.log('Fetching topic details for:', topic.name)
  
  // Use real API call
  request.sendPost(`/pulsar/${selectedStorage.value}/topicInfo`, { topic: topic.name })
    .success(data => {
      console.log('Topic info API response:', data)
      topicDetails.value = data
    })
    .error((error) => {
      console.error('Error loading topic info:', error)
      modal.alert($i('pulsar.loadTopicInfoFailed'))
    })
    .end(() => {
      topicLoading.value = false
    })
}

const refreshTopicDetails = () => {
  if (!selectedTopic.value) return
  viewTopicDetails(selectedTopic.value)
}

const deleteTenant = (tenant) => {
  modal.confirm(
    $i('pulsar.deleteTenant'),
    $i('pulsar.deleteTenantConfirm', tenant.name),
    () => {
      loading.value = true
      // Replace with actual API call when available
      setTimeout(() => {
        const index = tenants.value.findIndex(t => t.name === tenant.name)
        if (index > -1) {
          tenants.value.splice(index, 1)
        }
        loading.value = false
      }, 1000)
    }
  )
}

const createNamespace = () => {
  newNamespaceTenant.value = tenants.value.length > 0 ? tenants.value[0].name : ''
  newNamespaceName.value = ''
  showCreateNamespaceDialog.value = true
}

const submitCreateNamespace = () => {
  if (!newNamespaceTenant.value || !newNamespaceName.value) {
    modal.alert($i('pulsar.namespaceNameRequired'))
    return
  }
  
  loading.value = true
  // Replace with actual API call when available
  setTimeout(() => {
    const namespaceName = `${newNamespaceTenant.value}/${newNamespaceName.value}`
    namespaces.value.push({
      name: namespaceName,
      tenant: newNamespaceTenant.value
    })
    showCreateNamespaceDialog.value = false
    loading.value = false
  }, 1000)
}

const viewNamespace = (namespace) => {
  modal.alert($i('pulsar.viewNamespace', namespace.name))
}

const deleteNamespace = (namespace) => {
  modal.confirm(
    $i('pulsar.deleteNamespace'),
    $i('pulsar.deleteNamespaceConfirm', namespace.name),
    () => {
      loading.value = true
      // Replace with actual API call when available
      setTimeout(() => {
        const index = namespaces.value.findIndex(n => n.name === namespace.name)
        if (index > -1) {
          namespaces.value.splice(index, 1)
        }
        loading.value = false
      }, 1000)
    }
  )
}

const createTopic = () => {
  newTopicNamespace.value = namespaces.value.length > 0 ? namespaces.value[0].name : ''
  newTopicName.value = ''
  newTopicPartitions.value = 1
  showCreateDialog.value = true
}

const submitCreateTopic = () => {
  if (!newTopicNamespace.value || !newTopicName.value) {
    modal.alert($i('pulsar.topicNameRequired'))
    return
  }
  
  loading.value = true
  // Replace with actual API call when available
  setTimeout(() => {
    const topicName = `${newTopicNamespace.value}/${newTopicName.value}`
    topics.value.push({
      name: topicName
    })
    showCreateDialog.value = false
    loading.value = false
  }, 1000)
}

const deleteTopic = (topic) => {
  if (!topic || !selectedStorage.value) return
  
  // 显示确认框
  modal.confirm(
    $i('pulsar.deleteTopic'),
    $i('pulsar.deleteTopicConfirm', topic.name),
    () => {
      console.log('Deleting topic:', topic.name)
      console.log('selectedStorage:', selectedStorage.value)
      
      // 先关闭详情面板
      showTopicSidebar.value = false
      
      // Use real API call
      request.sendGet(`/pulsar/${selectedStorage.value}/deleteTopics`, {
        key: selectedStorage.value,
        topic: topic.name
      }, {
        showLoading: true
      })
        .success(data => {
          console.log('Delete topic API response:', data)
          console.log($i('pulsar.deleteTopicSuccess'))
          // 刷新话题列表
          if (selectedNamespace.value) {
            refreshTopics()
          } else {
            // 如果没有选择命名空间，直接从列表中移除话题
            const index = topics.value.findIndex(t => t.name === topic.name)
            if (index > -1) {
              topics.value.splice(index, 1)
            }
          }
        })
        .error((error) => {
          console.error('Error deleting topic:', error)
          console.warn($i('pulsar.deleteTopicFailed'), error)
        })
        .end(() => {
          console.log('Delete topic operation completed')
        })
    }
  )
}

// Reset consumer group offsets
const resetConsumerGroupOffsets = () => {
  if (!selectedStorage.value || !selectedTopic.value) {
    console.log('selectedStorage.value 或 selectedTopic.value 不存在')
    return
  }
  
  console.log('开始重置消费者组偏移量...')
  console.log('selectedStorage:', selectedStorage.value)
  console.log('selectedTopic:', selectedTopic.value)
  
  loadConsumerGroups(selectedTopic.value.name)
}

// Load consumer groups
const loadConsumerGroups = (topic) => {
  if (!selectedStorage.value) {
    console.log('selectedStorage.value 不存在')
    return
  }
  
  loadingConsumerGroups.value = true
  consumerGroups.value = []
  selectedConsumerGroup.value = ''
  
  console.log('开始加载消费者组列表...', topic)
  const apiUrl = `/pulsar/${selectedStorage.value}/topicInfo`
  console.log('API URL:', apiUrl)
  console.log('API 参数:', { topic: topic })
  
  // Use real API call to get consumer groups
  request.sendPost(apiUrl, {
    topic: topic
  })
    .success(data => {
      console.log('获取消费者组列表成功:', data)
      console.log('数据类型:', typeof data)
      console.log('数据内容:', data)
      
      if (data) {
        // 检查数据结构
        console.log('数据是否包含 stats:', 'stats' in data)
        if (typeof data === 'object' && data !== null) {
          console.log('数据的所有键:', Object.keys(data))
        }
        
        const subscriptionGroups = []
        
        if (data.stats && data.stats.subscriptions) {
          console.log('存在 stats.subscriptions 字段')
          console.log('subscriptions 类型:', typeof data.stats.subscriptions)
          console.log('subscriptions 内容:', data.stats.subscriptions)
          
          if (typeof data.stats.subscriptions === 'object' && data.stats.subscriptions !== null) {
            // 如果 subscriptions 是对象
            console.log('subscriptions 是对象')
            const keys = Object.keys(data.stats.subscriptions)
            console.log('subscriptions 键数量:', keys.length)
            console.log('subscriptions 键列表:', keys)
            
            // 直接使用所有订阅作为消费者组
            for (const key of keys) {
              console.log('添加订阅作为消费者组:', key)
              subscriptionGroups.push(key)
            }
          } else {
            console.log('subscriptions 不是对象')
          }
        } else {
          console.log('API 返回的数据中没有 stats.subscriptions 字段')
        }
        
        consumerGroups.value = subscriptionGroups
        console.log('最终添加的消费者组:', subscriptionGroups)
        console.log('consumerGroups.value 赋值后:', consumerGroups.value)
        console.log('consumerGroups.value 长度:', consumerGroups.value.length)
      } else {
        console.log('API 返回的数据为空')
      }
    })
    .error((error) => {
      console.error('获取消费者组列表失败:', error)
      console.error('错误详情:', error)
      console.warn($i('pulsar.loadConsumerGroupsFailed'), error)
    })
    .end(() => {
      console.log('加载消费者组列表完成')
      loadingConsumerGroups.value = false
      showConsumerGroupModal.value = true
    })
}

// Confirm reset consumer group offsets
const confirmResetOffsets = () => {
  if (!selectedStorage.value || !selectedTopic.value || !selectedConsumerGroup.value) {
    console.log('参数不完整')
    return
  }
  
  console.log('开始确认重置消费者组偏移量...')
  console.log('selectedStorage:', selectedStorage.value)
  console.log('selectedTopic:', selectedTopic.value)
  console.log('selectedConsumerGroup:', selectedConsumerGroup.value)
  
  request.sendGet(`/pulsar/${selectedStorage.value}/alterConsumerGroupOffsets`, {
    key: selectedStorage.value,
    topic: selectedTopic.value.name,
    group: selectedConsumerGroup.value
  }, {
    showLoading: true
  })
    .success(data => {
      console.log('重置消费者组偏移量成功:', data)
      console.log($i('pulsar.resetOffsetsSuccess'))
      showConsumerGroupModal.value = false
      // 重新加载 topic 详情
      viewTopicDetails(selectedTopic.value)
    })
    .error((error) => {
      console.error('重置消费者组偏移量失败:', error)
      console.warn($i('pulsar.resetOffsetsFailed'), error)
    })
    .end(() => {
      console.log('重置消费者组偏移量操作完成')
    })
}

// Format JSON for display
const formatJson = (obj) => {
  return JSON.stringify(obj, null, 2)
}


// Handle click outside to close dropdown
const handleClickOutside = (event) => {
  const storageSelector = document.querySelector('.magic-pulsar-storage-selector')
  const tenantSelector = document.querySelector('.magic-pulsar-tenant-selector')
  const namespaceSelector = document.querySelector('.magic-pulsar-namespace-selector')
  
  if (storageSelector && !storageSelector.contains(event.target)) {
    showStorageMenu.value = false
  }
  
  if (tenantSelector && !tenantSelector.contains(event.target)) {
    showTenantMenu.value = false
  }
  
  if (namespaceSelector && !namespaceSelector.contains(event.target)) {
    showNamespaceMenu.value = false
  }
}

// Lifecycle
onMounted(() => {
  // 延迟加载：等登录完成后再调用 availableStorages 接口，避免 token 无效
  bus.$on(Message.LOGINED, loadStorages)
  // Add click outside event listener
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  // Remove LOGINED event listener
  bus.$off(Message.LOGINED, loadStorages)
  // Remove click outside event listener
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.magic-pulsar {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100vh;
  width: 100%;
  background-color: var(--main-background-color);
}

.magic-pulsar-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.magic-pulsar-no-plugin {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  text-align: center;
  color: var(--text-secondary-color);
}

.magic-pulsar-no-plugin icon {
  margin-bottom: 16px;
  color: var(--primary-color);
}

.magic-pulsar-no-plugin p {
  margin: 8px 0;
  font-size: 16px;
}

.magic-pulsar-no-plugin-subtext {
  font-size: 14px !important;
  opacity: 0.7;
}

.magic-pulsar-header {
  padding: 4px 6px;
  border-bottom: 1px solid var(--main-border-color);
  background-color: var(--main-background-color);
  display: flex;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
  min-height: 30px;
  max-height: 30px;
}

.magic-pulsar-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  height: 100%;
}

.magic-pulsar-header-left {
  display: flex;
  align-items: center;
  margin-right: auto;
}

.magic-pulsar-header-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  cursor: pointer;
  padding: 0;
  box-sizing: border-box;
}

.magic-pulsar-header-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-pulsar-header-storage {
  margin-left: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  width: auto;
}

.magic-pulsar-storage-selector,
.magic-pulsar-tenant-selector,
.magic-pulsar-namespace-selector {
  position: relative;
  white-space: nowrap;
  width: 200px;
}

.magic-pulsar-button {
  display: flex;
  align-items: center;
  gap: 3px;
  padding: 2px 4px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  font-size: 10px;
  cursor: pointer;
  width: 100%;
  box-sizing: border-box;
  justify-content: space-between;
  height: 22px;
}

.magic-pulsar-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-pulsar-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background-color: var(--select-background-color);
}

.magic-pulsar-button-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0 4px;
}

.magic-pulsar-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 1px;
  border: 1px solid var(--main-border-color);
  border-radius: 3px;
  background-color: var(--main-background-color);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  min-width: 250px;
}

.magic-pulsar-dropdown-item {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 10px;
  color: var(--main-color);
  white-space: nowrap;
}

.magic-pulsar-dropdown-item:hover {
  background-color: var(--select-hover-background-color);
}

.magic-pulsar-dropdown-item.active {
  background-color: var(--select-hover-background-color);
  font-weight: bold;
}

.magic-pulsar-content {
  flex: 1;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

.magic-pulsar-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.magic-pulsar-content-wrapper.with-sidebar .magic-pulsar-topics {
  flex: 1;
}

.magic-pulsar-content-wrapper:not(.with-sidebar) .magic-pulsar-topics {
  flex: 1;
}

.magic-pulsar-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  height: 100%;
}

.magic-pulsar-topics {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  height: 100%;
}

/* Topic Details Panel */
.magic-pulsar-details-panel {
  width: 500px;
  height: 100%;
  border-left: 1px solid var(--main-border-color);
  background-color: var(--main-background-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.magic-pulsar-details-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--main-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--main-background-color);
}

.magic-pulsar-details-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: bold;
  color: var(--main-color);
}

.magic-pulsar-details-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.magic-pulsar-details-action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  color: var(--main-color);
  cursor: pointer;
  border-radius: 4px;
  padding: 0;
  transition: background-color 0.3s ease;
}

.magic-pulsar-details-action-button:hover {
  background-color: var(--main-hover-background-color);
}

.magic-pulsar-details-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  color: var(--main-color);
  cursor: pointer;
  border-radius: 4px;
  padding: 0;
  transition: background-color 0.3s ease;
}

.magic-pulsar-details-close:hover {
  background-color: red;
  color: white;
}

.magic-pulsar-details-content {
  flex: 1;
  padding: 0;
  overflow: hidden;
  background-color: var(--main-background-color);
  position: relative;
}

.magic-pulsar-details-json {
  margin: 0;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  line-height: 1.4;
  color: var(--main-color);
  background-color: rgba(0, 0, 0, 0.05);
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.magic-pulsar-details-content :deep(.magic-monaco-editor) {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}



.magic-pulsar-no-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: var(--main-color);
  font-size: 13px;
}

.magic-pulsar-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: var(--main-color);
}

.magic-pulsar-debug {
  background-color: #f0f0f0;
  padding: 10px;
  margin-bottom: 16px;
  border-radius: 4px;
  font-size: 12px;
  color: #333;
}

.magic-pulsar-debug pre {
  margin: 5px 0;
  padding: 10px;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 200px;
  overflow-y: auto;
}

/* Tabs Styles */
.magic-pulsar-tabs {
  display: flex;
  gap: 2px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--main-border-color);
}

.magic-pulsar-tab-button {
  padding: 8px 16px;
  border: 1px solid var(--main-border-color);
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  background-color: var(--select-background-color);
  color: var(--main-color);
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s ease;
}

.magic-pulsar-tab-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-pulsar-tab-button.active {
  background-color: var(--main-background-color);
  border-bottom: 1px solid var(--main-background-color);
  font-weight: bold;
}

.magic-pulsar-tab-content {
  min-height: 300px;
}

.magic-pulsar-controls {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.magic-pulsar-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  cursor: pointer;
  font-size: 12px;
}

.magic-pulsar-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-pulsar-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.magic-pulsar-tenants h4,
.magic-pulsar-namespaces h4,
.magic-pulsar-topics h4 {
  margin: 0 0 12px 0;
  font-size: 13px;
  font-weight: bold;
  color: var(--main-color);
}

.magic-pulsar-table-container {
  background-color: var(--main-background-color);
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}

/* Table row hover effect */
.magic-pulsar-table-container :deep(.magic-table-row:hover) {
  background-color: var(--main-hover-background-color) !important;
}

/* Fix scrollbar issue */
.magic-pulsar-table-container :deep(.magic-table-body) {
  overflow: hidden !important;
}

/* Ensure header and body columns align */
.magic-pulsar-table-container :deep(.magic-table-body .magic-table-row) {
  display: flex !important;
  width: 100% !important;
}

.magic-pulsar-table-container :deep(.magic-table-column) {
  flex: 1 !important;
  display: inline-flex !important;
  align-items: center !important;
}

.magic-pulsar-table-container :deep(.magic-table-body .magic-table-column:not([style*="flex:none"])) {
  justify-content: flex-start !important;
  padding-left: 16px !important;
}

/* Hide table header completely */
.magic-pulsar-table-container :deep(.magic-table-header) {
  display: none !important;
}

.magic-pulsar-tenant-name {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 0;
  justify-content: center;
  font-weight: 500;
  color: var(--main-color);
  min-height: 40px;
}

.magic-pulsar-tenant-icon {
  color: var(--tab-selected-border-color);
  flex-shrink: 0;
  font-weight: bold;
}

.magic-pulsar-action-buttons {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: center;
  padding: 12px 0;
}

.magic-pulsar-action-link {
  padding: 8px;
  color: var(--tab-selected-border-color);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  min-width: 40px;
  text-decoration: none;
  border-radius: 4px;
  transition: all 0.2s ease;
  background-color: rgba(64, 131, 202, 0.05);
}

.magic-pulsar-action-link:hover {
  background-color: rgba(64, 131, 201, 0.15);
}

.magic-pulsar-tenant-name-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--main-color);
  padding: 8px 16px;
  border-radius: 4px;
  background-color: rgba(0, 0, 0, 0.02);
  text-align: left;
  width: 100%;
  flex: 1;
}

/* Dialog Styles */
.magic-pulsar-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.magic-pulsar-dialog {
  background-color: var(--main-background-color);
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  width: 400px;
  max-width: 90%;
}

.magic-pulsar-dialog-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--main-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.magic-pulsar-dialog-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: bold;
  color: var(--main-color);
}

.magic-pulsar-dialog-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--main-color);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
  border-radius: 2px;
}

.magic-pulsar-dialog-close:hover {
  background-color: var(--main-hover-background-color);
}

.magic-pulsar-dialog-body {
  padding: 16px;
}

.magic-pulsar-form-item {
  margin-bottom: 12px;
}

.magic-pulsar-form-item label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--main-color);
}

.magic-pulsar-dialog-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--main-border-color);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.magic-pulsar-dialog-button {
  padding: 6px 12px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  cursor: pointer;
  font-size: 12px;
}

.magic-pulsar-dialog-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-pulsar-dialog-button.primary {
  background-color: var(--button-primary-background-color);
  color: white;
  border-color: var(--button-primary-background-color);
}

.magic-pulsar-dialog-button.primary:hover {
  background-color: var(--button-hover-background-color);
  border-color: var(--button-hover-background-color);
}



/* Make topic names clickable */
.magic-pulsar-tenant-name-text {
  cursor: pointer;
  transition: all 0.2s ease;
}

.magic-pulsar-tenant-name-text:hover {
  background-color: rgba(64, 131, 202, 0.1);
  text-decoration: underline;
}
/* Select Box Styles */
.magic-pulsar-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--input-border-color);
  border-radius: 3px;
  background-color: var(--select-background-color);
  color: var(--main-color);
  font-size: 13px;
}

.magic-pulsar-select:focus {
  outline: none;
  border-color: var(--primary-color);
}

/* Button Styles */
.magic-pulsar-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--main-border-color);
  border-radius: 3px;
  background-color: var(--main-background-color);
  color: var(--main-color);
  cursor: pointer;
  font-size: 13px;
  white-space: nowrap;
  width: 100%;
  justify-content: space-between;
  transition: all 0.2s ease;
}

.magic-pulsar-modal-footer .magic-pulsar-button {
  justify-content: center;
  width: auto;
  min-width: 100px;
}

.magic-pulsar-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-pulsar-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.magic-pulsar-button-primary {
  background-color: var(--primary-color) !important;
  color: white !important;
}

.magic-pulsar-button-primary:hover {
  background-color: var(--primary-hover-color) !important;
}

.magic-pulsar-button-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Modal Styles */
.magic-pulsar-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.magic-pulsar-modal {
  background-color: var(--main-background-color);
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.magic-pulsar-modal-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--main-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--header-background-color);
}

.magic-pulsar-modal-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--main-color);
}

.magic-pulsar-modal-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  color: var(--main-color);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.magic-pulsar-modal-close:hover {
  background-color: #ff4d4f;
  color: white;
}

.magic-pulsar-modal-body {
  padding: 16px;
  max-height: 300px;
  overflow-y: auto;
}

.magic-pulsar-form-group {
  margin-bottom: 16px;
}

.magic-pulsar-modal-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--main-border-color);
  display: flex;
  justify-content: center;
  gap: 8px;
  background-color: var(--header-background-color);
}

/* Loading Styles */
.magic-pulsar-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  color: var(--secondary-color);
  font-size: 13px;
}
</style>
