<template>
  <div class="magic-aia-container">
    <!-- 聊天内容区域 -->
    <div class="magic-aia-content">
      <!-- 欢迎信息 -->
      <div v-if="messages.length === 0" class="welcome-container">
        <div class="welcome-header">
          <div class="welcome-robot">🤖</div>
          <h2 class="welcome-title">{{ $i('aia.title') }}</h2>
        </div>
        <p class="welcome-description">
          {{ $i('aia.welcome') }}
        </p>

      </div>

      <!-- 聊天消息区域 -->
      <div v-else class="chat-messages" ref="messagesContainer">
        <div v-for="message in messages" :key="message.id" class="message-wrapper" :class="{'user-message': message.isUser, 'ai-message': !message.isUser }">
          <!-- 用户消息：复制按钮 -> 消息内容 -> 头像 -->
          <template v-if="message.isUser">
            <button class="user-message-copy-btn" @click="copyUserMessage(message)" :title="$i('aia.copy')">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                <path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
              </svg>
            </button>
            <div class="message" :class="{'user-message': message.isUser, 'ai-message': !message.isUser }">
              <div class="message-content">
                <!-- 用户消息使用v-html或magic-monaco-editor渲染 -->
                <template v-for="(contentItem, index) in (Array.isArray(message.content) ? message.content : [message.content])" :key="index">
                  <!-- 除了第一个元素外，其他元素前添加横线分隔符 -->
                  <div v-if="index > 0" class="v-html-divider"></div>
                  
                      <!-- 使用v-html渲染所有类型的内容 -->
                    <div class="text-content" v-html="renderMarkdown(contentItem)"></div>
                </template>
              </div>
            </div>
            <div class="message-avatar">
              <span class="avatar">{{ message.isUser ? '👤' : '🤖' }}</span>
            </div>
          </template>
          <!-- AI消息：头像 -> 消息内容 -->
          <template v-else>
            <div class="message-avatar">
              <span class="avatar">{{ message.isUser ? '👤' : '🤖' }}</span>
            </div>
            <div class="message" :class="{
              'user-message': message.isUser, 
              'ai-message': !message.isUser,
              'isTimeout': message.isTimeout // 添加超时状态类
            }" :style="{
              // 思考过程中隐藏消息框样式
              ...(message.isThinking ? {
                border: 'none !important',
                background: 'transparent !important',
                boxShadow: 'none !important',
                padding: '0 !important',
                margin: '0 !important'
              } : {})
            }">
              <div class="message-content">
                <!-- 思考中状态 -->
                <div v-if="message.isThinking" class="thinking-indicator">
                  <span class="thinking-text">{{ $i('aia.typing') }}</span>
                  <div class="thinking-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                  <span class="thinking-timeout" v-if="message.timeoutCountdown">
                    {{ message.timeoutCountdown }}s
                  </span>
                </div>

                <!-- 思考完成，显示完整内容 -->
                <template v-else>
                  <!-- AI响应内容 -->
                  <div class="ai-response">
                    <!-- 使用系统自带的magic-monaco-editor显示代码块 -->
                    <div class="message-content-wrapper">
                      <!-- 支持多个v-html元素，每个对应一条AI消息，之间用横线分隔 -->
                    <template v-for="(contentItem, index) in (Array.isArray(message.content) ? message.content : [message.content])" :key="index">
                      <!-- 除了第一个元素外，其他元素前添加横线分隔符 -->
                      <div v-if="index > 0" class="v-html-divider"></div>
                      
                      <!-- 使用v-html渲染所有类型的内容 -->
                      <div class="text-content" v-html="renderMarkdown(contentItem)"></div>
                    </template>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </template>
        </div>

      </div>

      <!-- 输入区域 -->
      <div class="input-section">
        <!-- 输入框容器 -->
        <div class="input-container">
          <div class="input-wrapper">
            <textarea
                v-model="inputMessage"
                ref="messageInput"
                class="message-input"
                :placeholder="wsConnected ? $i('aia.placeholder') : $i('aia.placeholder') + $i('aia.wsNotConnected')"
                @keydown="handleKeyDown"
                rows="1"
                :maxlength="MAX_MESSAGE_LENGTH"
                :disabled="!wsConnected || loading || messages.some(msg => msg.isThinking)"
                style="padding-right: 35px;"
            ></textarea>
            <span 
                v-show="inputMessage"
                @click="inputMessage = ''"
                style="
                    position: absolute;
                    right: 16px;
                    top: 10px;
                    cursor: pointer;
                    color: #999;
                    font-size: 18px;
                    font-weight: bold;
                    z-index: 10;
                    user-select: none;
                "
            >
                ✕
            </span>
            <div class="input-hints">
              <div class="input-buttons">
                <button class="clear-btn" @click="clearMessages" :disabled="messages.length === 0 || loading || messages.some(msg => msg.isThinking)" :title="$i('aia.clear')">
                  {{ $i('aia.clear') }}
                </button>
                <!-- 间隔符 -->
                <div class="ws-separator"></div>
                <!-- WebSocket连接按钮 -->
                <button class="connect-ws-btn" @click="wsConnected ? disconnectWebSocket() : initWebSocket()" :disabled="loading" :class="{ 'connected': wsConnected }" :title="wsConnected ? $i('aia.disconnect') : $i('aia.connect')">
                  <svg v-if="!wsConnected" viewBox="0 0 24 24" width="12" height="12" style="margin-right: 2px;">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z" fill="currentColor"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" width="12" height="12" style="margin-right: 2px;">
                    <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  {{ wsConnected ? $i('aia.disconnect') : loading ? $i('aia.connecting') : $i('aia.connect') }}
                </button>
                <!-- WebSocket设置 -->
                <div class="ws-settings">
                  <label class="ws-setting-item">
                    <span class="ws-status-value" :class="{'ws-status-connecting': wsConnecting, 'ws-status-connected': wsConnected, 'ws-status-failed': wsConnectFailed, 'ws-status-disconnected': !wsConnected && !wsConnectFailed && wsInitialized, 'ws-status-not-connected': !wsConnected && !wsConnectFailed && !wsInitialized}">
                      {{ wsStatus }}
                    </span>
                  </label>
                  <label class="ws-setting-item">
                    <span class="ws-setting-label">{{ $i('aia.autoReconnect') }}</span>
                    <div class="ws-setting-switch">
                      <input type="checkbox" v-model="wsAutoReconnect" :title="$i('aia.autoReconnect')" :disabled="!canOperateWS">
                      <span class="slider"></span>
                    </div>
                  </label>
                  <label class="ws-setting-item">
                    <span class="ws-setting-label">{{ $i('aia.reconnectTimes') }}</span>
                    <input type="number" v-model.number="wsMaxReconnectAttempts" min="1" max="20" class="ws-setting-input" :title="$i('aia.reconnectTimes')" :disabled="!canOperateWS">
                  </label>
                  <label class="ws-setting-item">
                    <span class="ws-setting-label">{{ $i('aia.reconnectInterval') }}</span>
                    <input type="number" v-model.number="wsReconnectInterval" min="1" max="60" class="ws-setting-input" :title="$i('aia.reconnectInterval')" :disabled="!canOperateWS">
                  </label>
                  <label class="ws-setting-item">
                    <span class="ws-setting-label">{{ $i('aia.connectTimeout') }}</span>
                    <input type="number" v-model.number="wsConnectTimeout" min="1" max="60" class="ws-setting-input" :title="$i('aia.connectTimeout')" :disabled="!canOperateWS">
                  </label>
                  <label class="ws-setting-item">
                    <span class="ws-setting-label">{{ $i('aia.thinkingTimeout') }}</span>
                    <input type="number" v-model.number="aiThinkingTimeout" min="10" max="300" class="ws-setting-input" :title="$i('aia.thinkingTimeout')" :disabled="!canOperateWS">
                  </label>
                </div>
              </div>
              <div class="input-stats">
                <span class="hint-text">按 <kbd>Enter</kbd> {{ $i('aia.send') }}，按 <kbd>Shift+Enter</kbd> 换行</span>
                <div class="ws-separator"></div>
                <span class="character-count" :class="{ 'near-limit': (inputMessage || '').length > MAX_MESSAGE_LENGTH * 0.8 }">
                  {{ (inputMessage || '').length }}/{{ MAX_MESSAGE_LENGTH }}
                </span>
              </div>
            </div>
          </div>
          <!-- 语音输入按钮 -->
          <button class="voice-btn" @click="toggleVoiceInput" :title="isVoiceActive ? '停止语音输入' : wsConnected ? '语音输入' : 'WebSocket未连接，无法使用语音输入'" :class="{ 'active': isVoiceActive }" :disabled="!wsConnected || loading || messages.some(msg => msg.isThinking)">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path d="M12 14c1.66 0 2.99-1.34 2.99-3L15 5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z" fill="currentColor"/>
            </svg>
          </button>
          <button class="send-btn" @click="sendMessage" :disabled="!inputMessage?.trim() || !wsConnected || loading || messages.some(msg => msg.isThinking)" :title="wsConnected ? $i('aia.send') : 'WebSocket未连接，无法发送消息'">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" fill="currentColor"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>


</template>

<script setup>
import { ref, nextTick, computed, watch, onMounted, onUnmounted } from 'vue'
import $i from '../../../scripts/i18n.js'
import { replaceURL } from '../../../scripts/utils.js'
import { marked } from 'marked'


const props = defineProps({
  // 可以根据需要添加props
})

const emit = defineEmits(['onAIResponse'])

// 配置 marked 库，自定义代码块渲染
const renderer = new marked.Renderer();

// HTML转义函数，确保代码中的HTML标签被正确转义
const escapeHtml = (text) => {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
};

// 自定义代码块渲染，将Markdown代码块渲染为具有特定样式的pre和code标签，带行号和复制按钮
renderer.code = (code, language, isEscaped) => {
  // 确保code是字符串类型，特别处理代码块对象
    let codeStr = '';
    if (typeof code === 'object' && code !== null) {
      // 检查是否是代码块对象，有raw字段
      if (code.raw && typeof code.raw === 'string') {
        // 如果是代码块对象，直接使用raw字段的内容
        codeStr = code.raw;
      } else {
        // 其他对象，转换为JSON字符串
        codeStr = JSON.stringify(code, null, 2);
      }
    } else {
      // 字符串类型，直接使用
      codeStr = String(code);
    }
    
    // 移除代码块中的Markdown代码块标记（如```java和```）
    // 处理多种格式的代码块标记
    
    // 情况1：直接匹配代码块标记（如```java和```）
    const codeBlockRegex = /^```\w*\s*([\s\S]*?)\s*```$/;
    const match = codeStr.match(codeBlockRegex);
    if (match && match[1]) {
      // 如果匹配到完整的代码块，只保留实际代码内容
      codeStr = match[1];
    } else {
      // 情况2：分别检查开头和结尾的标记
      const codeBlockStartRegex = /^```\w*\s*/;
      const codeBlockEndRegex = /\s*```$/;
      
      if (codeBlockStartRegex.test(codeStr)) {
        codeStr = codeStr.replace(codeBlockStartRegex, '');
      }
      
      if (codeBlockEndRegex.test(codeStr)) {
        codeStr = codeStr.replace(codeBlockEndRegex, '');
      }
    }
    
    // 确保language是字符串类型
    let lang = language ? language : '';
    
    // 如果代码块对象中有lang字段，使用它
    if (typeof code === 'object' && code !== null && code.lang && typeof code.lang === 'string') {
      lang = code.lang;
    }
  
  // 生成唯一的代码块ID，用于复制功能
  const codeBlockId = `code-block-${generateSecureId()}`;
  
  // 添加行号
  const lines = codeStr.split('\n');
  // 移除最后一个空行（如果有）
  if (lines.length > 0 && lines[lines.length - 1] === '') {
    lines.pop();
  }
  
  // 生成带行号的代码，对每行内容进行HTML转义
  const numberedCode = lines.map((line, index) => {
    // 行号从1开始
    const escapedLine = escapeHtml(line || ' ');
    return `<span class="line-number">${index + 1}</span><span class="line-content">${escapedLine}</span>`;
  }).join('\n');
  
  // 构建具有特定样式的代码块HTML，包含语言标签、行号和复制按钮
  const copyButton = `<div class="copy-button" data-code-id="${codeBlockId}">
    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" aria-hidden="true">
      <path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
    </svg>
  </div>`;
  
  if (lang) {
    return `<pre class="marked-code-block" id="${codeBlockId}"><div class="code-header">
      <div class="code-language-label">${escapeHtml(lang)}</div>
      ${copyButton}
    </div><code class="marked-code language-${lang}">${numberedCode}</code><textarea class="code-copy-textarea">${codeStr}</textarea></pre>`;
  } else {
    return `<pre class="marked-code-block" id="${codeBlockId}"><div class="code-header">
      ${copyButton}
    </div><code class="marked-code">${numberedCode}</code><textarea class="code-copy-textarea">${codeStr}</textarea></pre>`;
  }
};

// 配置 marked 库，禁用高亮功能以避免冲突
marked.setOptions({
  renderer: renderer,
  highlight: null, // 禁用高亮功能
  sanitize: false, // 允许使用HTML标签
  breaks: true, // 允许换行
  gfm: true // 启用GitHub风格的Markdown
});

