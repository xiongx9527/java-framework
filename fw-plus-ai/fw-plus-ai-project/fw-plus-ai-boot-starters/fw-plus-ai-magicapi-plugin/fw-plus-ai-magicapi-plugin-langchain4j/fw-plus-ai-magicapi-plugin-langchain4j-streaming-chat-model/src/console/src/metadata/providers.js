/**
 * provider → options 字段元数据表（本类型：langchain4j-streaming-chat-model）。
 *
 * 字段由后端各 provider 工厂实际读取的 options 键生成，顺序与工厂读取顺序一致（必填在前）。
 * 字段类型：text / number / switch / select / duration / tags。
 * 敏感字段（api-key、secret-key、password、token 等）由组件统一按密码框渲染。
 * 与后端的一致性由 test/streaming-chat-model-console-metadata.test.cjs 交叉校验。
 */
export const PROVIDER_FIELDS = {
    'anthropic': [{ name: 'api-key', type: 'text', required: true }, { name: 'base-url', type: 'text' }, { name: 'version', type: 'text' }, { name: 'model-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'stop-sequences', type: 'tags' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'azure-openai': [{ name: 'endpoint', type: 'text', required: true }, { name: 'api-key', type: 'text', required: true }, { name: 'deployment-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'presence-penalty', type: 'number' }, { name: 'frequency-penalty', type: 'number' }, { name: 'user', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'github-models': [{ name: 'github-token', type: 'text', required: true }, { name: 'endpoint', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'presence-penalty', type: 'number' }, { name: 'frequency-penalty', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'local-ai': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'ollama': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'repeat-penalty', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'num-predict', type: 'number' }, { name: 'num-ctx', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'format', type: 'text' }, { name: 'timeout', type: 'duration' }],
    'openai': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'organization-id', type: 'text' }, { name: 'model-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'max-tokens', type: 'number' }, { name: 'presence-penalty', type: 'number' }, { name: 'frequency-penalty', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'user', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'qianfan': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'secret-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'penalty-score', type: 'number' }, { name: 'endpoint', type: 'text' }, { name: 'response-format', type: 'text' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'qwen': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'enable-search', type: 'switch' }, { name: 'seed', type: 'number' }, { name: 'repetition-penalty', type: 'number' }, { name: 'stops', type: 'tags' }, { name: 'max-tokens', type: 'number' }],
    'vertex-ai-gemini': [{ name: 'project', type: 'text', required: true }, { name: 'location', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'max-output-tokens', type: 'number' }, { name: 'response-mime-type', type: 'text' }, { name: 'use-google-search', type: 'switch' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'zhipu-ai': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'stops', type: 'tags' }, { name: 'max-token', type: 'number' }, { name: 'call-timeout', type: 'duration' }, { name: 'connect-timeout', type: 'duration' }, { name: 'read-timeout', type: 'duration' }, { name: 'write-timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
};

/** 取某 provider 的 options 字段定义；未知 provider 返回空数组 */
export function optionsFields(provider) {
    return PROVIDER_FIELDS[provider] || [];
}

/** 已声明字段定义的 provider 短名集合 */
export function providerNames() {
    return Object.keys(PROVIDER_FIELDS);
}

/** 字段名是否需要按密码输入渲染 */
const SENSITIVE = ['api-key', 'secret-key', 'password', 'token', 'access-token', 'github-token'];

export function isSensitive(name) {
    return SENSITIVE.indexOf(name) >= 0;
}

/**
 * 枚举型字段的候选值：来自后端 SDK 枚举的常用取值；允许直接输入，SDK 新增取值不会阻塞配置。
 */
export const SELECT_HINTS = {

};
