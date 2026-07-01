<template>
  <div ref="containerRef" class="magic-kafka">
    <div v-if="!loading && !hasKafkaPlugin" class="magic-kafka-no-plugin">
      <magic-icon icon="kafka" size="48px" />
      <p>{{ $i('kafka.noPlugin') }}</p>
      <p class="magic-kafka-no-plugin-subtext">{{ $i('kafka.noPluginSubtext') }}</p>
    </div>
    <div v-else class="magic-kafka-wrapper">
      <!-- Toolbar -->
      <div class="magic-kafka-header">
        <div class="magic-kafka-header-actions">
          <div class="magic-kafka-header-left">
          </div>
          <div class="magic-kafka-header-storage">
            <div class="magic-kafka-storage-selector">
              <button class="magic-kafka-button" @click="toggleStorageMenu" :disabled="loading">
                <magic-icon icon="kafka" size="16px" />
                <span class="magic-kafka-button-text">{{ loading ? $i('kafka.loading') : (activeStorage ? activeStorage.name : $i('kafka.pleaseSelect')) }}</span>
                <magic-icon icon="arrow-bottom" size="12px" />
              </button>
              <div v-if="showStorageMenu && storageOptionsWithDefault.length > 0 && !loading" class="magic-kafka-dropdown">
                <div 
                  v-for="storage in storageOptionsWithDefault" 
                  :key="storage.value"
                  class="magic-kafka-dropdown-item"
                  :class="{ active: activeStorage && storage.value === activeStorage.key }"
                  @click="changeStorage(storage)"
                >
                  {{ storage.label }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="magic-kafka-content">
        <div v-if="loading" class="magic-kafka-loading">
          <span>{{ $i('kafka.loading') }}</span>
        </div>
        <div class="magic-kafka-content-wrapper" :class="{ 'with-sidebar': showTopicSidebar }">
  <div class="magic-kafka-main">
    <!-- Topics List -->
    <div class="magic-kafka-topics">
      <div class="magic-kafka-table-container">
        <magic-table :data="topics" border>
          <magic-table-column :title="''" align="left" #default="{ row }">
            <span class="magic-kafka-topic-name-text" @click="viewTopicDetails(row)">{{ row.name }}</span>
          </magic-table-column>
        </magic-table>
      </div>
    </div>
    
    <!-- Topic Details Panel -->
    <div v-if="showTopicSidebar" class="magic-kafka-details-panel">
      <div class="magic-kafka-details-header">
        <h3>{{ selectedTopic?.name }}</h3>
        <div class="magic-kafka-details-actions">
          <button class="magic-kafka-details-action-button" @click="viewTopicDetails(selectedTopic)" :title="$i('kafka.refresh')">
            <magic-icon icon="refresh" size="16px" />
          </button>
          <button class="magic-kafka-details-action-button" @click="resetConsumerGroupOffsets" :title="$i('kafka.resetOffsets')">
            <magic-icon icon="update" size="16px" />
          </button>
          <button class="magic-kafka-details-action-button" @click="deleteTopic(selectedTopic)" :title="$i('kafka.delete')">
            <magic-icon icon="delete" size="16px" />
          </button>
          <button class="magic-kafka-details-close" @click="showTopicSidebar = false">
            <magic-icon icon="close" size="16px" />
          </button>
        </div>
      </div>
      <div class="magic-kafka-details-content">
        <div v-if="topicLoading" class="magic-kafka-loading">
          <span>{{ $i('kafka.loading') }}</span>
        </div>
        <div v-else>
          <div v-if="topicDetails">
            <MagicMonacoEditor :value="formatJson(topicDetails)" language="json" :readonly="true" />
          </div>
          <div v-else class="magic-kafka-no-data">
            {{ $i('kafka.noTopicInfo') }}
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
      </div>
    </div>
  </div>
  
  <!-- Consumer Group Selection Modal -->
  <div v-if="showConsumerGroupModal" class="magic-kafka-modal-overlay">
    <div class="magic-kafka-modal">
      <div class="magic-kafka-modal-header">
        <h3>{{ $i('kafka.resetOffsets') }}</h3>
        <button class="magic-kafka-modal-close" @click="showConsumerGroupModal = false">
          <magic-icon icon="close" size="16px" />
        </button>
      </div>
      <div class="magic-kafka-modal-body">
        <div v-if="loadingConsumerGroups" class="magic-kafka-loading">
          <span>{{ $i('kafka.loading') }}</span>
        </div>
        <div v-else>
          <div class="magic-kafka-form-group">
            <select v-model="selectedConsumerGroup" class="magic-kafka-select">
              <option value="">{{ $i('kafka.pleaseSelectConsumerGroup') }}</option>
              <option v-for="group in consumerGroups" :key="group" :value="group">
                {{ group }}
              </option>
            </select>
          </div>
        </div>
      </div>
      <div class="magic-kafka-modal-footer">
        <button class="magic-kafka-button magic-kafka-button-primary" @click="confirmResetOffsets" :disabled="!selectedConsumerGroup" :title="$i('kafka.confirm')">
          <magic-icon icon="save" size="16px" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue'
import request from '../../../scripts/request.js'
import $i from '../../../scripts/i18n.js'
import MagicMonacoEditor from '../../common/magic-monaco-editor.vue'
import MagicTable from '../../common/data/magic-table.js'
import MagicTableColumn from '../../common/data/magic-table-column.vue'
import MagicIcon from '../../common/icon/magic-icon.vue'
import modal from '../../common/dialog/magic-modal.js'
import bus from '../../../scripts/bus.js'
import Message from '../../../scripts/constants/message.js'

// Constants
let availableStorages = []

// Stores
const storages = ref([])
const activeStorage = ref(null)
const showStorageMenu = ref(false)
const loading = ref(false)
const topics = ref([])
const topicDetails = ref(null)
const selectedTopic = ref(null)
const showTopicSidebar = ref(false)
const topicLoading = ref(false)
const hasKafkaPlugin = ref(true)

// Consumer group reset variables
const showConsumerGroupModal = ref(false)
const consumerGroups = ref([])
const selectedConsumerGroup = ref('')
const loadingConsumerGroups = ref(false)

// Computed
const storageOptionsWithDefault = computed(() => {
  const options = [{ label: $i('kafka.pleaseSelect'), value: '' }]
  if (storages.value && storages.value.length > 0) {
    storages.value.forEach(storage => {
      options.push({
        label: storage.name,
        value: storage.key
      })
    })
  }
  return options
})

// Format JSON for display
const formatJson = (obj) => {
  return JSON.stringify(obj, null, 2)
}
// Handle click outside to close dropdown
const handleClickOutside = (event) => {
  const storageSelector = document.querySelector('.magic-kafka-storage-selector')
  
  if (storageSelector && !storageSelector.contains(event.target)) {
    showStorageMenu.value = false
  }
}

// Load available storages
const loadStorages = () => {
  loading.value = true
  request.sendGet('/kafka/availableStorages')
    .success(data => {
      if (data) {
        storages.value = data
        availableStorages = data
        // 不自动选择第一个存储，保持默认的"请选择"状态
      }
    })
    .error(() => {
      // Plugin not installed
      hasKafkaPlugin.value = false
      storages.value = []
    })
    .end(() => {
      loading.value = false
    })
}

// Change storage
const changeStorage = (storage) => {
  if (storage.value) {
    activeStorage.value = {
      key: storage.value,
      name: storage.label
    }
    showStorageMenu.value = false
    topics.value = []
    topicDetails.value = null
    showTopicSidebar.value = false
    // Load topics for this storage
    refreshTopics()
  } else {
    activeStorage.value = null
    showStorageMenu.value = false
    topics.value = []
    topicDetails.value = null
    showTopicSidebar.value = false
  }
}

// Toggle storage menu
const toggleStorageMenu = () => {
  if (!loading.value) {
    showStorageMenu.value = !showStorageMenu.value
  }
}

// Refresh topics
const refreshTopics = () => {
  if (!activeStorage.value) return
  
  console.log('开始加载 topics...', activeStorage.value)
  loading.value = true
  request.sendGet(`/kafka/${activeStorage.value.key}/listTopics`)
    .success(data => {
      console.log('获取 topics 成功:', data)
      if (data) {
        console.log('topics 数据:', data)
        topics.value = data
        console.log('topics.value 赋值后:', topics.value)
      }
    })
    .error((error) => {
      console.warn($i('kafka.loadTopicsFailed'), error)
    })
    .end(() => {
      console.log('加载 topics 完成')
      loading.value = false
    })
}

// View topic details
const viewTopicDetails = (topic) => {
  if (!activeStorage.value) return
  
  selectedTopic.value = topic
  showTopicSidebar.value = true
  topicLoading.value = true
  
  console.log('开始加载 topic 详情...', topic)
  request.sendGet(`/kafka/${activeStorage.value.key}/describeTopics`, {
    topic: topic.name
  })
    .success(data => {
      console.log('获取 topic 详情成功:', data)
      if (data) {
        console.log('topic 详情数据:', data)
        topicDetails.value = data
        console.log('topicDetails.value 赋值后:', topicDetails.value)
      }
    })
    .error((error) => {
      console.warn($i('kafka.loadTopicInfoFailed'), error)
    })
    .end(() => {
      console.log('加载 topic 详情完成')
      topicLoading.value = false
    })
}

// Reset consumer group offsets
const resetConsumerGroupOffsets = () => {
  if (!activeStorage.value || !selectedTopic.value) {
    console.log('activeStorage.value 或 selectedTopic.value 不存在')
    return
  }
  
  console.log('开始重置消费者组偏移量...')
  console.log('activeStorage:', activeStorage.value)
  console.log('selectedTopic:', selectedTopic.value)
  
  loadConsumerGroups(selectedTopic.value.name)
}

// Load consumer groups
const loadConsumerGroups = (topic) => {
  if (!activeStorage.value) {
    console.log('activeStorage.value 不存在')
    return
  }
  
  loadingConsumerGroups.value = true
  consumerGroups.value = []
  selectedConsumerGroup.value = ''
  
  console.log('开始加载消费者组列表...', topic)
  const apiUrl = `/kafka/${activeStorage.value.key}/describeTopics`
  console.log('API URL:', apiUrl)
  console.log('API 参数:', { topic: topic })
  
  request.sendGet(apiUrl, {
    topic: topic
  })
    .success(data => {
      console.log('获取消费者组列表成功:', data)
      console.log('数据类型:', typeof data)
      console.log('数据内容:', data)
      
      if (data) {
        // 检查数据结构
        console.log('数据是否包含 subscriberGroups:', 'subscriberGroups' in data)
        if (typeof data === 'object' && data !== null) {
          console.log('数据的所有键:', Object.keys(data))
        }
        
        const emptyGroups = []
        
        if (data.subscriberGroups) {
          console.log('存在 subscriberGroups 字段')
          console.log('subscriberGroups 类型:', typeof data.subscriberGroups)
          console.log('subscriberGroups 内容:', data.subscriberGroups)
          
          if (typeof data.subscriberGroups === 'object' && data.subscriberGroups !== null) {
            // 如果 subscriberGroups 是对象
            console.log('subscriberGroups 是对象')
            const keys = Object.keys(data.subscriberGroups)
            console.log('subscriberGroups 键数量:', keys.length)
            console.log('subscriberGroups 键列表:', keys)
            
            for (const key of keys) {
              console.log('处理消费者组键:', key)
              const group = data.subscriberGroups[key]
              console.log('消费者组信息:', group)
              // 检查消费者组状态是否为 EMPTY
              const groupState = group.state || group.status
              console.log('消费者组状态:', groupState)
              if (groupState === 'EMPTY') {
                console.log('添加状态为 EMPTY 的消费者组:', key)
                emptyGroups.push(key)
              } else {
                console.log('跳过状态为', groupState, '的消费者组:', key)
              }
            }
          } else {
            console.log('subscriberGroups 不是对象')
          }
        } else {
          console.log('API 返回的数据中没有 subscriberGroups 字段')
        }
        
        consumerGroups.value = emptyGroups
        console.log('最终添加的消费者组:', emptyGroups)
        console.log('consumerGroups.value 赋值后:', consumerGroups.value)
        console.log('consumerGroups.value 长度:', consumerGroups.value.length)
      } else {
        console.log('API 返回的数据为空')
      }
    })
    .error((error) => {
      console.error('获取消费者组列表失败:', error)
      console.error('错误详情:', error)
      console.warn($i('kafka.loadConsumerGroupsFailed'), error)
    })
    .end(() => {
      console.log('加载消费者组列表完成')
      loadingConsumerGroups.value = false
      showConsumerGroupModal.value = true
      console.log('showConsumerGroupModal.value 已设置为:', showConsumerGroupModal.value)
      console.log('最终 consumerGroups.value:', consumerGroups.value)
    })
}

// Confirm reset consumer group offsets
const confirmResetOffsets = () => {
  if (!activeStorage.value || !selectedTopic.value || !selectedConsumerGroup.value) {
    console.log('参数不完整')
    return
  }
  
  console.log('开始确认重置消费者组偏移量...')
  console.log('activeStorage:', activeStorage.value)
  console.log('selectedTopic:', selectedTopic.value)
  console.log('selectedConsumerGroup:', selectedConsumerGroup.value)
  
  request.sendGet(`/kafka/${activeStorage.value.key}/alterConsumerGroupOffsets`, {
    topic: selectedTopic.value.name,
    group: selectedConsumerGroup.value
  })
    .success(data => {
      console.log('重置消费者组偏移量成功:', data)
      console.log($i('kafka.resetOffsetsSuccess'))
      showConsumerGroupModal.value = false
      // 重新加载 topic 详情
      viewTopicDetails(selectedTopic.value)
    })
    .error((error) => {
      console.error('重置消费者组偏移量失败:', error)
      console.warn($i('kafka.resetOffsetsFailed'), error)
    })
    .end(() => {
      console.log('重置消费者组偏移量操作完成')
    })
}

// Delete topic
const deleteTopic = (topic) => {
  if (!activeStorage.value || !topic) {
    console.log('activeStorage.value 或 topic 不存在')
    return
  }
  
  // 显示确认框
  modal.confirm(
    $i('kafka.delete'),
    $i('kafka.deleteTopicConfirm', topic.name),
    () => {
      console.log('开始删除话题...', topic)
      console.log('activeStorage:', activeStorage.value)
      
      // 先关闭详情面板
      showTopicSidebar.value = false
      
      request.sendGet(`/kafka/${activeStorage.value.key}/deleteTopics`, {
        key: activeStorage.value.key,
        topic: topic.name
      }, {
        showLoading: true
      })
        .success(data => {
          console.log('删除话题成功:', data)
          console.log($i('kafka.deleteTopicSuccess'))
          // 刷新话题列表
          refreshTopics()
        })
        .error((error) => {
          console.error('删除话题失败:', error)
          console.warn($i('kafka.deleteTopicFailed'), error)
        })
        .end(() => {
          console.log('删除话题操作完成')
        })
    }
  )
}// Lifecycle
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

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}