// 辅助函数：使用 marked 将 Markdown 转换为 HTML
const renderMarkdown = (content) => {
  let markdownText = ''
  
  try {
    // console.log('=== renderMarkdown 调用开始 ===')
    // console.log('content:', content)
    // console.log('content类型:', typeof content)
    // console.log('content.isArray:', Array.isArray(content))
    
    // 处理各种类型的content
    if (typeof content === 'string') {
      // 如果是字符串，检查并预处理JSON格式的代码块
      // console.log('content是字符串，检查并预处理JSON格式的代码块')
      let processedContent = content;
      
      // 使用正则表达式匹配JSON格式的代码块定义
      const jsonCodeBlockRegex = /```json\s*({[^`]+?})\s*```/gs;
      processedContent = processedContent.replace(jsonCodeBlockRegex, (match, jsonContent) => {
        try {
          // 解析JSON内容
          const codeBlockObj = JSON.parse(jsonContent);
          // 如果是代码块对象，使用raw字段的内容
          if (codeBlockObj.type === 'code' && codeBlockObj.raw && typeof codeBlockObj.raw === 'string') {
            // console.log('找到JSON格式的代码块，替换为raw字段内容')
            return codeBlockObj.raw;
          }
        } catch (parseError) {
          // 解析失败，保持原样
          console.error('解析JSON代码块失败:', parseError)
        }
        return match;
      });
      
      // 另外，检查Markdown代码块中是否包含代码块对象
      const codeBlockRegex = /```(\w*)\s*({[^`]+?})\s*```/gs;
      processedContent = processedContent.replace(codeBlockRegex, (match, lang, codeContent) => {
        try {
          // 尝试解析代码内容
          const codeBlockObj = JSON.parse(codeContent);
          // 如果是代码块对象，使用raw字段的内容
          if (codeBlockObj.type === 'code' && codeBlockObj.raw && typeof codeBlockObj.raw === 'string') {
            // console.log('找到代码块中的代码块对象，替换为raw字段内容')
            return codeBlockObj.raw;
          }
        } catch (parseError) {
          // 解析失败，保持原样
        }
        return match;
      });
      
      markdownText = processedContent;
    } else if (typeof content === 'object' && content !== null) {
      // 处理对象类型的content
      if (Array.isArray(content)) {
        // 如果是数组，处理每个元素
        // console.log('content是数组，处理每个元素')
        markdownText = content.map(item => {
          // 对于数组中的每个对象，优先使用raw字段
          if (typeof item === 'object' && item !== null) {
            // console.log('数组元素是对象，检查raw字段')
            if (item.type === 'code' && item.raw && typeof item.raw === 'string') {
              // 如果是代码块对象，直接使用raw字段
              return item.raw;
            } else if (item.text && typeof item.text === 'string') {
              // 如果有text字段，使用text字段
              return item.text;
            } else {
              // 其他情况，转换为字符串
              return JSON.stringify(item);
            }
          } else {
            // console.log('数组元素不是对象，转换为字符串')
            return String(item);
          }
        }).join('\n') // 用换行符连接数组中的每个元素
      } else {
        // 如果是单个对象，检查是否是代码块对象
        // console.log('content是单个对象，检查类型')
        if (content.type === 'code' && content.raw && typeof content.raw === 'string') {
          // 如果是代码块对象，直接使用raw字段
          // console.log('content是代码块对象，使用raw字段:', content.raw)
          markdownText = content.raw;
        } else {
          // 非代码块对象，检查是否有content字段（嵌套内容）
          if (content.content && typeof content.content === 'object') {
            // console.log('content有content字段，处理嵌套内容')
            // 递归处理嵌套的content
            return renderMarkdown(content.content);
          } else if (content.text && typeof content.text === 'string') {
            // 如果有text字段，使用text字段
            // console.log('content有text字段，使用text字段:', content.text)
            markdownText = content.text;
          } else {
            // 最后尝试将对象转换为字符串
            // console.log('content没有可用字段，转换为字符串')
            markdownText = JSON.stringify(content);
          }
        }
      }
    } else {
      // 其他类型，返回空字符串
      // console.log('content是其他类型，返回空字符串')
      markdownText = ''
    }
    
    // console.log('提取的markdownText:', markdownText)
  } catch (error) {
    console.error('Error in renderMarkdown:', error)
    markdownText = ''
  }
  
  // 使用 marked 库将 Markdown 转换为 HTML
  let htmlResult = ''
  try {
    // 调用marked将Markdown转换为HTML
    htmlResult = marked(markdownText)
    // console.log('marked处理后的HTML:', htmlResult)
  } catch (error) {
    console.error('Error in marked:', error)
    htmlResult = ''
  }
  
  // console.log('=== renderMarkdown 调用结束 ===')
  
  return htmlResult
}

// 生成安全ID的辅助函数
let idCounter = 0
const generateSecureId = () => {
  idCounter++
  // 组合：时间戳 + 随机数 + 递增计数器
  return `${Date.now()}-${Math.floor(Math.random() * 1000000)}-${idCounter}`
}

// 状态管理
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const messageInput = ref(null)
// 消息最大长度限制
const MAX_MESSAGE_LENGTH = 100
// 语音输入状态
const isVoiceActive = ref(false)
let recognition = null
// WebSocket状态
// AI思考超时定时器，key为消息id
const thinkingTimers = ref(new Map())
// 倒计时定时器，key为消息id
const countdownTimers = ref(new Map())
// AI思考最大超时时间（秒）- 改为可自定义
const aiThinkingTimeout = ref(60) // 默认60秒
const wsConnected = ref(false)
const wsConnecting = ref(false) // 连接中状态
const wsConnectFailed = ref(false) // 连接失败状态
const wsAutoReconnect = ref(true) // 自动重连开关，默认打开
const wsMaxReconnectAttempts = ref(5) // 重连次数
const wsConnectTimeout = ref(10) // 连接超时时间，单位：秒
const wsReconnectInterval = ref(3) // 重连时间间隔，单位：秒

// AI思考最大超时时间（毫秒）- 计算属性
const MAX_THINKING_TIME = computed(() => aiThinkingTimeout.value * 1000)
let wsInstance = null
let reconnectAttempts = 0 // 当前重连次数
let reconnectTimer = null // 重连定时器
let reachedMaxAttempts = false // 是否已达到最大重连次数限制
let wsInitialized = false // WebSocket是否已手动初始化过
let connectTimeoutTimer = null // 连接超时定时器
let manuallyDisconnected = false // 是否手动断开连接

// 计算当前WebSocket状态
const wsStatus = computed(() => {
  if (wsConnecting.value) {
    return $i('aia.connecting')
  } else if (wsConnected.value) {
    return $i('aia.connected')
  } else if (wsConnectFailed.value) {
    return $i('aia.connectFailed')
  } else if (wsInitialized) {
    return $i('aia.disconnected')
  } else {
    return $i('aia.notConnected')
  }
})

