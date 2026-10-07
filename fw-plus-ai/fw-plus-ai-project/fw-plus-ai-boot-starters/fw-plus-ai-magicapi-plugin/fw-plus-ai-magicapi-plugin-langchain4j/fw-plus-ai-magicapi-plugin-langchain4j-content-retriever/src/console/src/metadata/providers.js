/**
 * provider → options 字段元数据表（本类型：langchain4j-content-retriever）。
 *
 * 字段由后端各 provider 实际读取的 options 键生成，顺序与读取顺序一致（必填在前）。
 * 字段类型：text / number / switch / select / duration / tags / map。
 * 敏感字段（api-key、secret-key、password、token 等）由组件统一按密码框渲染。
 * 与后端的一致性由 test/content-retriever-console-metadata.test.cjs 交叉校验。
 */
export const PROVIDER_FIELDS = {
    'embedding-store': [{ name: 'embedding-store-key', type: 'text' }, { name: 'embedding-model-key', type: 'text' }, { name: 'max-results', type: 'number' }, { name: 'min-score', type: 'number' }],
    'neo4j': [{ name: 'chat-model-key', type: 'text' }, { name: 'uri', type: 'text' }, { name: 'username', type: 'text' }, { name: 'password', type: 'text' }, { name: 'prompt-template', type: 'text' }],
    'nebula': [{ name: 'chat-model-key', type: 'text' }, { name: 'addresses', type: 'text' }, { name: 'username', type: 'text' }, { name: 'password', type: 'text' }, { name: 'space-name', type: 'text' }, { name: 'max-session-size', type: 'number' }, { name: 'timeout', type: 'number' }, { name: 'prompt-template', type: 'text' }],
    'azure-ai-search': [{ name: 'endpoint', type: 'text' }, { name: 'api-key', type: 'text' }, { name: 'index-name', type: 'text' }, { name: 'embedding-model-key', type: 'text' }, { name: 'dimensions', type: 'number' }, { name: 'query-type', type: 'select' }, { name: 'max-results', type: 'number' }, { name: 'min-score', type: 'number' }, { name: 'create-or-update-index', type: 'switch' }],
    'web-search': [{ name: 'search-function', type: 'text' }, { name: 'max-results', type: 'number' }],
    'filtering': [{ name: 'content-retriever-key', type: 'text' }, { name: 'filter-function', type: 'text' }, { name: 'min-score', type: 'number' }, { name: 'filters', type: 'map' }],
    'aggregating': [{ name: 'content-retriever-keys', type: 'tags' }, { name: 'max-results', type: 'number' }, { name: 'deduplicate', type: 'switch' }],
    'knowledge': [{ name: 'dataset-id', type: 'text' }, { name: 'document-id', type: 'text' }, { name: 'max-results', type: 'number' }, { name: 'min-score', type: 'number' }, { name: 'metadata-condition', type: 'text' }, { name: 'metadata-filter-mode', type: 'select' }],
};

/**
 * provider → 被引用的 datasource 字段：这类字段要从同类资源里选 key，
 * 因此不放进 PROVIDER_FIELDS（那里只描述 options 键）。
 */
export const REFERENCE_FIELDS = {
    // required：后端用 o.required 读取（必填）；未标注的留空时后端回落到 Spring 默认实例
    'embedding-store': [{ name: 'embedding-store-key', resource: 'langchain4j-embedding-store', required: true }, { name: 'embedding-model-key', resource: 'langchain4j-embedding-model' }],
    'neo4j': [{ name: 'chat-model-key', resource: 'langchain4j-chat-model', required: true }],
    'nebula': [{ name: 'chat-model-key', resource: 'langchain4j-chat-model', required: true }],
    'azure-ai-search': [{ name: 'embedding-model-key', resource: 'langchain4j-embedding-model', required: true }],
    'web-search': [],
    'filtering': [{ name: 'content-retriever-key', resource: 'langchain4j-content-retriever', required: true }],
    // 组合检索引用的是多个 key（逗号分隔），用文本输入而非单个下拉，故不列入引用字段
    'aggregating': [],
    // knowledge 检索器引用知识库数据集（kb_dataset.id），候选来自管理端数据集列表接口
    'knowledge': [{ name: 'dataset-id', resource: 'kb-dataset', required: true }],
};

/** 取某 provider 的 options 字段定义；未知 provider 返回空数组 */
export function optionsFields(provider) {
    return PROVIDER_FIELDS[provider] || [];
}

/** 取某 provider 需要引用的 datasource 字段定义；未知 provider 返回空数组 */
export function referenceFields(provider) {
    return REFERENCE_FIELDS[provider] || [];
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
    'query-type': ['VECTOR', 'FULL_TEXT', 'HYBRID', 'HYBRID_WITH_RERANKING'],
    'metadata-filter-mode': ['none', 'auto'],
};
