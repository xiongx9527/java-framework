/**
 * (数据源类型 × provider) 的 options 字段元数据表。
 *
 * 说明：
 * - 键为后端 datasource type（与 Langchain4jModelTypes 的 type 值一致），值为 provider 短名到字段数组。
 * - 字段定义由后端各 provider 工厂实际读取的 options 键生成，顺序与工厂读取顺序一致（必填在前）。
 * - type 取值：text / password / number / switch / select / duration / tags / json。
 * - 敏感字段（api-key、secret-key、password、token、*-token）由渲染器统一按 password 处理，此处不重复标注。
 * - 表内容与后端工厂保持同步，由 test/langchain4j-console-metadata.test.cjs 交叉校验。
 */
export const PROVIDER_FIELDS = {
  'all-minilm': {
    'langchain4j-embedding-model': [],
  },
  'anthropic': {
    'langchain4j-chat-model': [{ name: 'api-key', type: 'text', required: true }, { name: 'base-url', type: 'text' }, { name: 'version', type: 'text' }, { name: 'beta', type: 'text' }, { name: 'model-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'stop-sequences', type: 'tags' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-streaming-chat-model': [{ name: 'api-key', type: 'text', required: true }, { name: 'base-url', type: 'text' }, { name: 'version', type: 'text' }, { name: 'model-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'stop-sequences', type: 'tags' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
  },
  'bge-small': {
    'langchain4j-embedding-model': [],
  },
  'azure-openai': {
    'langchain4j-chat-model': [{ name: 'endpoint', type: 'text', required: true }, { name: 'api-key', type: 'text', required: true }, { name: 'deployment-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'presence-penalty', type: 'number' }, { name: 'frequency-penalty', type: 'number' }, { name: 'user', type: 'text' }, { name: 'seed', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'langchain4j-embedding-model': [{ name: 'endpoint', type: 'text', required: true }, { name: 'api-key', type: 'text', required: true }, { name: 'deployment-name', type: 'text', required: true }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'langchain4j-language-model': [{ name: 'endpoint', type: 'text', required: true }, { name: 'api-key', type: 'text', required: true }, { name: 'deployment-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'langchain4j-streaming-chat-model': [{ name: 'endpoint', type: 'text', required: true }, { name: 'api-key', type: 'text', required: true }, { name: 'deployment-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'presence-penalty', type: 'number' }, { name: 'frequency-penalty', type: 'number' }, { name: 'user', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'langchain4j-streaming-language-model': [{ name: 'endpoint', type: 'text', required: true }, { name: 'api-key', type: 'text', required: true }, { name: 'deployment-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'langchain4j-tokenizer': [],
  },
  'elasticsearch': {
    'langchain4j-embedding-store': [{ name: 'server-url', type: 'text' }, { name: 'api-key', type: 'text' }, { name: 'username', type: 'text' }, { name: 'password', type: 'text' }, { name: 'index-name', type: 'text' }, { name: 'dimension', type: 'number' }],
  },
  'github-models': {
    'langchain4j-chat-model': [{ name: 'github-token', type: 'text', required: true }, { name: 'endpoint', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'presence-penalty', type: 'number' }, { name: 'frequency-penalty', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'langchain4j-embedding-model': [{ name: 'github-token', type: 'text', required: true }, { name: 'endpoint', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'langchain4j-streaming-chat-model': [{ name: 'github-token', type: 'text', required: true }, { name: 'endpoint', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'presence-penalty', type: 'number' }, { name: 'frequency-penalty', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests-and-responses', type: 'switch' }],
  },
  'google-ai-gemini': {
    'langchain4j-chat-model': [{ name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-output-tokens', type: 'number' }, { name: 'max-retries', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'stop-sequences', type: 'tags' }, { name: 'allow-code-execution', type: 'switch' }, { name: 'log-requests-and-responses', type: 'switch' }],
    'langchain4j-embedding-model': [{ name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'max-retries', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests-and-responses', type: 'switch' }],
  },
  'hugging-face': {
    'langchain4j-chat-model': [{ name: 'access-token', type: 'text', required: true }, { name: 'model-id', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'max-new-tokens', type: 'number' }, { name: 'return-full-text', type: 'switch' }, { name: 'wait-for-model', type: 'switch' }, { name: 'timeout', type: 'duration' }],
    'langchain4j-embedding-model': [{ name: 'access-token', type: 'text', required: true }, { name: 'model-id', type: 'text' }, { name: 'wait-for-model', type: 'switch' }, { name: 'timeout', type: 'duration' }],
    'langchain4j-language-model': [{ name: 'access-token', type: 'text', required: true }, { name: 'model-id', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'temperature', type: 'number' }, { name: 'max-new-tokens', type: 'number' }, { name: 'return-full-text', type: 'switch' }, { name: 'wait-for-model', type: 'switch' }],
  },
  'in-memory': {
    'langchain4j-embedding-store': [],
  },
  'local-ai': {
    'langchain4j-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-embedding-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-language-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-streaming-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-streaming-language-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'max-tokens', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
  },
  'milvus': {
    'langchain4j-embedding-store': [{ name: 'host', type: 'text' }, { name: 'port', type: 'number' }, { name: 'collection-name', type: 'text' }, { name: 'index-type', type: 'select' }, { name: 'metric-type', type: 'select' }, { name: 'uri', type: 'text' }, { name: 'token', type: 'text' }, { name: 'username', type: 'text' }, { name: 'password', type: 'text' }, { name: 'consistency-level', type: 'select' }, { name: 'retrieve-embeddings-on-search', type: 'switch' }, { name: 'auto-flush-on-insert', type: 'switch' }, { name: 'database-name', type: 'text' }, { name: 'dimension', type: 'number' }],
  },
  'neo4j': {
    'langchain4j-embedding-store': [{ name: 'uri', type: 'text' }, { name: 'username', type: 'text' }, { name: 'password', type: 'text' }, { name: 'label', type: 'text' }, { name: 'embedding-property', type: 'text' }, { name: 'id-property', type: 'text' }, { name: 'metadata-prefix', type: 'text' }, { name: 'text-property', type: 'text' }, { name: 'index-name', type: 'text' }, { name: 'database-name', type: 'text' }, { name: 'dimension', type: 'number' }],
  },
  'ollama': {
    'langchain4j-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'repeat-penalty', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'num-predict', type: 'number' }, { name: 'num-ctx', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'format', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }],
    'langchain4j-embedding-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }],
    'langchain4j-language-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'repeat-penalty', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'num-predict', type: 'number' }, { name: 'num-ctx', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'format', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }],
    'langchain4j-streaming-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'repeat-penalty', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'num-predict', type: 'number' }, { name: 'num-ctx', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'format', type: 'text' }, { name: 'timeout', type: 'duration' }],
    'langchain4j-streaming-language-model': [{ name: 'base-url', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'repeat-penalty', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'num-predict', type: 'number' }, { name: 'num-ctx', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'format', type: 'text' }, { name: 'timeout', type: 'duration' }],
  },
  'openai': {
    'langchain4j-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'organization-id', type: 'text' }, { name: 'model-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'max-tokens', type: 'number' }, { name: 'presence-penalty', type: 'number' }, { name: 'frequency-penalty', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'user', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-embedding-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'organization-id', type: 'text' }, { name: 'model-name', type: 'text', required: true }, { name: 'user', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-language-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'organization-id', type: 'text' }, { name: 'model-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-streaming-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'organization-id', type: 'text' }, { name: 'model-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'max-tokens', type: 'number' }, { name: 'presence-penalty', type: 'number' }, { name: 'frequency-penalty', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'user', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-streaming-language-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'organization-id', type: 'text' }, { name: 'model-name', type: 'text', required: true }, { name: 'temperature', type: 'number' }, { name: 'timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-tokenizer': [{ name: 'api-key', type: 'text', required: true }],
  },
  'qdrant': {
    'langchain4j-embedding-store': [{ name: 'host', type: 'text' }, { name: 'port', type: 'number' }, { name: 'collection-name', type: 'text' }, { name: 'use-tls', type: 'switch' }, { name: 'payload-text-key', type: 'text' }, { name: 'api-key', type: 'text' }],
  },
  'qianfan': {
    'langchain4j-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'secret-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'penalty-score', type: 'number' }, { name: 'max-output-tokens', type: 'number' }, { name: 'stop', type: 'tags' }, { name: 'endpoint', type: 'text' }, { name: 'response-format', type: 'text' }, { name: 'user-id', type: 'text' }, { name: 'system', type: 'text' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-embedding-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'secret-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'user', type: 'text' }, { name: 'endpoint', type: 'text' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-language-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'secret-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'penalty-score', type: 'number' }, { name: 'endpoint', type: 'text' }, { name: 'max-retries', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-streaming-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'secret-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'penalty-score', type: 'number' }, { name: 'endpoint', type: 'text' }, { name: 'response-format', type: 'text' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-streaming-language-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'secret-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'penalty-score', type: 'number' }, { name: 'endpoint', type: 'text' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
  },
  'qwen': {
    'langchain4j-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'enable-search', type: 'switch' }, { name: 'seed', type: 'number' }, { name: 'repetition-penalty', type: 'number' }, { name: 'stops', type: 'tags' }, { name: 'max-tokens', type: 'number' }],
    'langchain4j-embedding-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }],
    'langchain4j-language-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'enable-search', type: 'switch' }, { name: 'seed', type: 'number' }, { name: 'repetition-penalty', type: 'number' }, { name: 'stops', type: 'tags' }, { name: 'max-tokens', type: 'number' }],
    'langchain4j-streaming-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'enable-search', type: 'switch' }, { name: 'seed', type: 'number' }, { name: 'repetition-penalty', type: 'number' }, { name: 'stops', type: 'tags' }, { name: 'max-tokens', type: 'number' }],
    'langchain4j-streaming-language-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'enable-search', type: 'switch' }, { name: 'seed', type: 'number' }, { name: 'repetition-penalty', type: 'number' }, { name: 'stops', type: 'tags' }, { name: 'max-tokens', type: 'number' }],
    'langchain4j-tokenizer': [{ name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }],
  },
  'redis': {
    'langchain4j-embedding-store': [{ name: 'host', type: 'text' }, { name: 'port', type: 'number' }, { name: 'user', type: 'text' }, { name: 'password', type: 'text' }, { name: 'index-name', type: 'text' }, { name: 'metadata-keys', type: 'tags' }, { name: 'dimension', type: 'number' }],
  },
  'vertex-ai-gemini': {
    'langchain4j-chat-model': [{ name: 'project', type: 'text', required: true }, { name: 'location', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'max-output-tokens', type: 'number' }, { name: 'seed', type: 'number' }, { name: 'max-retries', type: 'number' }, { name: 'response-mime-type', type: 'text' }, { name: 'use-google-search', type: 'switch' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-streaming-chat-model': [{ name: 'project', type: 'text', required: true }, { name: 'location', type: 'text' }, { name: 'model-name', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'top-k', type: 'number' }, { name: 'max-output-tokens', type: 'number' }, { name: 'response-mime-type', type: 'text' }, { name: 'use-google-search', type: 'switch' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
  },
  'voyage-ai': {
    'langchain4j-embedding-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }, { name: 'input-type', type: 'text' }, { name: 'truncation', type: 'switch' }, { name: 'encoding-format', type: 'text' }, { name: 'timeout', type: 'duration' }, { name: 'max-retries', type: 'number' }, { name: 'max-segments-per-batch', type: 'number' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
  },
  'weaviate': {
    'langchain4j-embedding-store': [{ name: 'scheme', type: 'text' }, { name: 'host', type: 'text' }, { name: 'port', type: 'number' }, { name: 'api-key', type: 'text' }, { name: 'object-class', type: 'text' }, { name: 'use-grpc-for-inserts', type: 'switch' }, { name: 'secured-grpc', type: 'switch' }, { name: 'grpc-port', type: 'number' }, { name: 'avoid-dups', type: 'switch' }, { name: 'consistency-level', type: 'text' }, { name: 'metadata-keys', type: 'tags' }],
  },
  'zhipu-ai': {
    'langchain4j-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'stops', type: 'tags' }, { name: 'max-token', type: 'number' }, { name: 'max-retries', type: 'number' }, { name: 'call-timeout', type: 'duration' }, { name: 'connect-timeout', type: 'duration' }, { name: 'read-timeout', type: 'duration' }, { name: 'write-timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-embedding-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model', type: 'text' }, { name: 'dimensions', type: 'number' }, { name: 'max-retries', type: 'number' }, { name: 'call-timeout', type: 'duration' }, { name: 'connect-timeout', type: 'duration' }, { name: 'read-timeout', type: 'duration' }, { name: 'write-timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
    'langchain4j-streaming-chat-model': [{ name: 'base-url', type: 'text' }, { name: 'api-key', type: 'text', required: true }, { name: 'model', type: 'text' }, { name: 'temperature', type: 'number' }, { name: 'top-p', type: 'number' }, { name: 'stops', type: 'tags' }, { name: 'max-token', type: 'number' }, { name: 'call-timeout', type: 'duration' }, { name: 'connect-timeout', type: 'duration' }, { name: 'read-timeout', type: 'duration' }, { name: 'write-timeout', type: 'duration' }, { name: 'log-requests', type: 'switch' }, { name: 'log-responses', type: 'switch' }],
  },
};

/** 取某类型下某 provider 的 options 字段定义；未知组合返回空数组 */
export function optionsFields(type, provider) {
    const byType = PROVIDER_FIELDS[type];
    return (byType && byType[provider]) || [];
}

/** 某类型已声明字段定义的 provider 短名集合 */
export function providerNames(type) {
    return Object.keys(PROVIDER_FIELDS[type] || {});
}

/** 字段名是否需要按密码输入渲染 */
const SENSITIVE = ['api-key', 'secret-key', 'password', 'token', 'access-token', 'github-token'];

export function isSensitive(name) {
    return SENSITIVE.indexOf(name) >= 0;
}

/**
 * 枚举型字段的候选值：来自后端 SDK 枚举的常用取值。
 * 前端一律允许直接输入，因此 SDK 新增取值不会阻塞配置。
 */
export const SELECT_HINTS = {
    'index-type': ['FLAT', 'IVF_FLAT', 'IVF_SQ8', 'IVF_PQ', 'HNSW', 'ANNOY', 'DISKANN', 'AUTOINDEX'],
    'metric-type': ['L2', 'IP', 'COSINE'],
    'consistency-level': ['STRONG', 'SESSION', 'BOUNDED', 'EVENTUALLY']
};

