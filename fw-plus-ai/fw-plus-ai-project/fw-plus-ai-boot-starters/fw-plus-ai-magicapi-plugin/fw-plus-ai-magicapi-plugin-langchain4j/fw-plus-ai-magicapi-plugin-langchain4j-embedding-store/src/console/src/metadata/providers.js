/**
 * provider → options 字段元数据表（本类型：langchain4j-embedding-store）。
 *
 * 字段由后端各 provider 工厂实际读取的 options 键生成，顺序与工厂读取顺序一致（必填在前）。
 * 字段类型：text / number / switch / select / duration / tags。
 * 敏感字段（api-key、secret-key、password、token 等）由组件统一按密码框渲染。
 * 与后端的一致性由 test/embedding-store-console-metadata.test.cjs 交叉校验。
 */
export const PROVIDER_FIELDS = {
    'elasticsearch': [{ name: 'server-url', type: 'text' }, { name: 'api-key', type: 'text' }, { name: 'username', type: 'text' }, { name: 'password', type: 'text' }, { name: 'index-name', type: 'text' }, { name: 'dimension', type: 'number' }],
    'in-memory': [],
    'milvus': [{ name: 'host', type: 'text' }, { name: 'port', type: 'number' }, { name: 'collection-name', type: 'text' }, { name: 'index-type', type: 'select' }, { name: 'metric-type', type: 'select' }, { name: 'uri', type: 'text' }, { name: 'token', type: 'text' }, { name: 'username', type: 'text' }, { name: 'password', type: 'text' }, { name: 'consistency-level', type: 'select' }, { name: 'retrieve-embeddings-on-search', type: 'switch' }, { name: 'auto-flush-on-insert', type: 'switch' }, { name: 'database-name', type: 'text' }, { name: 'dimension', type: 'number' }],
    'neo4j': [{ name: 'uri', type: 'text' }, { name: 'username', type: 'text' }, { name: 'password', type: 'text' }, { name: 'label', type: 'text' }, { name: 'embedding-property', type: 'text' }, { name: 'id-property', type: 'text' }, { name: 'metadata-prefix', type: 'text' }, { name: 'text-property', type: 'text' }, { name: 'index-name', type: 'text' }, { name: 'database-name', type: 'text' }, { name: 'dimension', type: 'number' }],
    'nebula': [{ name: 'addresses', type: 'text' }, { name: 'username', type: 'text' }, { name: 'password', type: 'text' }, { name: 'space-name', type: 'text' }, { name: 'tag', type: 'text' }, { name: 'id-property', type: 'text' }, { name: 'text-property', type: 'text' }, { name: 'embedding-property', type: 'text' }, { name: 'metadata-property', type: 'text' }, { name: 'max-session-size', type: 'number' }, { name: 'timeout', type: 'number' }, { name: 'max-scan-rows', type: 'number' }, { name: 'await-schema-timeout', type: 'number' }, { name: 'dimension', type: 'number' }],
    'qdrant': [{ name: 'host', type: 'text' }, { name: 'port', type: 'number' }, { name: 'collection-name', type: 'text' }, { name: 'use-tls', type: 'switch' }, { name: 'payload-text-key', type: 'text' }, { name: 'api-key', type: 'text' }],
    'redis': [{ name: 'host', type: 'text' }, { name: 'port', type: 'number' }, { name: 'user', type: 'text' }, { name: 'password', type: 'text' }, { name: 'index-name', type: 'text' }, { name: 'metadata-keys', type: 'tags' }, { name: 'dimension', type: 'number' }],
    'weaviate': [{ name: 'scheme', type: 'text' }, { name: 'host', type: 'text' }, { name: 'port', type: 'number' }, { name: 'api-key', type: 'text' }, { name: 'object-class', type: 'text' }, { name: 'use-grpc-for-inserts', type: 'switch' }, { name: 'secured-grpc', type: 'switch' }, { name: 'grpc-port', type: 'number' }, { name: 'avoid-dups', type: 'switch' }, { name: 'consistency-level', type: 'text' }, { name: 'metadata-keys', type: 'tags' }],
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
    'consistency-level': ['STRONG', 'SESSION', 'BOUNDED', 'EVENTUALLY'],
    'index-type': ['FLAT', 'IVF_FLAT', 'IVF_SQ8', 'IVF_PQ', 'HNSW', 'ANNOY', 'DISKANN', 'AUTOINDEX'],
    'metric-type': ['L2', 'IP', 'COSINE'],
};