// 计算是否可以操作WebSocket设置（未连接、已断开或连接失败状态）
const canOperateWS = computed(() => {
  // 未连接、已断开或连接失败状态下可以操作
  return !wsConnected.value && !wsConnecting.value
})
// WebSocket连接管理
const initWebSocket = () => {
  // 手动初始化时设置标志
  wsInitialized = true
  
  // 设置连接中状态，重置连接失败状态
  wsConnecting.value = true
  wsConnectFailed.value = false
  
  // 重置标志，但不重置重连计数（重连计数在连接成功或达到最大次数时重置）
  reachedMaxAttempts = false
  
  try {
    // 构建WebSocket URL，与系统保持一致
    let link = `${location.protocol}//${location.host}${location.pathname}`.replace('/index.html', '')
    // 使用系统定义的baseURL
    const baseURL = import.meta.env.MAGIC_API_BASE_URL || './'
    if (baseURL.startsWith('http')) {
      link = baseURL
    } else if (baseURL.startsWith('/')) {
      link = `${location.protocol}//${location.host}${baseURL}`
    }
    // 将http协议替换为ws协议，https替换为wss
    const wsUrl = replaceURL(link.replace(/^http/, 'ws') + '/ws/aia/chat')
    
    // console.log(`WebSocket连接初始化，URL: ${wsUrl}`)
    
    // 使用原生WebSocket API初始化连接
    wsInstance = new WebSocket(wsUrl)
    
    // 设置连接超时定时器
    connectTimeoutTimer = setTimeout(() => {
      // console.log(`WebSocket连接超时(${wsConnectTimeout.value}秒)`)
      // 连接超时，关闭连接
      if (wsInstance) {
        // 使用有效的WebSocket关闭码，1000表示正常关闭
        wsInstance.close(1000, 'Connection Timeout')
      }
      // 设置非连接中状态，标记连接失败
      wsConnecting.value = false
      wsConnectFailed.value = true
    }, wsConnectTimeout.value * 1000)
    
    // 监听连接打开事件
    wsInstance.onopen = () => {
      // console.log('WebSocket连接成功')
      wsConnected.value = true
      wsConnecting.value = false // 连接成功，设置为非连接中状态
      wsConnectFailed.value = false // 连接成功，重置连接失败状态
      reconnectAttempts = 0 // 连接成功，重置重连次数
      reachedMaxAttempts = false // 连接成功，重置最大尝试次数标志
      // 清除重连定时器
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
      // 清除连接超时定时器
      if (connectTimeoutTimer) {
        clearTimeout(connectTimeoutTimer)
        connectTimeoutTimer = null
      }
    }
    
    // 监听连接关闭事件
    wsInstance.onclose = (event) => {
      // console.log(`WebSocket连接关闭: ${event.code} - ${event.reason}`)
      wsConnected.value = false
      wsConnecting.value = false // 连接关闭，设置为非连接中状态
      // 清除连接超时定时器
      if (connectTimeoutTimer) {
        clearTimeout(connectTimeoutTimer)
        connectTimeoutTimer = null
      }
      
      // 检查是否有正在思考的AI消息，如果有，显示超时信息
      const thinkingMessageIndex = messages.value.findIndex(msg => !msg.isUser && msg.isThinking)
      if (thinkingMessageIndex > -1) {
        // 更新消息为超时状态
        messages.value[thinkingMessageIndex] = {
          ...messages.value[thinkingMessageIndex],
          isThinking: false,
          content: { type: 'text', text: $i('aia.connectionTimeout') },
          thoughtProcess: "连接已断开，AI思考过程超时。",
          codeExecution: [
            { file: "WebSocket", status: "timeout" }
          ]
        }
        // 触发事件
        emit('onAIResponse', messages.value[thinkingMessageIndex].content)
        // 滚动到底部
        scrollToBottom()
      }
      
      // 检查是否是手动断开连接
      if (manuallyDisconnected) {
        // console.log('WebSocket手动断开，不自动重连')
        // 手动断开连接，不标记为连接失败
        wsConnectFailed.value = false
        // 重置手动断开标志
        manuallyDisconnected = false
        // 重置状态，以便下次手动连接
        reconnectAttempts = 0
        reachedMaxAttempts = false
        // 清除重连定时器
        if (reconnectTimer) {
          clearTimeout(reconnectTimer)
          reconnectTimer = null
        }
        return
      }
      
      // 非手动断开，标记为连接失败
      wsConnectFailed.value = true
      
      // 只有在WebSocket已手动初始化且开启了自动重连时，才尝试重连
      if (wsInitialized && wsAutoReconnect.value && reconnectAttempts < wsMaxReconnectAttempts.value && !reachedMaxAttempts) {
        reconnectAttempts++
        // console.log(`WebSocket重连尝试 ${reconnectAttempts}/${wsMaxReconnectAttempts.value}`)
        // 延迟重连，使用用户设置的重连间隔
        reconnectTimer = setTimeout(() => {
          initWebSocket()
        }, wsReconnectInterval.value * 1000) // 用户设置的时间后重连
      } else if (!wsAutoReconnect.value) {
        // console.log('WebSocket自动重连已关闭')
        // 自动重连关闭时，重置重连次数和标志，以便下次开启时能重新开始计数
        reconnectAttempts = 0
        reachedMaxAttempts = false
        // 清除可能存在的重连定时器
        if (reconnectTimer) {
          clearTimeout(reconnectTimer)
          reconnectTimer = null
        }
      } else {
        // console.log(`WebSocket重连达到设置次数 ${wsMaxReconnectAttempts.value}，停止尝试`)
        // 达到重连次数后，设置标志，不再尝试重连
        reachedMaxAttempts = true
        // 重置重连次数，以便下次手动连接时能重新开始计数
        reconnectAttempts = 0
        // 清除重连定时器
        if (reconnectTimer) {
          clearTimeout(reconnectTimer)
          reconnectTimer = null
        }
      }
    }
    
    // 监听连接错误事件
    wsInstance.onerror = (error) => {
      console.error('WebSocket连接错误:', error)
      wsConnected.value = false
      wsConnecting.value = false // 连接错误，设置为非连接中状态
      wsConnectFailed.value = true // 连接错误，标记连接失败
      // 清除连接超时定时器
      if (connectTimeoutTimer) {
        clearTimeout(connectTimeoutTimer)
        connectTimeoutTimer = null
      }
    }
    // 监听WebSocket响应
    wsInstance.onmessage = (event) => {
      try {
        // 解析WebSocket消息
        const response = JSON.parse(event.data)
        const messageId = response.id || generateSecureId()
        
        // 检查是否存在相同id的消息
        const existingMessageIndex = messages.value.findIndex(msg => msg.id === messageId)
        
        // 处理响应内容，支持text和code类型
        let processedContent = response.content;
        
        // 深度处理内容，确保代码块对象的raw字段被正确使用
        const processContent = (content) => {
          if (typeof content === 'object' && content !== null) {
            if (Array.isArray(content)) {
              // 如果是数组，处理每个元素
              return content.map(processContent).filter(Boolean);
            } else {
              // 如果是对象，检查类型
              if (content.type === 'code' && content.raw && typeof content.raw === 'string') {
                // 代码块对象，直接返回raw字段内容作为字符串
                // console.log('发现代码块对象，提取raw字段:', content.raw)
                return content.raw;
              } else if (content.type === 'text' && content.text && typeof content.text === 'string') {
                // 文本对象，返回text字段
                return content.text;
              } else if (content.content) {
                // 嵌套内容，递归处理
                return processContent(content.content);
              } else {
                // 其他对象，转换为字符串
                return JSON.stringify(content);
              }
            }
          } else if (typeof content === 'string') {
            // 字符串，直接返回
            return content;
          } else {
            // 其他类型，转换为字符串
            return String(content);
          }
        };
        
        // 处理内容
        processedContent = processContent(response.content);
        
        // 构建消息对象
        const aiMessage = {
          id: messageId,
          content: processedContent,
          isUser: false,
          timestamp: new Date(response.timestamp || Date.now()),
          liked: response.liked || false,
          isThinking: response.isThinking || false
        }
        
        // 处理isThinking状态
        // console.log(`收到WebSocket消息: id=${aiMessage.id}, isThinking=${aiMessage.isThinking}`)
        
        if (response.isThinking) {
          // 检查是否已存在相同id的消息（可能是重试后更新的消息）
          const existingMessageIndex = messages.value.findIndex(msg => msg.id === messageId)
          
          if (existingMessageIndex !== -1) {
            // 已存在相同id的消息，更新它
            // console.log(`更新已存在的AI思考消息: id=${aiMessage.id}`)
            // 更新现有消息为思考状态
            messages.value[existingMessageIndex] = {
              ...messages.value[existingMessageIndex],
              isThinking: true,
              isTimeout: false,
              // 添加timeoutCountdown属性，初始值为最大思考时间（秒）
              timeoutCountdown: Math.floor(MAX_THINKING_TIME.value / 1000)
            }
          } else {
            // 不存在相同id的消息，生成新的AI消息体
            // console.log(`添加新的AI思考消息: id=${aiMessage.id}`)
            // 添加timeoutCountdown属性，初始值为最大思考时间（秒）
            aiMessage.timeoutCountdown = Math.floor(MAX_THINKING_TIME.value / 1000)
            messages.value.push(aiMessage)
          }
          
          // 设置AI思考超时定时器
          const timer = setTimeout(() => {
            // 超时后，更新消息状态
            const msgIndex = messages.value.findIndex(msg => msg.id === aiMessage.id)
            if (msgIndex !== -1) {
              // console.log(`AI思考超时，更新消息: id=${aiMessage.id}`)
              
              // 发送特殊消息：仅包含当前消息id，无content
              if (wsConnected.value && wsInstance) {
                const specialMessage = {
                  id: aiMessage.id
                };
                wsInstance.send(JSON.stringify(specialMessage));
                // console.log(`发送超时前特殊消息: ${JSON.stringify(specialMessage)}`);
              }
              
              // 保存之前的思考内容
              const previousContent = messages.value[msgIndex].content;
              const messageId = messages.value[msgIndex].id;
              
              // 创建超时消息内容，使用安全的HTML生成方式
              const timeoutContent = $i('aia.timeout') + ' <button class="retry-button" title="' + $i('aia.copy') + '"><svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg></button>';
              
              // 超时消息使用新的ID，确保不会被后续WebSocket消息替换
              const timeoutMessage = {
                ...messages.value[msgIndex],
                id: generateSecureId(), // 生成新的唯一ID，避免被后续消息替换
                isThinking: false,
                isTimeout: true, // 标记为超时消息
                content: timeoutContent,
                // 保存之前的思考内容，用于重试
                previousContent: previousContent,
                // 移除倒计时
                timeoutCountdown: undefined
              };
              // 替换原消息为超时消息
              messages.value[msgIndex] = timeoutMessage
              // 清除加载状态
              loading.value = false
            }
            // 清除定时器引用
            thinkingTimers.value.delete(aiMessage.id)
            // 清除倒计时定时器
            if (countdownTimers.value.has(aiMessage.id)) {
              clearInterval(countdownTimers.value.get(aiMessage.id))
              countdownTimers.value.delete(aiMessage.id)
            }
          }, MAX_THINKING_TIME.value)
          
          // 保存定时器引用
          thinkingTimers.value.set(aiMessage.id, timer)
          
          // 设置倒计时定时器，每秒更新一次
          const countdownTimer = setInterval(() => {
            const msgIndex = messages.value.findIndex(msg => msg.id === aiMessage.id)
            if (msgIndex !== -1) {
              const message = messages.value[msgIndex]
              if (message && message.timeoutCountdown > 0) {
                // 每秒减少1秒
                message.timeoutCountdown--
              } else {
                // 倒计时结束，清除定时器
                clearInterval(countdownTimer)
                countdownTimers.value.delete(aiMessage.id)
              }
            } else {
              // 消息不存在，清除定时器
              clearInterval(countdownTimer)
              countdownTimers.value.delete(aiMessage.id)
            }
          }, 1000)
          
          // 保存倒计时定时器引用
          countdownTimers.value.set(aiMessage.id, countdownTimer)
        } else {
          // isThinking为false，处理思考完成的消息，在同一个消息体内追加v-html元素
          if (existingMessageIndex !== -1) {
            // 检查原消息是否已超时或已被替换为超时消息
            const existingMessage = messages.value[existingMessageIndex]
            // console.log(`找到消息，准备更新: id=${aiMessage.id}, existingId=${existingMessage.id}, isTimeout=${existingMessage.isTimeout}, isThinking=${existingMessage.isThinking}`)
            
            // 在同一个消息体内追加v-html元素
            if (existingMessage.isThinking) {
              // 原消息是思考中状态，替换为包含新内容的消息
              // console.log(`更新AI思考中消息: id=${aiMessage.id}`)
              messages.value[existingMessageIndex] = {
                ...aiMessage,
                content: [aiMessage.content], // 将content转换为数组，支持多个v-html元素
                isUser: false // 明确设置为AI消息
              }
            } else {
              // 原消息已经不是思考中状态，将新内容追加到同一个消息体内，形成多个v-html
              // console.log(`在同一个消息体内追加v-html: id=${aiMessage.id}`)
              
              // 确保content是数组类型
              const currentContent = Array.isArray(existingMessage.content) ? existingMessage.content : [existingMessage.content]
              
              // 添加新的content项
              const updatedContent = [...currentContent, aiMessage.content]
              
              // 更新消息
              messages.value[existingMessageIndex] = {
                ...existingMessage,
                content: updatedContent,
                isThinking: false,
                isTimeout: false,
                isUser: false // 明确设置为AI消息
              }
            }
          } else {
            // 没有找到相同id的消息，丢弃这个消息
            // console.log(`未找到消息，丢弃该消息: id=${aiMessage.id}`)
          }
          
          // 清除该消息的超时定时器
          if (thinkingTimers.value.has(aiMessage.id)) {
            // console.log(`清除超时定时器: id=${aiMessage.id}`)
            clearTimeout(thinkingTimers.value.get(aiMessage.id))
            thinkingTimers.value.delete(aiMessage.id)
          }
        }
        
        // 触发事件
        emit('onAIResponse', response.content)
        // 隐藏加载状态
        loading.value = false
        // 滚动到底部
        scrollToBottom()
      } catch (error) {
        console.error('WebSocket消息解析失败:', error)
        // 显示解析失败的错误信息给用户
        messages.value.push({
          id: generateSecureId(),
          content: { type: 'text', text: `WebSocket消息解析失败: ${error.message}` },
          isUser: false,
          timestamp: new Date(),
          liked: false,
          thoughtProcess: "处理WebSocket消息时发生解析错误，需要返回友好的错误提示。",
          codeExecution: [
            { file: "src/components/panel/main/magic-aia.vue", status: "failed" }
          ]
        })
        // 隐藏加载状态
        loading.value = false
        // 滚动到底部
        scrollToBottom()
      }
    }
    
    // console.log('WebSocket连接初始化成功')
  } catch (error) {
    console.error('WebSocket初始化失败:', error)
    wsConnected.value = false
    wsConnecting.value = false // 初始化失败，设置为非连接中状态
    wsConnectFailed.value = true // 初始化失败，标记连接失败
    // 只有在WebSocket已手动初始化且开启了自动重连时，才尝试重连
    if (wsInitialized && wsAutoReconnect.value && reconnectAttempts < wsMaxReconnectAttempts.value && !reachedMaxAttempts) {
      reconnectAttempts++
      // console.log(`WebSocket重连尝试 ${reconnectAttempts}/${wsMaxReconnectAttempts.value}`)
      // 延迟重连，使用用户设置的重连间隔
      reconnectTimer = setTimeout(() => {
        initWebSocket()
      }, wsReconnectInterval.value * 1000) // 用户设置的时间后重连
    } else {
      // console.log(`WebSocket重连达到设置次数 ${wsMaxReconnectAttempts.value}，停止尝试`)
      // 达到重连次数后，设置标志，不再尝试重连
      reachedMaxAttempts = true
      // 重置重连次数，以便下次手动连接时能重新开始计数
      reconnectAttempts = 0
      // 清除重连定时器
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
    }
  }
}

// 确认清理对话 - 移除确认对话框，直接调用清理函数
const confirmClearMessages = () => {
  // 检查AI是否正在思考或加载中
  const isAIIsThinking = messages.value.some(msg => msg.isThinking)
  if (loading.value || isAIIsThinking) {
    // AI正在思考或加载中，不允许清理对话
    alert('AI正在思考中，不能清理对话')
    return
  }
  clearMessages()
}

// 手动断开WebSocket连接
const disconnectWebSocket = () => {
  // 标记为手动断开，防止自动重连
  manuallyDisconnected = true
  // 关闭WebSocket实例
  if (wsInstance) {
    wsInstance.close()
    wsInstance = null
  }
  // 清除重连定时器
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  // 重置状态
  wsConnected.value = false
  wsConnecting.value = false
  reconnectAttempts = 0
  wsConnectFailed.value = false
}

// 监听WebSocket设置项变化，确保在合理范围内
// 监听重连次数变化
watch(() => wsMaxReconnectAttempts.value, (newVal) => {
  if (newVal < 1) {
    wsMaxReconnectAttempts.value = 1
  } else if (newVal > 20) {
    wsMaxReconnectAttempts.value = 20
  }
})

// 监听重连间隔变化
watch(() => wsReconnectInterval.value, (newVal) => {
  if (newVal < 1) {
    wsReconnectInterval.value = 1
  } else if (newVal > 60) {
    wsReconnectInterval.value = 60
  }
})

// 监听连接超时变化
watch(() => wsConnectTimeout.value, (newVal) => {
  if (newVal < 1) {
    wsConnectTimeout.value = 1
  } else if (newVal > 60) {
    wsConnectTimeout.value = 60
  }
})

// 监听AI思考超时时间变化
watch(() => aiThinkingTimeout.value, (newVal) => {
  if (newVal < 10) {
    aiThinkingTimeout.value = 10
  } else if (newVal > 300) {
    aiThinkingTimeout.value = 300
  }
})

// 监听自动连接开关状态变化
watch(() => wsAutoReconnect.value, (newVal) => {
  if (newVal && !wsConnected.value) {
    // 自动连接开关从关闭变为打开，且当前未连接，则尝试连接
    reconnectAttempts = 0
    reachedMaxAttempts = false // 重置最大尝试次数标志
    initWebSocket()
  } else if (!newVal) {
    // 自动连接开关从打开变为关闭，清除重连定时器，停止重连尝试
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    // 自动连接关闭时，重置重连次数和标志，以便下次开启时能重新开始计数
    reconnectAttempts = 0
    reachedMaxAttempts = false
    // console.log('WebSocket自动重连已关闭，停止重连尝试')
  }
})

// 组件挂载时不自动初始化WebSocket，仅通过按钮触发
onMounted(() => {
  // 页面加载时不自动连接WebSocket，仅通过按钮触发
  
  // 添加复制代码按钮的事件监听器
  setTimeout(() => {
    initializeCopyButtons();
  }, 100);
})

