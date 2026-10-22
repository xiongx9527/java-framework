<template>
    
        <div class="magic-assistant-panel" style="position:relative;width:100%;height:100%">

            <div class="magic-assistant-container">
                <!-- 对话消息区域 -->
                <div class="magic-assistant-messages" ref="messagesContainer">
                    <div v-if="messages.length === 0" class="magic-assistant-welcome">
                        <div class="magic-assistant-welcome-icon">✨</div>
                        <div class="magic-assistant-welcome-title">{{ $i('assistant.welcomeTitle') }}</div>
                        <div class="magic-assistant-welcome-desc">{{ $i('assistant.welcomeDesc') }}</div>

                    </div>
                    <template v-if="messages.length > 0">
                        <div
                            v-for="(msg, index) in messages"
                            :key="index"
                            :class="['magic-assistant-message', msg.role === 'user' ? 'magic-assistant-message--user' : 'magic-assistant-message--assistant']"
                        >
                            <div class="magic-assistant-message-avatar">
                                <span v-if="msg.role === 'user'">
                                    <svg viewBox="0 0 20 20" width="16" height="16" fill="currentColor"><path d="M10 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm-7 8a7 7 0 0 1 14 0H3z"/></svg>
                                </span>
                                <span v-else>
                                    <svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10 3a3.5 3.5 0 0 1 3.5 3.5V8A2.5 2.5 0 0 1 16 10.5V12a2 2 0 0 1-2 2h-.5L12 17H8l-1.5-3H6a2 2 0 0 1-2-2v-1.5A2.5 2.5 0 0 1 6.5 8v-1.5A3.5 3.5 0 0 1 10 3z"/><circle cx="8" cy="9" r="0.8" fill="currentColor"/><circle cx="12" cy="9" r="0.8" fill="currentColor"/></svg>
                                </span>
                            </div>
                            <div class="magic-assistant-message-body">
                                <div class="magic-assistant-message-content">
                                    <div v-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)"></div>
                                    <div v-else>{{ msg.content }}</div>
                                    <span v-if="streaming && msg.role === 'assistant' && index === messages.length - 1" class="magic-assistant-cursor">▍</span>
                                </div>
                                <!-- 代码操作按钮 -->
                                <div class="magic-assistant-code-actions" v-if="msg.role === 'assistant' && !streaming && extractCode(msg.content)">
                                    <button class="magic-assistant-btn magic-assistant-btn--insert" @click="insertCode(extractCode(msg.content))">
                                        <svg viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M8 3v10M3 8h10"/></svg>
                                        {{ $i('assistant.insertCode') }}
                                    </button>
                                    <button class="magic-assistant-btn magic-assistant-btn--replace" @click="replaceCode(extractCode(msg.content))">
                                        <svg viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M2 8h12M12 4l4 4-4 4"/></svg>
                                        {{ $i('assistant.replaceCode') }}
                                    </button>
                                    <button class="magic-assistant-btn magic-assistant-btn--copy" @click="copyCode(extractCode(msg.content))">
                                        <svg viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><rect x="5" y="5" width="9" height="9" rx="1.5"/><path d="M5 11H3.5A1.5 1.5 0 0 1 2 9.5v-7A1.5 1.5 0 0 1 3.5 1h7A1.5 1.5 0 0 1 12 2.5V5"/></svg>
                                        {{ $i('assistant.copy') }}
                                    </button>
                                </div>
                            </div>
                        </div>
                        <!-- 正在等待（非流式阶段才显示loading动画） -->
                        <div v-if="loading && !streaming" class="magic-assistant-message magic-assistant-message--assistant">
                            <div class="magic-assistant-message-avatar">
                                <span>
                                    <svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10 3a3.5 3.5 0 0 1 3.5 3.5V8A2.5 2.5 0 0 1 16 10.5V12a2 2 0 0 1-2 2h-.5L12 17H8l-1.5-3H6a2 2 0 0 1-2-2v-1.5A2.5 2.5 0 0 1 6.5 8v-1.5A3.5 3.5 0 0 1 10 3z"/><circle cx="8" cy="9" r="0.8" fill="currentColor"/><circle cx="12" cy="9" r="0.8" fill="currentColor"/></svg>
                                </span>
                            </div>
                            <div class="magic-assistant-message-body">
                                <div class="magic-assistant-message-content">
                                    <div class="magic-assistant-loading">
                                        <span></span><span></span><span></span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </template>
                </div>

                <!-- 输入区域 -->
                <div class="magic-assistant-input-area">
                    <!-- 模型选择器 -->
                    <div class="magic-assistant-model-selector" v-if="configLoaded && selectableProviders.length > 0">
                        <div class="magic-assistant-select-wrap">
                            <select class="magic-assistant-select" :value="activeProvider" @change="onProviderChange">
                                <option v-for="p in selectableProviders" :key="p.key" :value="p.key">{{ p.label }}</option>
                            </select>
                            <svg class="magic-assistant-select-arrow" viewBox="0 0 12 12" width="10" height="10"><path d="M3 5l3 3 3-3" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                        </div>
                        <div class="magic-assistant-select-wrap magic-assistant-select-wrap--model">
                            <select class="magic-assistant-select" :value="activeModel" @change="onModelChange">
                                <option v-for="m in availableModels" :key="m" :value="m">{{ m }}</option>
                            </select>
                            <svg class="magic-assistant-select-arrow" viewBox="0 0 12 12" width="10" height="10"><path d="M3 5l3 3 3-3" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                        </div>
                    </div>
                    <div class="magic-assistant-input-toolbar">
                        <label class="magic-assistant-context-toggle">
                            <input type="checkbox" v-model="useCurrentCode" />
                            <span class="magic-assistant-toggle-text">{{ $i('assistant.carryContext') }}</span>
                        </label>
                        <button class="magic-assistant-clear-btn" @click="clearMessages" :disabled="messages.length === 0">
                            <svg viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M2 4h12M5 4V3a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1M6 7v5M10 7v5M4 4l1 10h6l1-10"/></svg>
                            {{ $i('assistant.clear') }}
                        </button>
                    </div>
                    <div class="magic-assistant-input-wrapper">
                        <textarea
                            ref="inputRef"
                            v-model="inputText"
                            class="magic-assistant-textarea"
                            :placeholder="$i('assistant.placeholder')"
                            @keydown="onKeydown"
                            :disabled="loading"
                            rows="3"
                        ></textarea>
                        <button class="magic-assistant-send-btn" @click="sendMessage" :disabled="loading || !inputText.trim()">
                            <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M4 10l6-6 6 6M10 4v12"/>
                            </svg>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    