* {
  box-sizing: border-box;
}
</style>

<style scoped>
.magic-kafka {
  display: flex;
  flex: 1;
  flex-direction: column;
  height: 100%;
  min-height: 100%;
  background-color: var(--main-background-color);
  position: relative;
}

.magic-kafka-no-plugin {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 20px;
  text-align: center;
  color: var(--text-secondary-color);
}

.magic-kafka-no-plugin icon {
  margin-bottom: 16px;
  color: var(--primary-color);
}

.magic-kafka-no-plugin p {
  margin: 8px 0;
  font-size: 16px;
}

.magic-kafka-no-plugin-subtext {
  font-size: 14px !important;
  opacity: 0.7;
}

.magic-kafka-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.magic-kafka-header {
  padding: 4px 6px;
  border-bottom: 1px solid var(--main-border-color);
  background-color: var(--main-background-color);
  position: sticky;
  top: 0;
  z-index: 100;
  min-height: 30px;
  max-height: 30px;
  display: flex;
  align-items: center;
}

.magic-kafka-header-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 100%;
}

.magic-kafka-header-left {
  display: flex;
  align-items: center;
  margin-right: 12px;
}

.magic-kafka-header-button {
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
  margin-right: 8px;
}

.magic-kafka-header-button:hover {
  background-color: var(--main-hover-background-color);
}