// 组件卸载时关闭WebSocket和清理所有定时器
onUnmounted(() => {
  if (wsInstance) {
    wsInstance.close()
    wsInstance = null
  }
  wsConnected.value = false
  
  // 清除重连定时器
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  
  // 清除连接超时定时器
  if (connectTimeoutTimer) {
    clearTimeout(connectTimeoutTimer)
    connectTimeoutTimer = null
  }
  
  // 清除所有思考超时定时器
  for (const [id, timer] of thinkingTimers.value.entries()) {
    clearTimeout(timer)
  }
  thinkingTimers.value.clear()
  
  // 清除所有倒计时定时器
  for (const [id, timer] of countdownTimers.value.entries()) {
    clearInterval(timer)
  }
  countdownTimers.value.clear()
})

// 初始化复制代码按钮和重试按钮
const initializeCopyButtons = () => {
  // 添加点击事件监听器
  document.addEventListener('click', (event) => {
    handleCopyButtonClick(event);
    handleRetryButtonClick(event);
  });
  
  // 移除了未定义的鼠标事件监听器
};

// 处理重试图标点击事件
const handleRetryButtonClick = (event) => {
  if (event.target.closest('.retry-button')) {
    const retryButton = event.target.closest('.retry-button');
    const messageElement = retryButton.closest('.message-wrapper');
    
    if (messageElement) {
      // 查找对应的消息索引
      const messageIndex = Array.from(messageElement.parentElement.children).indexOf(messageElement);
      if (messageIndex !== -1) {
        const message = messages.value[messageIndex];
        if (message && message.previousContent) {
          // 检查WebSocket连接状态，只有已连接才允许重试
          if (wsConnected.value) {
            // 调用重试消息函数
            retryMessage(message.id, message.previousContent);
          } else {
            // WebSocket未连接，提示用户
            // console.log('WebSocket未连接，无法重试消息');
            // 可以添加一个提示，或者禁用重试按钮
            retryButton.style.opacity = '0.5';
            retryButton.style.cursor = 'not-allowed';
            retryButton.title = 'WebSocket未连接，无法重试';
          }
        }
      }
    }
  }
};

// 处理复制按钮点击事件
const handleCopyButtonClick = (event) => {
  if (event.target.closest('.copy-button')) {
    const copyButton = event.target.closest('.copy-button');
    const codeBlockId = copyButton.dataset.codeId;
    copyCodeToClipboard(codeBlockId, copyButton);
  }
};

// 复制代码到剪贴板
const copyCodeToClipboard = (codeBlockId, copyButton) => {
  const codeBlock = document.getElementById(codeBlockId);
  if (!codeBlock) return;
  
  // 获取隐藏的textarea元素，其中包含原始代码
  const textarea = codeBlock.querySelector('.code-copy-textarea');
  if (textarea) {
    // 复制到剪贴板
    textarea.select();
    textarea.setSelectionRange(0, 99999); // 兼容移动端
    
    try {
      document.execCommand('copy');
    } catch (err) {
      console.error('复制失败:', err);
    }
  } else {
    // 备选方案：从代码元素中提取代码
    const codeElement = codeBlock.querySelector('.marked-code');
    if (codeElement) {
      // 获取所有行内容，不包含行号
      const lineContents = codeElement.querySelectorAll('.line-content');
      const codeText = Array.from(lineContents).map(line => line.textContent).join('\n');
      
      // 使用Clipboard API复制
      if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(codeText).catch(err => {
          console.error('复制失败:', err);
        });
      } else {
        // 传统方法复制
        const tempTextarea = document.createElement('textarea');
        tempTextarea.value = codeText;
        tempTextarea.style.position = 'fixed';
        tempTextarea.style.opacity = '0';
        document.body.appendChild(tempTextarea);
        tempTextarea.select();
        tempTextarea.setSelectionRange(0, 99999);
        
        try {
          document.execCommand('copy');
        } catch (err) {
          console.error('复制失败:', err);
        } finally {
          document.body.removeChild(tempTextarea);
        }
      }
    }
  }
};

// 复制用户消息到剪贴板
const copyUserMessage = (message) => {
  let messageText = '';
  
  // 处理消息内容，提取文本
  if (typeof message.content === 'string') {
    messageText = message.content;
  } else if (Array.isArray(message.content)) {
    messageText = message.content.map(contentItem => {
      if (typeof contentItem === 'string') {
        return contentItem;
      } else if (contentItem.text) {
        return contentItem.text;
      } else if (contentItem.raw) {
        return contentItem.raw;
      } else {
        return JSON.stringify(contentItem);
      }
    }).join('\n');
  } else if (message.content.text) {
    messageText = message.content.text;
  } else if (message.content.raw) {
    messageText = message.content.raw;
  } else {
    messageText = JSON.stringify(message.content);
  }
  
  // 使用Clipboard API复制
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(messageText).catch(err => {
      console.error('复制失败:', err);
    });
  } else {
    // 传统方法复制
    const tempTextarea = document.createElement('textarea');
    tempTextarea.value = messageText;
    tempTextarea.style.position = 'fixed';
    tempTextarea.style.opacity = '0';
    document.body.appendChild(tempTextarea);
    tempTextarea.select();
    tempTextarea.setSelectionRange(0, 99999);
    
    try {
      document.execCommand('copy');
    } catch (err) {
      console.error('复制失败:', err);
    } finally {
      document.body.removeChild(tempTextarea);
    }
  }
};


// 初始化语音识别
const initSpeechRecognition = () => {
  // 检查浏览器是否支持语音识别
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    console.error('浏览器不支持语音识别API')
    return null
  }

  const rec = new SpeechRecognition()
  rec.continuous = false
  rec.interimResults = false
  rec.lang = 'zh-CN' // 设置为中文

  // 语音识别结果事件
  rec.onresult = (event) => {
    const transcript = event.results[0][0].transcript
    inputMessage.value += transcript
    adjustInputHeight()
  }

  // 语音识别结束事件
  rec.onend = () => {
    isVoiceActive.value = false
  }

  // 语音识别错误事件
  rec.onerror = (event) => {
    console.error('语音识别错误:', event.error)
    isVoiceActive.value = false
  }

  return rec
}

// 切换语音输入状态
const toggleVoiceInput = () => {
  if (!recognition) {
    recognition = initSpeechRecognition()
    if (!recognition) {
      alert('您的浏览器不支持语音识别功能')
      return
    }
  }

  // 检查当前状态，避免重复调用
  try {
    if (isVoiceActive.value) {
      // 停止语音识别
      if (recognition.state === 'running') {
        recognition.stop()
      }
      isVoiceActive.value = false
    } else {
      // 开始语音识别
      if (recognition.state !== 'running') {
        recognition.start()
        isVoiceActive.value = true
      }
    }
  } catch (error) {
    console.error('语音识别状态切换失败:', error)
    // 错误处理：确保UI状态与实际状态一致
    if (error.name === 'InvalidStateError') {
      // 如果已经启动，直接设置状态为true
      isVoiceActive.value = recognition.state === 'running'
    } else {
      // 其他错误，重置状态
      isVoiceActive.value = false
    }
  }
}

// 重试消息函数
const retryMessage = (messageId, previousContent) => {
  try {
    // 查找超时消息的索引
    const messageIndex = messages.value.findIndex(msg => msg.id === messageId);
    
    if (messageIndex !== -1) {
      // 直接使用previousContent对象，不需要解析
      const parsedContent = previousContent;
      const aiMessage = {
        ...messages.value[messageIndex],
        isThinking: true,
        isTimeout: false,
        content: parsedContent,
        // 添加timeoutCountdown属性，初始值为最大思考时间（秒）
        timeoutCountdown: Math.floor(MAX_THINKING_TIME / 1000)
      };
      
      // 更新消息
      messages.value[messageIndex] = aiMessage;
      
      // 发送包含之前思考内容和id的消息，不显示多余的加载状态
      if (wsConnected.value && wsInstance) {
        const message = {
          id: messageId,
          content: parsedContent
        };
        wsInstance.send(JSON.stringify(message));
      } else {
        // WebSocket未连接，使用HTTP API作为备选
        console.error('WebSocket未连接');
      }
      
      // 清除可能存在的旧定时器
      if (thinkingTimers.value.has(messageId)) {
        clearTimeout(thinkingTimers.value.get(messageId));
        thinkingTimers.value.delete(messageId);
      }
      
      // 设置AI思考超时定时器
      const timer = setTimeout(() => {
        // 超时后，更新消息状态
        const msgIndex = messages.value.findIndex(msg => msg.id === messageId);
        if (msgIndex !== -1) {
          // console.log(`AI思考超时，更新消息: id=${messageId}`);
          
          // 发送特殊消息：仅包含当前消息id，无content
          if (wsConnected.value && wsInstance) {
            const specialMessage = {
              id: messageId
            };
            wsInstance.send(JSON.stringify(specialMessage));
            // console.log(`发送超时前特殊消息: ${JSON.stringify(specialMessage)}`);
          }
          
          // 保存之前的思考内容
          const previousContent = messages.value[msgIndex].content;
          
          // 创建超时消息内容，使用安全的HTML生成方式
          const timeoutContent = $i('aia.timeout') + ' <button class="retry-button" title="' + $i('aia.copy') + '"><svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/></svg></button>';
          
          // 更新消息为超时状态，生成新的ID
          const timeoutMessage = {
            ...messages.value[msgIndex],
            id: generateSecureId(), // 生成新的唯一ID，避免被后续消息替换
            isThinking: false,
            isTimeout: true, // 标记为超时消息
            content: timeoutContent,
            // 保存之前的思考内容，用于重试
            previousContent: previousContent,
            // 移除倒计时
            timeoutCountdown: undefined
          };
          // 替换原消息为超时消息
          messages.value[msgIndex] = timeoutMessage;
          // 清除加载状态
          loading.value = false;
        }
        // 清除定时器引用
        thinkingTimers.value.delete(messageId);
        // 清除倒计时定时器
        if (countdownTimers.value.has(messageId)) {
          clearInterval(countdownTimers.value.get(messageId));
          countdownTimers.value.delete(messageId);
        }
      }, MAX_THINKING_TIME);
      
      // 保存定时器引用
      thinkingTimers.value.set(messageId, timer);
      
      // 清除可能存在的旧倒计时定时器
      if (countdownTimers.value.has(messageId)) {
        clearInterval(countdownTimers.value.get(messageId));
        countdownTimers.value.delete(messageId);
      }
      
      // 设置新的倒计时定时器，每秒更新一次
      const countdownTimer = setInterval(() => {
        const msgIndex = messages.value.findIndex(msg => msg.id === messageId);
        if (msgIndex !== -1) {
          const message = messages.value[msgIndex];
          if (message && message.timeoutCountdown > 0) {
            // 每秒减少1秒
            message.timeoutCountdown--;
          } else {
            // 倒计时结束，清除定时器
            clearInterval(countdownTimer);
            countdownTimers.value.delete(messageId);
          }
        } else {
          // 消息不存在，清除定时器
          clearInterval(countdownTimer);
          countdownTimers.value.delete(messageId);
        }
      }, 1000);
      
      // 保存倒计时定时器引用
      countdownTimers.value.set(messageId, countdownTimer);
    }
  } catch (error) {
    console.error('重试消息失败:', error);
  }
};

// 发送消息
  const sendMessage = async () => {
    const content = inputMessage.value ? inputMessage.value.trim() : ''
    if (!content || loading.value) return

    // 添加用户消息
    const userMessage = {
      id: generateSecureId(),
      content: { type: 'text', text: content },
      isUser: true,
      timestamp: new Date(),
      liked: false
    }
    messages.value.push(userMessage)
    inputMessage.value = ''

    // 滚动到底部
    scrollToBottom()

    // 显示加载状态
    loading.value = true

    try {
      if (wsConnected.value && wsInstance) {
        // 使用WebSocket发送消息
        const message = {
          content: content
        }
        wsInstance.send(JSON.stringify(message))
      } else {
        // WebSocket未连接，使用HTTP API作为备选
        throw new Error('WebSocket未连接')
      }
    } catch (error) {
      console.error('WebSocket发送失败，尝试使用HTTP API:', error)
      // WebSocket发送失败，尝试使用HTTP API
      try {
        const response = await fetch('/magic-api/aia/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ content })
        })
        
        if (!response.ok) {
          throw new Error('API请求失败')
        }
        
        const aiResponse = await response.json()
        
        // 添加AI消息，确保content是JSON格式，只支持text类型
        const formattedResponse = {
          ...aiResponse,
          content: typeof aiResponse.content === 'string' ? 
            // 如果返回的是字符串，直接处理为text类型
            { type: 'text', text: aiResponse.content } :
          // 如果已经是JSON对象，确保type为text
          typeof aiResponse.content === 'object' && aiResponse.content !== null ?
            { ...aiResponse.content, type: 'text' } :
          // 其他类型，转换为text类型
          { type: 'text', text: JSON.stringify(aiResponse.content) }
        }
        messages.value.push(formattedResponse)
        
        // 触发事件
        emit('onAIResponse', formattedResponse.content)
      } catch (httpError) {
        console.error('AI响应错误:', httpError)
        messages.value.push({
          id: generateSecureId(),
          content: { type: 'text', text: $i('aia.error') },
          isUser: false,
          timestamp: new Date(),
          liked: false,
          thoughtProcess: "处理用户请求时发生错误，需要返回友好的错误提示。",
          codeExecution: [
            { file: "src/services/aiService.js", status: "failed" }
          ]
        })
      } finally {
        loading.value = false
        scrollToBottom()
      }
    }
  }