</template>

<script setup>
import { ref, nextTick, computed, inject, onMounted } from 'vue'
import request from '../../../scripts/request.js'
import modal from '../../common/dialog/magic-modal.js'

const bus = inject('bus')
const $i = inject('i18n.format')

const t_requestFailed = $i('assistant.requestFailed')
const t_emptyResponse = $i('assistant.emptyResponse')
const t_configError = $i('assistant.configError')
const t_connectionInterrupted = $i('assistant.connectionInterrupted')
const t_insertSuccess = $i('assistant.insertSuccess')
const t_replaceConfirm = $i('assistant.replaceConfirm')
const t_replaceSuccess = $i('assistant.replaceSuccess')
const t_copySuccess = $i('assistant.copySuccess')
const t_copyFailed = $i('assistant.copyFailed')
const t_tips = $i('assistant.tips')

const visible = ref(false)
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const streaming = ref(false)  // 流式输出中
const useCurrentCode = ref(false)
const messagesContainer = ref(null)
const inputRef = ref(null)

// 模型选择相关状态
const activeProvider = ref('')
const activeModel = ref('')
const configuredProviders = ref([]) // [{key, label, models}]
const configLoaded = ref(false)

const STORAGE_KEY_PROVIDER = 'magic-assistant-provider'
const STORAGE_KEY_MODEL = 'magic-assistant-model'

// 从 localStorage 读取上次选择
const loadLocalSelection = () => {
    try {
        const p = localStorage.getItem(STORAGE_KEY_PROVIDER)
        const m = localStorage.getItem(STORAGE_KEY_MODEL)
        if (p) activeProvider.value = p
        if (m) activeModel.value = m
    } catch (e) { /* ignore */ }
}

// 保存选择到 localStorage
const saveLocalSelection = () => {
    try {
        localStorage.setItem(STORAGE_KEY_PROVIDER, activeProvider.value)
        localStorage.setItem(STORAGE_KEY_MODEL, activeModel.value)
    } catch (e) { /* ignore */ }
}

// 过滤出后端已配置的提供商
const selectableProviders = computed(() => {
    return configuredProviders.value.map(p => ({ key: p.key, label: p.label || p.key }))
})