.magic-kafka-storage-selector {
  position: relative;
  white-space: nowrap;
  width: 200px;
}

.magic-kafka-button {
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

.magic-kafka-modal-footer .magic-kafka-button {
  justify-content: center;
  width: auto;
  min-width: 100px;
}

.magic-kafka-button:hover {
  background-color: var(--select-hover-background-color);
}

.magic-kafka-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.magic-kafka-button-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.magic-kafka-dropdown {
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

.magic-kafka-dropdown-item {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 11px;
  color: var(--main-color);
  white-space: nowrap;
}

.magic-kafka-dropdown-item:hover {
  background-color: var(--select-hover-background-color);
}

.magic-kafka-dropdown-item.active {
  background-color: var(--select-hover-background-color);
  font-weight: bold;
}

.magic-kafka-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
  height: 100%;
}

.magic-kafka-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.magic-kafka-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  height: 100%;
}

.magic-kafka-topics {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  height: 100%;
}

.magic-kafka-table-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--main-background-color);
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}

.magic-table {
  display: flex;
  flex-direction: column;
}

.magic-table-body {
  flex: 1;
  overflow-y: auto;
}

/* Table row hover effect */
.magic-kafka-table-container :deep(.magic-table-row:hover) {
  background-color: var(--main-hover-background-color) !important;
}