// 生成示例响应
const generateExampleResponse = () => {
  // 返回包含content、thoughtProcess和codeExecution的对象，只支持text类型
  return {
    content: { type: 'text', text: '这是一个示例响应，展示AI助手的功能。\n\n例如，你可以询问：\n- 如何使用magic-api？\n- 如何编写API接口？\n- 如何调试API？\n\n我会为你提供详细的解答和帮助。' },
    thoughtProcess: "用户的问题比较宽泛，我需要提供一个简单的示例来展示AI助手的功能。",
    codeExecution: []
  }
}


// 监听消息变化，滚动到底部
watch(() => messages.value.length, () => {
  scrollToBottom()
})

// 监听WebSocket连接状态变化，更新重试按钮状态
watch(() => wsConnected.value, (newValue) => {
  // 更新所有重试按钮的状态
  nextTick(() => {
    const retryButtons = document.querySelectorAll('.retry-button')
    retryButtons.forEach(button => {
      if (newValue) {
        // 已连接，启用重试按钮
        button.style.opacity = '1';
        button.style.cursor = 'pointer';
        button.title = $i('aia.retry');
      } else {
        // 未连接，禁用重试按钮
        button.style.opacity = '0.5';
        button.style.cursor = 'not-allowed';
        button.title = 'WebSocket未连接，无法重试';
      }
    })
  })
})

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 格式化时间
const formatTime = (date) => {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 清空输入框内容
const clearInput = () => {
  // console.log('clearInput函数被调用')
  // 确保inputMessage是响应式更新
  inputMessage.value = ''
  // 直接调用adjustInputHeight
  adjustInputHeight()
}

// 清理对话消息
const clearMessages = () => {
  // 清除所有思考超时定时器
  thinkingTimers.value.forEach(timer => clearTimeout(timer))
  thinkingTimers.value.clear()
  
  // 直接清空本地消息，不调用API
  messages.value = []
  // 清空输入框
  inputMessage.value = ''
  // 滚动到底部
  scrollToBottom()
}















// 移除了代码块相关功能，只支持text类型

// 自动调整输入框高度
const adjustInputHeight = () => {
  const input = messageInput.value
if (input) {
    // 重置高度以获得真实的滚动高度
    input.style.height = 'auto'
    // 设置新高度，限制最大高度
    input.style.height = Math.min(input.scrollHeight, 120) + 'px'
  }
}

// 监听键盘事件
const handleKeyDown = (event) => {
  // 语音输入快捷键：Ctrl+Shift+V
  if (event.ctrlKey && event.shiftKey && event.key === 'V') {
    event.preventDefault()
    toggleVoiceInput()
    return
  }
  
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  } else {
    // 输入时自动调整高度
    setTimeout(adjustInputHeight, 0)
  }
}

// 监听输入变化，调整高度
watch(() => inputMessage.value, () => {
  // 使用nextTick确保DOM已经更新，messageInput已经被正确初始化
  nextTick(() => {
    adjustInputHeight()
  })
})

defineExpose({
  sendMessage
})
</script>

<style scoped>
/* 主容器 */
.magic-aia-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--main-background-color);
  border-radius: 0;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}

/* 内容区域 */
.magic-aia-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--content-background-color);
}

/* 欢迎信息 */
.welcome-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px;
  text-align: center;
  color: var(--text-color);
  background-color: var(--main-background-color);
  width: 100%;
}

.welcome-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.welcome-header > * {
  margin-right: 12px;
}

.welcome-header > *:last-child {
  margin-right: 0;
}

.welcome-robot {
  font-size: 32px;
}

.welcome-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-color);
}

.welcome-description {
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-color);
  margin-bottom: 8px;
  max-width: 100%;
  width: 100%;
}

/* 聊天消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
  position: relative;
  background-color: var(--content-background-color);
  display: flex;
  flex-direction: column;
}

.chat-messages > .message-wrapper {
  margin-bottom: 4px;
}

.chat-messages > .message-wrapper:last-child {
  margin-bottom: 0;
}

/* 消息包装器 */
.message-wrapper {
  display: flex;
  align-items: flex-start;
  margin-bottom: 3px;
  width: 100%;
}

.message-wrapper > .message-avatar {
  margin-right: 3px;
}

.message-wrapper.ai-message {
  justify-content: flex-start;
  flex-direction: row;
  /* 默认顶部对齐，仅在思考和超时状态下居中 */
  align-items: flex-start;
}

/* 思考中和超时状态的AI消息居中对齐 */
.message-wrapper.ai-message:has(.thinking-indicator),
.message-wrapper.ai-message:has(.message.isTimeout) {
  align-items: center;
}

.message-wrapper.user-message {
  justify-content: flex-end;
  flex-direction: row;
  align-items: center;
}

/* 用户消息样式 */
.message-wrapper.user-message .message {
  max-width: 90%;
  margin-right: 0;
}

/* 用户消息复制按钮样式 */
.user-message-copy-btn {
  position: relative;
  display: none;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background-color: transparent;
  border: none;
  color: var(--main-color);
  cursor: pointer;
  border-radius: 3px;
  transition: all 0.3s ease;
  flex-shrink: 0;
  margin-right: 2px;
  vertical-align: middle;
}

/* 鼠标悬停在用户消息上时显示复制按钮 */
.message-wrapper.user-message:hover .user-message-copy-btn {
  display: flex;
}

/* 复制按钮悬停样式 */
.user-message-copy-btn:hover {
  background-color: var(--main-hover-background-color);
  color: var(--main-color-hover);
}

/* 复制按钮图标样式 */
.user-message-copy-btn svg {
  width: 12px;
  height: 12px;
  fill: currentColor;
}

/* 用户消息元素样式 */
.message-wrapper.user-message .user-message-copy-btn {
  margin-right: 2px;
  margin-left: 0;
}

.message-wrapper.user-message .message {
  margin-right: 0;
}

/* 思考状态指示器样式 */
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  color: var(--text-color);
  font-size: 12px;
}

/* 思考中文字样式 */
.thinking-text {
  color: var(--text-color);
  font-size: 12px;
}

/* 思考中点样式 */
.thinking-dots {
  display: flex;
  align-items: center;
}

.thinking-dots span {
  margin-right: 4px;
}

.thinking-dots span:last-child {
  margin-right: 0;
}

.thinking-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--text-color);
  opacity: 0.5;
  animation: thinking-dot 1.4s infinite ease-in-out both;
}

.thinking-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.thinking-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes thinking-dot {
  0%, 80%, 100% {
    opacity: 0.5;
    transform: scale(1);
  }
  40% {
    opacity: 1;
    transform: scale(1.2);
  }
}

/* 思考倒计时样式 */
.thinking-timeout {
  color: var(--text-color);
  font-size: 12px;
  margin-left: auto;
  opacity: 0.8;
}

.message-wrapper.user-message .message-avatar {
  margin-left: 2px;
  margin-right: 0;
}

/* AI消息布局保持不变 */
.message-wrapper.ai-message {
  justify-content: flex-start;
  flex-direction: row;
}

.message-wrapper.user-message .message-avatar {
  margin-right: 0;
  margin-left: 2px;
}

/* 消息头像 */
.message-avatar {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ai-avatar-background);
  color: var(--ai-avatar-text-color);
  font-size: 14px;
  box-shadow: 0 2px 6px rgba(0, 123, 255, 0.4);
  border: 2px solid #007bff;
  transition: all 0.3s ease;
}

.message-avatar:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.6);
}

.message-wrapper.user-message .message-avatar {
  background: var(--user-avatar-background);
  border-color: #28a745;
  box-shadow: 0 2px 6px rgba(40, 167, 69, 0.4);
}

.message-wrapper.user-message .message-avatar:hover {
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.6);
}

/* 消息内容 */
.message {
  max-width: 90%;
  padding: 4px 8px; /* 减少内边距，更紧凑 */
  border-radius: 6px; /* 减小边框圆角，更紧凑 */
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); /* 减小阴影，更紧凑 */
  border: 1px solid var(--main-border-color); /* 减小边框宽度，更紧凑 */
  background: var(--message-background-color);
  color: var(--message-text-color);
  font-family: inherit;
  font-size: 10px; /* 减小字体大小，更紧凑 */
  line-height: 1.2; /* 减小行高，更紧凑 */
  font-weight: normal;
  margin: 2px 0; /* 减少外边距，更紧凑 */
}

.message-wrapper.ai-message .message {
  align-self: flex-start;
  max-width: 90%;
  width: 90%;
}

.message-wrapper.user-message .message {
  align-self: flex-end;
  max-width: 90%;
  word-break: break-word;
  overflow-wrap: break-word;
  background: var(--user-message-background-color);
  color: var(--user-message-text-color);
  border-bottom-right-radius: 4px;
  border-top-right-radius: 8px;
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}

.message-wrapper.ai-message .message {
  border: none; /* 移除边框，与用户消息保持一致 */
  border-bottom-left-radius: 4px;
  border-top-left-radius: 10px;
  border-top-right-radius: 10px;
  border-bottom-right-radius: 10px;
}

/* 消息头部 */
.message-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 2px; /* 减少底部外边距，更紧凑 */
  font-size: 10px; /* 减小字体大小，更紧凑 */
  opacity: 0.8;
}

.message-wrapper.user-message .message-header {
  justify-content: flex-end;
}