// 当前提供商下的可用模型
const availableModels = computed(() => {
    const provider = activeProvider.value
    const found = configuredProviders.value.find(p => p.key === provider)
    return found && found.models ? found.models : []
})

// 从后端加载已配置的提供商列表，并恢复本地选择
const loadAssistantConfig = () => {
    request.sendJson('/ai/config/get', {}).success(data => {
        if (Array.isArray(data)) {
            configuredProviders.value = data
        }
        // 恢复 localStorage 中的选择
        loadLocalSelection()
        // 如果本地没有选择或选择的提供商已不存在，使用第一个提供商
        const providerKeys = configuredProviders.value.map(p => p.key)
        if (!activeProvider.value || !providerKeys.includes(activeProvider.value)) {
            if (configuredProviders.value.length > 0) {
                activeProvider.value = configuredProviders.value[0].key
                const models = configuredProviders.value[0].models || []
                activeModel.value = models[0] || ''
                saveLocalSelection()
            }
        } else {
            // 验证当前模型是否仍在可用列表中
            const found = configuredProviders.value.find(p => p.key === activeProvider.value)
            const models = found && found.models ? found.models : []
            if (activeModel.value && !models.includes(activeModel.value)) {
                activeModel.value = models[0] || ''
                saveLocalSelection()
            }
        }
        configLoaded.value = true
    }).error(() => {
        configLoaded.value = true
    })
}

const onProviderChange = (e) => {
    const newProvider = e.target.value
    activeProvider.value = newProvider
    // 自动选择新提供商的第一个模型
    const found = configuredProviders.value.find(p => p.key === newProvider)
    const models = found && found.models ? found.models : []
    activeModel.value = models[0] || ''
    saveLocalSelection()
}

const onModelChange = (e) => {
    activeModel.value = e.target.value
    saveLocalSelection()
}



// 从bus事件打开对话框
onMounted(() => {
    if (!configLoaded.value) {
        loadAssistantConfig()
    }
    nextTick(() => inputRef.value && inputRef.value.focus())
})

const onClose = () => {
    visible.value = false
}





// 获取请求的 baseURL（从当前页面路径推导）
const getBaseURL = () => {
    const path = window.location.pathname
    // 页面路径如 /magic/web/index.html 或 /magic/web/，取目录部分
    const dir = path.endsWith('/') ? path.slice(0, -1) : path.substring(0, path.lastIndexOf('/'))
    return window.location.origin + dir
}

const sendMessage = async () => {
    const text = inputText.value.trim()
    if (!text || loading.value) return

    let currentCode = ''
    if (useCurrentCode.value) {
        bus.$emit('assistant-get-current-code', (code) => {
            currentCode = code || ''
        })
        await nextTick()
    }

    messages.value.push({ role: 'user', content: text })
    inputText.value = ''
    loading.value = true
    streaming.value = false
    scrollToBottom()

    const history = messages.value.slice(0, -1).slice(-10).map(m => ({
        role: m.role,
        content: m.content
    }))

    // 添加空的 assistant 消息占位，用于流式追加
    messages.value.push({ role: 'assistant', content: '' })
    const msgIndex = messages.value.length - 1
    scrollToBottom()

    const token = localStorage.getItem('magic-token') || 'unauthorization'
    const baseURL = getBaseURL()

    try {
        const resp = await fetch(baseURL + '/ai/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'magic-token': token
            },
            body: JSON.stringify({
                message: text,
                history,
                currentCode: useCurrentCode.value ? currentCode : '',
                provider: activeProvider.value,
                model: activeModel.value
            })
        })

        if (!resp.ok) {
            messages.value[msgIndex].content = t_requestFailed + resp.status
            scrollToBottom()
            return
        }

        streaming.value = true
        const reader = resp.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (true) {
            const { done, value } = await reader.read()
            if (done) break
            buffer += decoder.decode(value, { stream: true })

            // 按行解析 SSE 数据
            const lines = buffer.split('\n')
            buffer = lines.pop() // 保留不完整的最后一行

            for (const line of lines) {
                const trimmed = line.trim()
                if (!trimmed || trimmed.startsWith(':')) continue
                if (trimmed.startsWith('event:done') || trimmed.startsWith('event: done')) {
                    // 流结束
                    continue
                }
                if (trimmed.startsWith('data:')) {
                    const data = trimmed.substring(5)
                    if (data.trim() === '[DONE]') continue
                    // SSE data 是 JSON 编码的字符串，parse 还原换行等特殊字符
                    try {
                        const text = JSON.parse(data)
                        messages.value[msgIndex].content += text
                    } catch (e) {
                        // 解析失败则直接追加原始文本
                        messages.value[msgIndex].content += data
                    }
                    scrollToBottom()
                }
            }
        }

        // 如果最终内容为空，显示提示
        if (!messages.value[msgIndex].content.trim()) {
            messages.value[msgIndex].content = t_emptyResponse
        }
    } catch (e) {
        if (!messages.value[msgIndex].content) {
            messages.value[msgIndex].content = t_configError
        } else {
            messages.value[msgIndex].content += '\n\n' + t_connectionInterrupted
        }
        scrollToBottom()
    } finally {
        loading.value = false
        streaming.value = false
    }
}