/* Fix scrollbar issue */
.magic-kafka-table-container :deep(.magic-table-body) {
  overflow: hidden !important;
}

/* Ensure header and body columns align */
.magic-kafka-table-container :deep(.magic-table-body .magic-table-row) {
  display: flex !important;
  width: 100% !important;
}

.magic-kafka-table-container :deep(.magic-table-column) {
  flex: 1 !important;
  display: inline-flex !important;
  align-items: center !important;
}

.magic-kafka-table-container :deep(.magic-table-body .magic-table-column:not([style*="flex:none"])) {
  justify-content: flex-start !important;
  padding-left: 16px !important;
}

/* Hide table header completely */
.magic-kafka-table-container :deep(.magic-table-header) {
  display: none !important;
}

.magic-kafka-topic-name-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--main-color);
  padding: 8px 16px;
  border-radius: 4px;
  background-color: rgba(0, 0, 0, 0.02);
  text-align: left;
  width: 100%;
  flex: 1;
  transition: all 0.2s ease;
}

.magic-kafka-topic-name-text:hover {
  background-color: rgba(64, 131, 202, 0.1);
  text-decoration: underline;
}

/* Topic Details Panel */
.magic-kafka-details-panel {
  width: 500px;
  height: 100%;
  border-left: 1px solid var(--main-border-color);
  background-color: var(--main-background-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.magic-kafka-content-wrapper.with-sidebar .magic-kafka-topics {
  flex: 1;
}

.magic-kafka-content-wrapper:not(.with-sidebar) .magic-kafka-topics {
  flex: 1;
}

.magic-kafka-details-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--main-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--main-background-color);
}