.time {
  font-size: 9px; /* 减小字体大小，更紧凑 */
  opacity: 0.9;
  color: var(--time-text-color, #fff);
  background: var(--time-background-color, #007bff);
  padding: 2px 6px; /* 减少内边距，更紧凑 */
  border-radius: 4px; /* 减小边框圆角，更紧凑 */
  font-weight: 500; /* 减小字体粗细，更紧凑 */
  box-shadow: 0 1px 3px rgba(var(--time-background-color-rgb, 0, 123, 255), 0.3); /* 减小阴影，更紧凑 */
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

/* 消息内容 */
.message-content {
  line-height: 1.2; /* 减小行高，更紧凑 */
  font-size: 10px; /* 减小字体大小，更紧凑 */
  margin: 1px 0; /* 减少外边距，更紧凑 */
}

/* 文本内容样式 - v-html 基础样式 */
.text-content {
  line-height: 1.3; /* 减小行高，更紧凑 */
  width: 100%;
  white-space: normal;
  color: inherit;
  font-size: 10px; /* 减小字体大小，更紧凑 */
}

/* v-html元素之间的横线分隔符 */
.v-html-divider {
  height: 1px;
  background-color: var(--main-border-color);
  margin: 6px 0;
  opacity: 0.6;
  width: 100%;
}

/* Markdown代码块样式增强，使其更突出 */
/* 行内代码样式 */
:deep(.text-content) code {
  padding: 3px 6px !important;
  font-size: 0.9em !important;
  color: var(--main-color) !important;
  border: 1px solid var(--main-border-color) !important;
  display: inline-block !important;
  background-color: var(--main-hover-background-color) !important;
  border-radius: 4px !important;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1) !important;
  white-space: nowrap !important; /* 行内代码不换行 */
}

/* 代码块容器样式 - 确保有明显的视觉效果 */
:deep(.text-content) pre.marked-code-block {
  display: block !important;
  width: 100% !important;
  padding: 4px 4px !important; /* 进一步减少内边距，更紧凑 */
  margin: 6px 0 !important; /* 进一步减少外边距，更紧凑 */
  background-color: var(--code-background-color, var(--main-background-color)) !important;
  color: var(--code-text-color, var(--text-color)) !important;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  font-size: 10px !important; /* 进一步减小字体大小，更紧凑 */
  line-height: 1.0 !important; /* 进一步减小行高，更紧凑 */
  border: 1px solid var(--code-border-color, var(--main-border-color)) !important;
  border-radius: 4px !important; /* 进一步减小边框圆角，更紧凑 */
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1) !important; /* 进一步减小阴影，更紧凑 */
  overflow-x: auto !important; /* 水平滚动 */
  white-space: normal !important; /* 允许换行 */
  position: relative !important;
  box-sizing: border-box !important;
  /* 确保没有其他样式影响行间距 */
  border-spacing: 0 !important;
  border-collapse: collapse !important;
}

/* 代码头容器样式 */
:deep(.text-content) pre.marked-code-block .code-header {
  display: flex !important;
  justify-content: space-between !important;
  align-items: center !important;
  width: 100% !important;
  padding: 2px 4px !important; /* 进一步减少内边距，更紧凑 */
  background-color: var(--code-header-background-color, var(--main-hover-background-color)) !important;
  border-bottom: 1px solid var(--code-border-color, var(--main-border-color)) !important;
  margin-bottom: 1px !important; /* 进一步减少底部外边距，更紧凑 */
  margin-top: 0 !important;
  margin-left: 0 !important;
  margin-right: 0 !important;
  box-sizing: border-box !important;
  height: auto !important; /* 自动高度，更紧凑 */
  min-height: auto !important; /* 最小高度自动，更紧凑 */
}

/* 代码语言标签样式 - 增强视觉突出效果 */
:deep(.text-content) pre.marked-code-block .code-language-label {
  display: inline-block !important;
  background-color: var(--code-language-background-color, var(--main-color)) !important;
  color: var(--code-language-text-color, white) !important;
  padding: 2px 6px !important;
  font-size: 9px !important;
  font-weight: bold !important;
  border-radius: 4px !important;
  margin-bottom: 0 !important;
  margin-top: 0 !important;
  margin-left: 0 !important;
  margin-right: 8px !important;
  text-transform: uppercase !important;
  letter-spacing: 0.5px !important;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  box-shadow: 0 1px 3px rgba(var(--code-language-background-color-rgb, 0, 0, 0), 0.2) !important;
  border: 1px solid var(--code-language-border-color, var(--main-border-color)) !important;
  transition: all 0.3s ease !important;
}

/* 复制按钮样式 */
:deep(.text-content) pre.marked-code-block .copy-button {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 20px !important; /* 进一步减小宽度，更紧凑 */
  height: 18px !important; /* 进一步减小高度，更紧凑 */
  background-color: transparent !important;
  border: 1px solid transparent !important;
  border-radius: 2px !important; /* 进一步减小边框圆角，更紧凑 */
  cursor: pointer !important;
  transition: all 0.3s ease !important;
  color: var(--copy-button-color, var(--main-color)) !important;
  position: relative !important;
  overflow: visible !important;
  padding: 0 !important;
  margin: 0 !important;
}

/* 复制按钮悬停样式 */
:deep(.text-content) pre.marked-code-block .copy-button:hover {
  background-color: var(--copy-button-hover-background, var(--main-hover-background-color)) !important;
  color: var(--copy-button-hover-color, var(--main-color)) !important;
  border-color: var(--copy-button-hover-border, var(--main-border-color)) !important;
}

/* 复制按钮点击样式 */
:deep(.text-content) pre.marked-code-block .copy-button:active {
  background-color: var(--copy-button-active-background, var(--main-background-color)) !important;
  transform: translateY(1px) !important;
}

/* 重试按钮样式 - 使用:deep()确保样式生效 */
:deep(.retry-button) {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  background-color: transparent !important;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  color: var(--main-color) !important;
  font-size: 10px !important;
  padding: 4px !important;
  border-radius: 4px !important;
  cursor: pointer !important;
  transition: all 0.3s ease !important;
  margin-left: 4px !important;
  font-weight: normal !important;
  font-family: inherit !important;
  vertical-align: middle !important;
  width: 24px !important;
  height: 24px !important;
}

:deep(.retry-button:hover) {
  background-color: var(--main-hover-background-color) !important;
  color: var(--main-color-hover) !important;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
}

:deep(.retry-button:active) {
  transform: translateY(1px) !important;
  background-color: var(--main-background-color) !important;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
}

:deep(.retry-button svg) {
  width: 14px !important;
  height: 14px !important;
  fill: currentColor !important;
  margin: 0 !important;
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
}

/* 复制按钮图标样式 */
:deep(.text-content) pre.marked-code-block .copy-button svg {
  width: 12px !important; /* 进一步减小图标大小，更紧凑 */
  height: 12px !important; /* 进一步减小图标大小，更紧凑 */
  fill: currentColor !important;
  display: block !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* 隐藏的代码复制textarea样式 */
:deep(.text-content) pre.marked-code-block .code-copy-textarea {
  position: absolute !important;
  top: -9999px !important;
  left: -9999px !important;
  width: 1px !important;
  height: 1px !important;
  opacity: 0 !important;
  visibility: hidden !important;
  overflow: hidden !important;
  z-index: -1 !important;
  pointer-events: none !important;
  user-select: none !important;
}

/* 代码块内的代码样式 */
:deep(.text-content) pre.marked-code-block code.marked-code {
  display: block !important;
  width: 100% !important;
  padding: 0 !important;
  margin: 0 !important;
  background-color: transparent !important;
  border: none !important;
  color: var(--code-text-color, var(--text-color)) !important;
  font-family: inherit !important;
  font-size: inherit !important;
  line-height: 1.0 !important; /* 保持紧凑的行高 */
  box-shadow: none !important;
  white-space: pre-wrap !important; /* 保留换行，允许自动换行 */
  word-wrap: break-word !important; /* 长单词自动换行 */
  overflow-wrap: break-word !important; /* 长单词自动换行 */
  position: relative !important;
  box-sizing: border-box !important;
  overflow: visible !important;
  text-indent: 0 !important;
  /* 确保没有其他样式影响行间距 */
  border: none !important;
  outline: none !important;
}

/* 行号样式 */
:deep(.text-content) pre.marked-code-block .line-number {
  display: inline-block !important;
  width: 40px !important; /* 进一步减小宽度，更紧凑 */
  padding: 0 8px 0 6px !important; /* 进一步减小内边距，更紧凑 */
  margin: 0 !important;
  background-color: var(--line-number-background-color, var(--main-hover-background-color)) !important;
  color: var(--line-number-color, var(--main-color)) !important;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  font-size: 9px !important; /* 进一步减小字体大小，更紧凑 */
  line-height: 1.0 !important; /* 保持紧凑的行高 */
  text-align: right !important;
  user-select: none !important;
  border-right: 1px solid var(--line-number-border-color, var(--main-border-color)) !important;
  margin-right: 4px !important; /* 进一步减小右边距，更紧凑 */
  transition: background-color 0.3s ease !important;
  box-sizing: border-box !important; /* 确保宽度包含内边距和边框 */
  min-width: 40px !important; /* 进一步减小最小宽度，更紧凑 */
  white-space: nowrap !important; /* 确保行号不换行 */
  overflow: visible !important;
  vertical-align: top !important;
  display: inline !important; /* 改为inline，确保行号和内容在同一行 */
  float: none !important; /* 取消浮动，避免影响行间距 */
  clear: none !important; /* 取消清除浮动，避免影响行间距 */
  height: auto !important; /* 自动高度，避免影响行间距 */
  /* 确保没有其他样式影响行间距 */
  border: none !important;
  outline: none !important;
}

/* 行内容样式 */
:deep(.text-content) pre.marked-code-block .line-content {
  display: inline !important; /* 改为inline，确保行号和内容在同一行 */
  padding: 0 !important;
  margin: 0 !important;
  font-family: inherit !important;
  font-size: inherit !important;
  line-height: 1.0 !important; /* 进一步减小行高，更紧凑 */
  background-color: transparent !important;
  color: inherit !important;
  overflow: visible !important;
  white-space: pre-wrap !important;
  word-wrap: break-word !important;
  overflow-wrap: break-word !important;
  margin-left: 0 !important; /* 取消左边距，避免影响行间距 */
  height: auto !important; /* 自动高度，避免影响行间距 */
  box-sizing: border-box !important;
  text-indent: 0 !important;
  /* 确保没有其他样式影响行间距 */
  border: none !important;
  outline: none !important;
}

/* 鼠标悬停时行号背景色变化 */
:deep(.text-content) pre.marked-code-block:hover .line-number {
  background-color: var(--line-number-hover-background, var(--main-background-color)) !important;
}

/* 选中代码时行号样式保持不变 */
:deep(.text-content) pre.marked-code-block code.marked-code::selection {
  background-color: var(--code-selection-background, rgba(66, 153, 225, 0.3)) !important;
}

:deep(.text-content) pre.marked-code-block .line-number::selection {
  background-color: transparent !important;
}

/* 确保所有pre元素都有正确的样式 */
:deep(.text-content) pre {
  display: block !important;
  white-space: pre-wrap !important; /* 保留换行，允许自动换行 */
  word-wrap: break-word !important; /* 长单词自动换行 */
  overflow-x: auto !important;
}

/* 确保所有code元素都有正确的样式 */
:deep(.text-content) code {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
}

/* 文本消息样式 */
.text-message {
  white-space: pre-wrap;
  line-height: 1.6;
}

/* AI响应内容 */
.ai-response {
  white-space: pre-wrap;
  width: 100%;
  margin: 2px 0;
}

.message-content-wrapper {
  width: 100%;
}

/* 代码块样式 */
.code-part {
  width: 100%;
  margin: 12px 0;
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  overflow: hidden;
  background-color: var(--main-background-color);
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background-color: var(--main-hover-background-color);
  border-bottom: 1px solid var(--main-border-color);
  font-size: 12px;
  color: var(--main-color);
  font-weight: bold;
}

.language {
  font-weight: bold;
  font-family: 'JetBrains Mono', monospace;
}

.copy-container {
  display: flex;
  align-items: center;
  gap: 4px;
}

.copy-btn {
  background: var(--main-hover-background-color);
  border: 1px solid var(--main-border-color);
  border-radius: 3px;
  padding: 2px 8px;
  font-size: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: var(--main-color);
}

.copy-btn:hover {
  background: var(--main-color);
  color: var(--main-background-color);
  border-color: var(--main-color);
}

.copy-success-tooltip {
  font-size: 10px;
  color: var(--success-color);
  opacity: 0.8;
  transition: opacity 0.3s ease;
}

/* 输入区域样式 */
.input-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 6px;
  background: var(--main-background-color);
  border-top: 1px solid var(--main-border-color);
}

.input-container {
  display: flex;
  align-items: flex-start;
  width: 100%;
}

.input-container > .input-wrapper {
  margin-right: 12px;
}

.input-container > .input-wrapper:last-child {
  margin-right: 0;
}

.input-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  position: relative;
  width: 100%;
}

.message-input {
  flex: 1;
  border: 1px solid var(--main-border-color);
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.4;
  resize: none;
  min-height: 24px;
  max-height: 120px;
  overflow-y: auto;
  transition: all 0.3s ease;
  background: var(--main-background-color);
  color: var(--main-color);
  font-family: inherit;
}

.message-input:focus {
  outline: none;
  border-color: var(--main-color);
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.input-hints {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 10px;
  color: var(--secondary-color);
  line-height: 1.4;
}

.input-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.clear-btn,
.connect-ws-btn {
  background: var(--main-hover-background-color);
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: var(--main-color);
  white-space: nowrap;
}

.clear-btn:hover,
.connect-ws-btn:hover {
  background: var(--main-color);
  color: var(--main-background-color);
  border-color: var(--main-color);
}

.clear-btn:disabled,
.connect-ws-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--main-hover-background-color);
  color: var(--secondary-color);
  border-color: var(--main-border-color);
}

.connect-ws-btn.connected {
  background: var(--success-color);
  color: var(--main-background-color);
  border-color: var(--success-color);
}

.ws-separator {
  width: 1px;
  height: 12px;
  background: var(--main-border-color);
  margin: 0 6px;
}

.ws-settings {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 10px;
}

.ws-setting-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.ws-setting-label {
  color: var(--secondary-color);
  white-space: nowrap;
}

.ws-setting-switch {
  position: relative;
  display: inline-block;
  width: 24px;
  height: 12px;
}

.ws-setting-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 12px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 10px;
  width: 10px;
  left: 1px;
  bottom: 1px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: var(--main-color);
}

input:focus + .slider {
  box-shadow: 0 0 1px var(--main-color);
}

input:checked + .slider:before {
  transform: translateX(12px);
}

.ws-setting-input {
  width: 40px;
  padding: 2px 4px;
  border: 1px solid var(--main-border-color);
  border-radius: 3px;
  font-size: 10px;
  background: var(--main-background-color);
  color: var(--main-color);
}

.ws-status-value {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: bold;
  color: var(--secondary-color);
}

.ws-status-connecting {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.ws-status-connected {
  background: rgba(40, 167, 69, 0.2);
  color: #28a745;
}

.ws-status-failed {
  background: rgba(220, 53, 69, 0.2);
  color: #dc3545;
}

.ws-status-disconnected {
  background: rgba(108, 117, 125, 0.2);
  color: #6c757d;
}

.ws-status-not-connected {
  background: rgba(108, 117, 125, 0.2);
  color: #6c757d;
}

.input-stats {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 10px;
  color: var(--secondary-color);
  flex-wrap: wrap;
}

.hint-text {
  flex: 1;
  min-width: 200px;
  text-align: left;
}

.character-count {
  white-space: nowrap;
}

.near-limit {
  color: var(--warning-color);
  font-weight: bold;
}

.voice-btn,
.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--main-border-color);
  border-radius: 6px;
  background: var(--main-hover-background-color);
  color: var(--main-color);
  cursor: pointer;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.voice-btn:hover,
.send-btn:hover {
  background: var(--main-color);
  color: var(--main-background-color);
  border-color: var(--main-color);
}

.voice-btn:disabled,
.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--main-hover-background-color);
  color: var(--secondary-color);
  border-color: var(--main-border-color);
}

.voice-btn.active {
  background: var(--success-color);
  color: var(--main-background-color);
  border-color: var(--success-color);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(40, 167, 69, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(40, 167, 69, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(40, 167, 69, 0);
  }
}

/* 自定义确认对话框样式 */
.confirm-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.confirm-dialog {
  background: var(--main-background-color);
  border: 1px solid var(--main-border-color);
  border-radius: 6px;
  padding: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
  min-width: 240px;
}

.confirm-dialog-title {
  font-weight: bold;
  font-size: 14px;
  margin-bottom: 8px;
  color: var(--main-color);
}

.confirm-dialog-content {
  font-size: 12px;
  margin-bottom: 12px;
  color: var(--secondary-color);
  line-height: 1.4;
}

.confirm-dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.confirm-dialog-btn {
  padding: 4px 12px;
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--main-hover-background-color);
  color: var(--main-color);
}