const onKeydown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        sendMessage()
    }
}

const clearMessages = () => {
    messages.value = []
}

const scrollToBottom = () => {
    nextTick(() => {
        if (messagesContainer.value) {
            messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
        }
    })
}

const extractCode = (content) => {
    if (!content) return null
    const match = content.match(/```(?:magicscript|ms)?\s*\n?([\s\S]*?)```/)
    return match ? match[1].trim() : null
}

const insertCode = (code) => {
    bus.$emit('assistant-insert-code', code)
    modal.message(t_insertSuccess)
}

const replaceCode = (code) => {
    modal.confirm(t_tips, t_replaceConfirm, () => {
        bus.$emit('assistant-replace-code', code)
        modal.message(t_replaceSuccess)
    })
}

const copyCode = (code) => {
    navigator.clipboard.writeText(code).then(() => {
        modal.message(t_copySuccess)
    }).catch(() => {
        modal.alert(t_copyFailed)
    })
}

const renderMarkdown = (content) => {
    if (!content) return ''
    let html = content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')

    html = html.replace(/```(?:magicscript|ms|java|sql|json|javascript|js)?\s*\n?([\s\S]*?)```/g, (match, code) => {
        return `<div class="magic-assistant-code-block"><pre><code>${code.trim()}</code></pre></div>`
    })

    html = html.replace(/`([^`\n]+)`/g, '<code class="magic-assistant-inline-code">$1</code>')
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    html = html.replace(/^### (.+)$/gm, '<h4>$1</h4>')
    html = html.replace(/^## (.+)$/gm, '<h3>$1</h3>')
    html = html.replace(/^# (.+)$/gm, '<h2>$1</h2>')
    html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
    html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>')
    html = html.replace(/<\/ul>\s*<ul>/g, '')
    html = html.replace(/\n/g, '<br/>')

    return html
}

defineExpose({ visible })
</script>

<style scoped>
.magic-assistant-panel {
    display: flex;
    flex-direction: column;
    height: 100%;
    width: 100%;
    max-width: 420px;
    margin: 0 auto;
    background: var(--main-background-color);
    display: flex;
    flex-direction: column;
    background: var(--main-background-color);
}
@keyframes slideInRight {
    from { transform: translateX(20px); opacity: 0; }
    to { transform: translateX(0); opacity: 1; }
}


/* Container */
.magic-assistant-container {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
}

/* Messages area */
.magic-assistant-messages {
    flex: 1;
    overflow-y: auto;
    padding: 10px 12px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-height: 0;
}
.magic-assistant-messages::-webkit-scrollbar {
    width: 5px;
}
.magic-assistant-messages::-webkit-scrollbar-thumb {
    background: var(--main-border-color);
    border-radius: 3px;
}
.magic-assistant-messages::-webkit-scrollbar-track {
    background: transparent;
}


/* Welcome */
.magic-assistant-welcome {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    padding: 10px 20px;
}
.magic-assistant-welcome-icon-wrap {
    margin-bottom: 8px;
}
.magic-assistant-welcome-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--main-color);
    margin-bottom: 4px;
}
.magic-assistant-welcome-desc {
    font-size: 12px;
    color: var(--empty-color);
    margin-bottom: 12px;
    text-align: center;
    line-height: 1.5;
    max-width: 280px;
}


/* Messages */
.magic-assistant-message {
    display: flex;
    gap: 10px;
    align-items: flex-start;
    animation: fadeInMsg 0.2s ease-out;
}
@keyframes fadeInMsg {
    from { opacity: 0; transform: translateY(6px); }
    to { opacity: 1; transform: translateY(0); }
}
.magic-assistant-message--user {
    flex-direction: row-reverse;
}
.magic-assistant-message-avatar {
    width: 30px;
    height: 30px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    font-weight: bold;
    flex-shrink: 0;
}
.magic-assistant-message--user .magic-assistant-message-avatar {
    background: #5b7ee5;
    color: #fff;
}
.magic-assistant-message--assistant .magic-assistant-message-avatar {
    background: linear-gradient(135deg, #5b7ee5 0%, #4da6e8 100%);
    color: #fff;
}
.magic-assistant-message-body {
    max-width: 82%;
    min-width: 0;
}
.magic-assistant-message-content {
    padding: 10px 14px;
    border-radius: 12px;
    font-size: 13px;
    line-height: 1.65;
    color: var(--main-color);
    background: var(--select-option-hover-background-color);
    word-break: break-word;
}
.magic-assistant-message--user .magic-assistant-message-content {
    background: var(--select-option-hover-background-color);
    color: var(--main-color);
    border-radius: 12px 4px 12px 12px;
}
.magic-assistant-message--assistant .magic-assistant-message-content {
    border-radius: 4px 12px 12px 12px;
}

/* Code action buttons */
.magic-assistant-code-actions {
    display: flex;
    gap: 6px;
    margin-top: 8px;
    flex-wrap: wrap;
}
.magic-assistant-btn {
    padding: 4px 10px;
    border-radius: 6px;
    font-size: 11px;
    cursor: pointer;
    border: none;
    transition: all 0.15s;
    display: flex;
    align-items: center;
    gap: 4px;
    font-family: inherit;
}
.magic-assistant-btn--insert {
    background: var(--select-option-hover-background-color);
    color: var(--main-color);
    border: 1px solid var(--main-border-color);
}
.magic-assistant-btn--insert:hover {
    background: #5b7ee5;
    color: #fff;
    border-color: #5b7ee5;
}
.magic-assistant-btn--replace {
    background: var(--select-option-hover-background-color);
    color: var(--main-color);
    border: 1px solid var(--main-border-color);
}
.magic-assistant-btn--replace:hover {
    background: #5b7ee5;
    color: #fff;
    border-color: #5b7ee5;
}
.magic-assistant-btn--copy {
    background: transparent;
    color: var(--main-color);
    border: 1px solid var(--main-border-color);
}
.magic-assistant-btn--copy:hover {
    background: var(--select-option-hover-background-color);
}

/* Loading dots */
.magic-assistant-loading {
    display: flex;
    gap: 5px;
    align-items: center;
    padding: 4px 0;
}
.magic-assistant-loading span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--main-icon-color, #999);
    animation: assistantDot 1.4s infinite ease-in-out;
}
.magic-assistant-loading span:nth-child(2) { animation-delay: 0.2s; }
.magic-assistant-loading span:nth-child(3) { animation-delay: 0.4s; }
@keyframes assistantDot {
    0%, 80%, 100% { transform: scale(0.5); opacity: 0.3; }
    40% { transform: scale(1); opacity: 1; }
}

/* Input area */
.magic-assistant-input-area {
    border-top: 1px solid var(--main-border-color);
    padding: 8px 10px 10px;
    background: var(--main-background-color);
    flex-shrink: 0;
}

/* Model selector */
.magic-assistant-model-selector {
    display: flex;
    gap: 6px;
    margin-bottom: 4px;
}
.magic-assistant-select-wrap {
    flex: 1;
    position: relative;
    display: flex;
    align-items: center;
}
.magic-assistant-select-wrap--model {
    flex: 1.5;
}
.magic-assistant-select {
    width: 100%;
    height: 30px;
    padding: 0 22px 0 8px;
    border: 1px solid var(--main-border-color);
    border-radius: 6px;
    background: var(--main-background-color);
    color: var(--main-color);
    font-size: 12px;
    outline: none;
    cursor: pointer;
    appearance: none;
    -webkit-appearance: none;
    transition: border-color 0.15s;
}
.magic-assistant-select:focus {
    border-color: #5b7ee5;
}
.magic-assistant-select-arrow {
    position: absolute;
    right: 8px;
    pointer-events: none;
    opacity: 0.5;
    color: var(--main-color);
}

/* Toolbar */
.magic-assistant-input-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 4px;
}
.magic-assistant-context-toggle {
    display: flex;
    align-items: center;
    gap: 5px;
    cursor: pointer;
    user-select: none;
}
.magic-assistant-context-toggle input {
    cursor: pointer;
    accent-color: #5b7ee5;
    width: 14px;
    height: 14px;
}
.magic-assistant-toggle-text {
    font-size: 11px;
    color: var(--empty-color);
}
.magic-assistant-clear-btn {
    padding: 3px 8px;
    background: transparent;
    border: 1px solid var(--main-border-color);
    border-radius: 5px;
    font-size: 11px;
    cursor: pointer;
    color: var(--empty-color);
    display: flex;
    align-items: center;
    gap: 3px;
    transition: all 0.15s;
    font-family: inherit;
}
.magic-assistant-clear-btn:hover:not(:disabled) {
    background: var(--select-option-hover-background-color);
    color: var(--main-color);
    border-color: var(--main-color);
}
.magic-assistant-clear-btn:disabled { opacity: 0.3; cursor: default; }

/* Input wrapper */
.magic-assistant-input-wrapper {
    display: flex;
    gap: 8px;
    align-items: flex-end;
}
.magic-assistant-textarea {
    flex: 1;
    min-height: 48px;
    max-height: 120px;
    padding: 8px 12px;
    border: 1px solid var(--main-border-color);
    border-radius: 10px;
    resize: none;
    font-size: 13px;
    background: var(--main-background-color);
    color: var(--main-color);
    font-family: inherit;
    line-height: 1.5;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
}
.magic-assistant-textarea:focus {
    border-color: #5b7ee5;
    box-shadow: 0 0 0 2px rgba(91, 126, 229, 0.12);
}
.magic-assistant-textarea:disabled { opacity: 0.5; cursor: not-allowed; }
.magic-assistant-textarea::placeholder { color: var(--empty-color); font-size: 12px; }

/* Send button */
.magic-assistant-send-btn {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    background: linear-gradient(135deg, #5b7ee5 0%, #4da6e8 100%);
    border: none;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    transition: all 0.2s;
    color: #fff;
}
.magic-assistant-send-btn:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 3px 10px rgba(91, 126, 229, 0.3);
}
.magic-assistant-send-btn:active:not(:disabled) {
    transform: translateY(0);
}
.magic-assistant-send-btn:disabled {
    background: var(--main-border-color);
    cursor: not-allowed;
    opacity: 0.5;
}

/* Streaming cursor */
.magic-assistant-cursor {
    display: inline;
    animation: blink 1s step-end infinite;
    color: var(--main-color);
    font-weight: bold;
}
@keyframes blink {
    0%, 50% { opacity: 1; }
    51%, 100% { opacity: 0; }
}
</style>

<style>
/* Code block global styles */
.magic-assistant-message-content .magic-assistant-code-block {
    background: #1a1b26;
    border-radius: 8px;
    margin: 8px 0;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.06);
}
.magic-assistant-message-content .magic-assistant-code-block pre {
    margin: 0;
    padding: 14px 16px;
    overflow-x: auto;
}
.magic-assistant-message-content .magic-assistant-code-block pre::-webkit-scrollbar {
    height: 4px;
}
.magic-assistant-message-content .magic-assistant-code-block pre::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.15);
    border-radius: 2px;
}
.magic-assistant-message-content .magic-assistant-code-block code {
    color: #c0caf5;
    font-family: 'JetBrainsMono', 'Fira Code', Consolas, 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.6;
    white-space: pre;
}
.magic-assistant-message-content .magic-assistant-inline-code {
    background: var(--select-option-hover-background-color);
    padding: 1px 6px;
    border-radius: 4px;
    font-family: 'JetBrainsMono', Consolas, monospace;
    font-size: 12px;
    color: var(--main-color);
}
.magic-assistant-message-content h2,
.magic-assistant-message-content h3,
.magic-assistant-message-content h4 {
    margin: 10px 0 4px;
    font-weight: 600;
    line-height: 1.4;
}
.magic-assistant-message-content h2 { font-size: 15px; }
.magic-assistant-message-content h3 { font-size: 14px; }
.magic-assistant-message-content h4 { font-size: 13px; }
.magic-assistant-message-content ul {
    margin: 4px 0;
    padding-left: 18px;
}
.magic-assistant-message-content li {
    margin: 3px 0;
    line-height: 1.5;
}
.magic-assistant-message-content strong {
    font-weight: 600;
}
</style>