.magic-kafka-details-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: bold;
  color: var(--main-color);
}

.magic-kafka-details-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.magic-kafka-details-action-button {
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

.magic-kafka-details-action-button:hover {
  background-color: var(--main-hover-background-color);
}

.magic-kafka-details-close {
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

.magic-kafka-details-close:hover {
  background-color: red;
  color: white;
}

.magic-kafka-details-content {
  flex: 1;
  padding: 0;
  overflow: hidden;
  background-color: var(--main-background-color);
  position: relative;
}

.magic-kafka-details-json {
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

.magic-kafka-details-content :deep(.magic-monaco-editor) {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.magic-kafka-no-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: var(--main-color);
  font-size: 13px;
}

.magic-kafka-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: var(--main-color);
}

.magic-kafka-topic-name-text {
  cursor: pointer;
  color: var(--primary-color);
}

.magic-kafka-topic-name-text:hover {
  text-decoration: underline;
}



/* Animation keyframes */
@keyframes kafka-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* Modal styles */
.magic-kafka-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.magic-kafka-modal {
  background-color: var(--main-background-color);
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  width: 400px;
  max-width: 90%;
}

.magic-kafka-modal-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--main-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.magic-kafka-modal-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: bold;
  color: var(--main-color);
}

.magic-kafka-modal-close {
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

.magic-kafka-modal-close:hover {
  background-color: red;
  color: white;
}

.magic-kafka-modal-body {
  padding: 16px;
}

.magic-kafka-form-group {
  margin-bottom: 16px;
}

.magic-kafka-form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--main-color);
}

.magic-kafka-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--main-border-color);
  border-radius: 3px;
  background-color: var(--main-background-color);
  color: var(--main-color);
  font-size: 13px;
}

.magic-kafka-select:focus {
  outline: none;
  border-color: var(--primary-color);
}

.magic-kafka-modal-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--main-border-color);
  display: flex;
  justify-content: center;
  gap: 8px;
}

.magic-kafka-button-primary {
  background-color: var(--primary-color) !important;
  color: white !important;
}

.magic-kafka-button-primary:hover {
  background-color: var(--primary-hover-color) !important;
}

.magic-kafka-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>