.confirm-dialog-btn:hover {
  background: var(--main-color);
  color: var(--main-background-color);
  border-color: var(--main-color);
}

.confirm-dialog-btn-cancel {
  background: var(--main-hover-background-color);
  color: var(--secondary-color);
}

/* 消息体外部的复制图标样式 */
.message-copy-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #999;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
  position: relative;
  margin-top: 4px;
  flex-shrink: 0;
}

.message-copy-icon:hover {
  background-color: rgba(0, 0, 0, 0.05);
  color: var(--main-color);
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.message-copy-icon svg {
  width: 14px;
  height: 14px;
}

/* 复制成功提示样式 */
.message-copy-icon .copy-success-tooltip {
  left: 50%;
  transform: translateY(-100%) translateX(-50%);
  margin-bottom: 5px;
  top: -10px;
  font-size: 10px;
  background: rgba(0, 0, 0, 0.8);
  color: #fff;
  padding: 2px 6px;
  border-radius: 3px;
  position: absolute;
  white-space: nowrap;
  z-index: 1000;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.message-copy-icon:hover .copy-success-tooltip,
.message-copy-icon .copy-success-tooltip.visible {
  opacity: 1;
}

/* 消息内容换行设置 */
.text-message,
.ai-response,
.message-content-wrapper {
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: normal;
  font-family: inherit;
  font-size: 11px;
  line-height: 1.3;
  font-weight: normal;
  color: inherit;
}

.message-wrapper.user-message .message-avatar {
  background: var(--user-avatar-background);
  border-color: #28a745;
  box-shadow: 0 2px 6px rgba(40, 167, 69, 0.4);
}

.message-wrapper.user-message .message-avatar:hover {
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.6);
}

.avatar {
  font-size: 12px;
  font-weight: bold;
}

/* 消息内容 */
.message {
  max-width: 90%;
  padding: 4px 8px;
  border-radius: 6px;
  /* 移除边框和阴影效果 */
  box-shadow: none;
  border: none;
  transition: all 0.3s ease;
  animation: messageSlideIn 0.3s ease-out;
  background: var(--message-background-color);
  color: var(--message-text-color);
  font-family: inherit;
  font-size: 11px;
  line-height: 1.3;
  font-weight: normal;
}

.message-wrapper.ai-message .message {
  align-self: flex-start;
  max-width: 90%;
  width: 90%;
}

.message-wrapper.user-message .message {
  align-self: flex-end;
  max-width: 90%;
  word-break: break-word;
  overflow-wrap: break-word;
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-wrapper.user-message .message {
  background: var(--user-message-background-color);
  color: var(--user-message-text-color);
  border: none; /* 移除边框 */
  border-bottom-right-radius: 4px;
  border-top-right-radius: 10px;
  border-top-left-radius: 10px;
  border-bottom-left-radius: 10px;
}

.message-wrapper.ai-message .message {
  border-bottom-left-radius: 4px;
  border-top-left-radius: 10px;
  border-top-right-radius: 10px;
  border-bottom-right-radius: 10px;
}

/* 消息头部 */
.message-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-bottom: 4px;
  font-size: 11px;
  opacity: 0.8;
}

/* 用户消息时间显示在右侧 */
.message-wrapper.user-message .message-header {
  justify-content: flex-end;
}

.sender {
  font-weight: 600;
  font-size: 13px;
  opacity: 0.9;
}

.time {
  font-size: 10px;
  opacity: 1;
  color: #fff;
  background: #007bff;
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(0, 123, 255, 0.3);
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

/* 消息内容容器 */
.message-content {
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: normal;
  font-family: inherit;
  font-size: 11px;
  line-height: 1.3;
  font-weight: normal;
  color: inherit;
  margin: 1px 0;
}

/* 消息内容换行设置 */
.text-message,
.ai-response,
.message-content-wrapper {
  word-break: break-word;
  overflow-wrap: break-word;
  white-space: normal;
  font-family: inherit;
  font-size: 11px;
  line-height: 1.3;
  font-weight: normal;
  color: inherit;
}

.time:hover {
  background: #0056b3;
  box-shadow: 0 3px 10px rgba(0, 123, 255, 0.4);
  transform: translateY(-1px);
}

/* 消息内容 */
.message-content {
  line-height: 1.3;
  font-size: 13px;
  margin: 2px 0;
}

.text-message {
  white-space: pre-wrap;
  line-height: 1.6;
}

/* 富文本样式 - Vue 3 推荐的 v-html 样式 */
/* 增强富文本内容的视觉突出度 */
.text-content,
.ai-response .message-content-wrapper .text-content {
  line-height: 1.6;
  width: 100%;
  white-space: normal;
  color: inherit;
}

/* 保留用户消息原有样式 */
.text-message {
  /* 用户消息样式保持不变 */
}

/* 图片样式增强 */
.text-content img,
.ai-response .message-content-wrapper .text-content img {
  max-width: 100% !important;
  height: auto !important;
  border-radius: 12px !important;
  margin: 20px 0 !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15) !important;
  transition: all 0.3s ease !important;
  border: 3px solid var(--main-background-color) !important;
  white-space: normal !important;
}

.text-content img:hover,
.ai-response .message-content-wrapper .text-content img:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25) !important;
  transform: translateY(-3px) scale(1.02) !important;
  border-color: var(--main-color) !important;
  white-space: normal !important;
}

/* 键盘快捷键样式增强 */
.text-content kbd,
.ai-response .message-content-wrapper .text-content kbd {
  display: inline-block !important;
  padding: 4px 10px !important;
  font-size: 12px !important;
  font-family: 'JetBrains Mono', 'Consolas', 'Monaco', monospace !important;
  color: var(--main-color) !important;
  background-color: var(--main-hover-background-color) !important;
  border: 2px solid var(--main-border-color) !important;
  border-radius: 6px !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15) !important;
  margin: 0 4px !important;
  vertical-align: middle !important;
  transition: all 0.3s ease !important;
  font-weight: 600 !important;
  text-transform: uppercase !important;
  letter-spacing: 0.5px !important;
  white-space: normal !important;
}

.text-content kbd:hover,
.ai-response .message-content-wrapper .text-content kbd:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25) !important;
  transform: translateY(-2px) !important;
  border-color: var(--main-color) !important;
  background-color: var(--main-background-color) !important;
  white-space: normal !important;
}

/* AI响应 */
.ai-response {
  white-space: pre-wrap;
  width: 100%;
  margin: 2px 0;
}

.message-content-wrapper {
  width: 100%;
}

.code-part {
  width: 100%;
}

.text-content {
  line-height: 1.6;
  width: 100%;
}

/* 代码块样式 */
.code-block {
  margin: 12px 0;
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  overflow: hidden;
  background-color: var(--main-background-color);
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  position: relative;
}

/* 代码头部 */
.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 3px 8px;
  background-color: var(--main-hover-background-color);
  border-bottom: 1px solid var(--main-border-color);
  font-size: 11px;
  font-weight: 600;
}

/* 语言标签 */
.language {
  color: var(--main-color);
  font-size: 11px;
  font-weight: normal;
}

/* 复制按钮容器 */
.copy-container {
  display: flex;
  align-items: center;
  position: relative;
}

/* 复制按钮 */
.copy-btn {
  background: none;
  color: #007bff;
  border: none;
  padding: 0;
  font-size: 9px;
  cursor: pointer;
  text-decoration: underline;
  font-family: inherit;
  transition: all 0.2s ease;
  font-weight: 600;
  margin-right: 2px;
}

.copy-btn:hover {
  color: #0056b3;
  text-decoration: none;
  transform: translateY(-1px);
}

.copy-btn:active {
  color: #004085;
  transform: translateY(0);
}

/* 复制成功提示 */
.copy-success-tooltip {
  position: absolute;
  right: 0;
  top: -20px;
  background: #28a745;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 9px;
  font-weight: 600;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  z-index: 100;
  animation: copySuccessFade 0.3s ease-out forwards;
}

@keyframes copySuccessFade {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Monaco编辑器容器样式 */
.monaco-editor-container {
  height: 200px;
  width: 100%;
  border: 1px solid var(--main-border-color);
  border-top: none;
  border-bottom-left-radius: 4px;
  border-bottom-right-radius: 4px;
  overflow: hidden;
  background-color: var(--main-background-color);
}

/* 确保Monaco编辑器样式正确 */
.monaco-editor-container .monaco-editor {
  height: 100% !important;
  width: 100% !important;
}

/* 确保Monaco编辑器样式与系统一致 */
.monaco-editor-container .monaco-editor .monaco-editor-background {
  background-color: var(--main-background-color) !important;
}

.monaco-editor-container .monaco-editor .line-numbers {
  background-color: var(--main-hover-background-color) !important;
  border-right: 1px solid var(--main-border-color) !important;
}

/* 消息操作按钮 */
.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--message-border-color);
}

.message-wrapper.user-message .message-actions {
  border-top-color: var(--user-message-border-color);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: var(--action-button-background-color);
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  color: inherit;
}

.action-btn:hover {
  background: var(--action-button-hover-background-color);
  transform: translateY(-1px);
}

.message-wrapper.user-message .action-btn {
  background: var(--user-action-button-background-color);
}

.message-wrapper.user-message .action-btn:hover {
  background: var(--user-action-button-hover-background-color);
}

/* 加载指示器 */
.loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--text-color);
  font-size: 11px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 0;
  box-shadow: none;
  margin: 0;
  width: auto;
  min-height: auto;
  line-height: normal;
}

.loading-dots {
  display: flex;
  gap: 6px;
}

.loading-dots span {
  width: 8px;
  height: 8px;
  background: var(--loading-dot-color);
  border-radius: 50%;
  animation: loading 1.6s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.48s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(3) {
  animation-delay: -0.16s;
}

@keyframes loading {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.loading-text {
  font-weight: 500;
  color: var(--text-color);
}

/* 思考中状态 */
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px !important;
  margin: 0;
  color: var(--text-color);
  font-size: 11px;
  animation: thinkingSlideIn 0.3s ease-out forwards;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

/* 思考中倒计时样式 */
.thinking-timeout {
  font-size: 10px;
  color: var(--main-color);
  opacity: 0.8;
  font-weight: bold;
  margin-left: 4px;
}

/* 思考中状态滑入动画 */
@keyframes thinkingSlideIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.thinking-dots {
  display: flex;
  gap: 3px;
}

.thinking-dots span {
  width: 6px;
  height: 6px;
  /* 使用蓝色主题色 */
  background: #2196F3;
  border-radius: 50%;
  animation: loading 1.4s infinite ease-in-out both;
  box-shadow: 0 0 4px rgba(33, 150, 243, 0.4);
}

.thinking-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.thinking-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

/* 增强思考点动画 */
@keyframes loading {
  0%, 80%, 100% {
    opacity: 0.4;
    transform: scale(0.8);
    box-shadow: 0 0 4px rgba(33, 150, 243, 0.4);
  }
  40% {
    opacity: 1;
    transform: scale(1.2);
    box-shadow: 0 0 8px rgba(33, 150, 243, 0.6);
  }
}

.thinking-text {
  font-size: 11px;
  /* 使用与思考点一致的蓝色主题色 */
  color: #2196F3;
  opacity: 0.9;
  animation: thinkingTextPulse 1.5s ease-in-out infinite;
  font-weight: 500;
}

/* 思考文字脉冲动画 */
@keyframes thinkingTextPulse {
  0%, 100% {
    opacity: 0.8;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.02);
  }
}

/* 输入区域 */
.input-section {
  padding: 6px 8px;
  background: var(--input-section-background-color);
  border-top: 1px solid var(--input-section-border-color);
  box-shadow: 0 -1px 2px rgba(0, 0, 0, 0.1);
  position: relative;
  z-index: 100;
}

/* 输入按钮容器 */
.input-buttons {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-shrink: 0;
  flex-wrap: nowrap;
}

/* WebSocket设置 */
.ws-settings {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-shrink: 0;
  flex-wrap: nowrap;
}

/* WebSocket设置项 */
.ws-setting-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  white-space: nowrap;
  flex-shrink: 0;
}

/* WebSocket设置标签 */
.ws-setting-label {
  color: var(--text-color);
  opacity: 0.8;
  flex-shrink: 0;
}

/* WebSocket开关 */
.ws-setting-switch {
  position: relative;
  display: inline-block;
  width: 36px;
  height: 18px;
  flex-shrink: 0;
}

.ws-setting-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.ws-setting-switch input:checked + .slider {
  background-color: #2196F3;
}

.ws-setting-switch input:checked + .slider:before {
  transform: translateX(18px);
}

/* WebSocket开关滑块 */
.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 18px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 14px;
  width: 14px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

/* WebSocket设置输入框 */
.ws-setting-input {
  width: 45px;
  height: 22px;
  padding: 0 4px;
  border: 1px solid var(--input-border-color);
  border-radius: 4px;
  background: var(--input-background-color);
  color: var(--input-text-color);
  font-size: 11px;
  text-align: center;
  transition: all 0.3s ease;
  flex-shrink: 0;
  outline: none;
}

/* 禁用状态的设置输入框 */
.ws-setting-input:disabled {
  background-color: var(--input-disabled-background-color) !important;
  color: var(--input-disabled-text-color) !important;
  border-color: var(--input-disabled-border-color) !important;
  cursor: not-allowed !important;
  opacity: 0.6 !important;
}

/* 禁用状态的开关 */
.ws-setting-switch input:disabled + .slider {
  background-color: #ccc !important;
  cursor: not-allowed !important;
  opacity: 0.6 !important;
}

.ws-setting-switch input:disabled + .slider:before {
  background-color: #fff !important;
  cursor: not-allowed !important;
}

/* 禁用状态的开关输入 */
.ws-setting-switch input:disabled {
  cursor: not-allowed !important;
}

/* 禁用状态的设置项标签 */
.ws-setting-item input:disabled,
.ws-setting-item select:disabled,
.ws-setting-item textarea:disabled {
  cursor: not-allowed !important;
  opacity: 0.6 !important;
}

/* WebSocket分隔符 */
.ws-separator {
  width: 1px;
  height: 16px;
  background-color: var(--main-border-color);
  margin: 0 4px;
  flex-shrink: 0;
}

/* WebSocket状态值 */
.ws-status-value {
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  background-color: var(--input-background-color);
  color: var(--input-text-color);
  flex-shrink: 0;
}

/* 连接中状态 */
.ws-status-connecting {
  color: #ffc107 !important;
  background-color: rgba(255, 193, 7, 0.1) !important;
  border: 1px solid rgba(255, 193, 7, 0.3) !important;
  animation: thinkingTextPulse 1.5s ease-in-out infinite !important;
  position: relative;
  display: inline-flex;
  align-items: center;
  padding-right: 20px; /* 为三个点预留空间 */
}

/* 连接失败状态 */
.ws-status-failed {
  color: #f44336 !important;
  background-color: rgba(244, 67, 54, 0.1) !important;
  border: 1px solid rgba(244, 67, 54, 0.3) !important;
}

/* 为连接中状态添加三个点的动画效果，与AI思考中类似 */
.ws-status-connecting::after {
  content: '...';
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  font-weight: bold;
  font-size: 14px;
  color: #ffc107;
  animation: thinkingDots 1.4s ease-in-out infinite;
}

/* 脉冲动画，与AI思考中保持一致 */
@keyframes thinkingTextPulse {
  0%, 100% {
    opacity: 0.8;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.02);
  }
}

/* 三个点的动画效果 */
@keyframes thinkingDots {
  0%, 80%, 100% {
    opacity: 0.4;
    transform: translateY(-50%) scale(0.8);
  }
  40% {
    opacity: 1;
    transform: translateY(-50%) scale(1.2);
  }
}

/* 自定义确认对话框样式 */
.confirm-dialog-overlay {
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

.confirm-dialog {
  background-color: var(--main-background-color);
  border: 1px solid var(--main-border-color);
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  width: 350px;
  max-width: 90%;
  font-size: 12px;
}

.confirm-dialog-title {
  padding: 10px 16px;
  border-bottom: 1px solid var(--main-border-color);
  font-weight: 600;
  background-color: var(--main-hover-background-color);
  color: var(--main-color);
}

.confirm-dialog-content {
  padding: 16px;
  color: var(--main-color);
  line-height: 1.5;
}

.confirm-dialog-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 16px;
  border-top: 1px solid var(--main-border-color);
  background-color: var(--main-hover-background-color);
}

.confirm-dialog-btn {
  padding: 4px 12px;
  border: 1px solid var(--button-border-color);
  border-radius: 3px;
  background-color: var(--button-background-color);
  color: var(--main-color);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.confirm-dialog-btn:hover {
  background-color: var(--button-hover-background-color);
  border-color: var(--button-hover-border-color);
}

.confirm-dialog-btn-confirm {
  background-color: #2196F3;
  color: white;
  border-color: #1976D2;
}

.confirm-dialog-btn-confirm:hover {
  background-color: #1976D2;
  border-color: #1565C0;
}

.confirm-dialog-btn-cancel {
  background-color: var(--button-background-color);
  color: var(--main-color);
  border-color: var(--button-border-color);
}

.confirm-dialog-btn-cancel:hover {
  background-color: var(--button-hover-background-color);
  border-color: var(--button-hover-border-color);
}

/* 已连接状态 */
.ws-status-connected {
  color: #28a745 !important;
  background-color: rgba(40, 167, 69, 0.1) !important;
  border: 1px solid rgba(40, 167, 69, 0.3) !important;
}

/* 已断开状态 */
.ws-status-disconnected {
  color: #17a2b8 !important;
  background-color: rgba(23, 162, 184, 0.1) !important;
  border: 1px solid rgba(23, 162, 184, 0.3) !important;
}

/* 未连接状态 */
.ws-status-not-connected {
  color: #6c757d !important;
  background-color: rgba(108, 117, 125, 0.1) !important;
  border: 1px solid rgba(108, 117, 125, 0.3) !important;
}

/* WebSocket连接按钮 */
.connect-ws-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-color, #2196F3);
  color: var(--primary-text-color, white);
  border: 1px solid var(--primary-color, #2196F3);
  border-radius: 15px;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(var(--primary-color-rgb), 0.3);
}

.connect-ws-btn:hover:not(:disabled) {
  background: var(--primary-hover-color, #1976D2);
  border-color: var(--primary-hover-color, #1976D2);
  box-shadow: 0 3px 8px rgba(var(--primary-color-rgb), 0.4);
  transform: translateY(-1px);
}

.connect-ws-btn:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 2px 4px rgba(var(--primary-color-rgb), 0.3);
}

.connect-ws-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  box-shadow: none;
  transform: none;
}

/* 连接成功状态 (断开连接按钮) */
.connect-ws-btn.connected {
  background: var(--danger-color, #dc3545);
  border-color: var(--danger-color, #dc3545);
  box-shadow: 0 2px 4px rgba(var(--danger-color-rgb), 0.3);
}

.connect-ws-btn.connected:hover {
  background: var(--danger-hover-color, #c82333);
  border-color: var(--danger-hover-color, #c82333);
  box-shadow: 0 3px 8px rgba(var(--danger-color-rgb), 0.4);
}

.connect-ws-btn.connected:active {
  box-shadow: 0 2px 4px rgba(var(--danger-color-rgb), 0.3);
}

.connect-ws-btn.connected:disabled {
  background: var(--danger-color, #dc3545);
  border-color: var(--danger-color, #dc3545);
}

/* 输入框容器 */
.input-container {
  display: flex;
  gap: 6px;
  align-items: center;
  width: 100%;
}

.input-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  position: relative;
}

/* 带清除按钮的输入框容器 */
.input-wrapper-with-clear {
  position: relative;
  flex: 1;
  display: flex;
}

/* 输入提示区域 */
.input-hints {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
  flex-wrap: wrap;
  white-space: normal;
  overflow: visible;
}

/* 消息输入框 */
.message-input {
  flex: 1;
  padding: 10px 35px 10px 14px; /* 增加右侧内边距，为清除按钮留出空间 */
  border: 2px solid var(--input-border-color);
  border-radius: 20px;
  resize: none;
  font-size: 13px;
  line-height: 1.5;
  transition: all 0.3s ease;
  min-height: 40px;
  max-height: 140px;
  font-family: inherit;
  background: var(--input-background-color);
  color: var(--input-text-color);
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.05);
}

/* 清空输入按钮 */
.clear-input-btn {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  background: #ff4d4f;
  color: white;
  border: none;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  font-size: 12px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 10; /* 增加z-index，确保显示在输入框上方 */
  opacity: 0.7; /* 增加透明度，使其不太突兀 */
}

.clear-input-btn:hover {
  opacity: 1; /* 鼠标悬停时不透明 */
  background: #ff7875;
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.15);
}

.clear-input-btn:active {
  transform: translateY(-50%) scale(0.95);
  background: #ff4d4f;
}

.message-input:focus {
  outline: none;
  border-color: #007bff;
  background: var(--input-focus-background-color);
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25), inset 0 1px 3px rgba(0, 0, 0, 0.05);
  transform: translateY(-1px);
}

.message-input::placeholder {
  color: #999;
  font-style: italic;
}

.message-input:hover:not(:focus) {
  border-color: #6c757d;
}

/* 输入提示 */
.input-hints {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
  flex-wrap: nowrap;
  white-space: nowrap;
  overflow: hidden;
}

/* 清理对话按钮 */
.clear-btn {
  background: var(--danger-color, #dc3545);
  border: 1px solid var(--danger-color, #dc3545);
  color: var(--danger-text-color, white);
  padding: 4px 10px;
  border-radius: 15px;
  font-size: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 700;
  font-family: inherit;
  box-shadow: 0 1px 3px rgba(var(--danger-color-rgb), 0.3);
  text-transform: uppercase;
  letter-spacing: 0.3px;
  min-height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-btn:hover:not(:disabled) {
  background: var(--danger-hover-color, #c82333);
  border-color: var(--danger-hover-color, #bd2130);
  transform: translateY(-1px) scale(1.03);
  box-shadow: 0 2px 6px rgba(var(--danger-color-rgb), 0.4);
}

.clear-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.97);
  box-shadow: 0 1px 3px rgba(var(--danger-color-rgb), 0.3);
}

.clear-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.hint-text {
  font-size: 10px;
  color: var(--hint-text-color);
  display: flex;
  align-items: center;
  gap: 4px;
}

.hint-text kbd {
  background: var(--kbd-background-color, var(--main-color));
  border: 1px solid var(--kbd-border-color, var(--main-border-color));
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 9px;
  font-weight: 700;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  color: var(--kbd-text-color, white);
  transition: all 0.2s ease;
}

.hint-text kbd:hover {
  background: var(--kbd-hover-background-color, var(--main-hover-background-color));
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
  transform: translateY(-1px);
}

/* 输入统计区域 */
.input-stats {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 字符计数 */
.character-count {
  font-size: 11px;
  font-weight: 700;
  color: var(--character-count-color, var(--main-color));
  opacity: 1;
  transition: all 0.3s ease;
  white-space: nowrap;
  background: linear-gradient(135deg, rgba(var(--main-color-rgb), 0.1) 0%, rgba(var(--main-color-rgb), 0.05) 100%);
  padding: 3px 8px;
  border-radius: 12px;
  border: 1px solid rgba(var(--main-color-rgb), 0.3);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  position: relative;
  overflow: hidden;
}

.character-count::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(var(--main-color-rgb), 0.5), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.character-count:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(var(--main-color-rgb), 0.25);
}

.character-count:hover::before {
  opacity: 1;
}

/* 接近字符限制时的样式 */
.character-count.near-limit {
  color: var(--character-count-limit-text-color, white);
  background: linear-gradient(135deg, var(--warning-color) 0%, var(--danger-color) 100%);
  border-color: var(--danger-color);
  opacity: 1;
  animation: pulse 1s infinite;
  box-shadow: 0 2px 6px rgba(var(--danger-color-rgb), 0.4);
}

.character-count.near-limit:hover {
  box-shadow: 0 3px 10px rgba(var(--danger-color-rgb), 0.5);
}

.character-count.near-limit::before {
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.5), transparent);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.05);
  }
}

/* 发送按钮 */
/* 语音输入按钮样式 */
.voice-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #666;
  border: 2px solid #e0e0e0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  flex-shrink: 0;
  margin-right: 8px;
  font-size: 16px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.voice-btn:hover {
  background: #e0e0e0;
  border-color: #d0d0d0;
  transform: scale(1.1) translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.voice-btn:active {
  transform: scale(0.95) translateY(0);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 语音输入激活状态 */
.voice-btn.active {
  background: #2196F3;
  color: white;
  border-color: #2196F3;
  animation: voicePulse 1.5s ease-in-out infinite;
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.4);
}

/* 语音输入激活动画 */
@keyframes voicePulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 2px 6px rgba(33, 150, 243, 0.4);
  }
  50% {
    transform: scale(1.1);
    box-shadow: 0 4px 12px rgba(33, 150, 243, 0.8);
  }
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #007bff;
  color: white;
  border: 2px solid #007bff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  flex-shrink: 0;
  margin-left: 8px;
  font-size: 16px;
  box-shadow: 0 2px 6px rgba(0, 123, 255, 0.3);
}

.send-btn:hover:not(:disabled) {
  background: #0056b3;
  border-color: #0056b3;
  transform: scale(1.1) translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.4);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95) translateY(0);
  box-shadow: 0 2px 6px rgba(0, 123, 255, 0.3);
}

.send-btn:disabled {
  background: #e9ecef;
  color: #6c757d;
  border-color: #dee2e6;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
  opacity: 0.8;
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-track {
  background: var(--scrollbar-track-color);
}

.chat-messages::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb-color);
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: var(--scrollbar-thumb-hover-color);
}
</style>
