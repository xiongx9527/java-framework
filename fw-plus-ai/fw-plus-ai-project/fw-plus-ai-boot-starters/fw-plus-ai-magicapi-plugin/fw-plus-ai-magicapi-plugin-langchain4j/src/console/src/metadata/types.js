/**
 * 七类 LangChain4j 模型资源：短类型与后端 Langchain4jModelTypes 一一对应，
 * 与 `langchain4j-` 前缀拼成 datasource type（同时也是 magic-api 资源类型标识）。
 */
export const DATASOURCE_TYPES = [
    'chat-model',
    'streaming-chat-model',
    'language-model',
    'streaming-language-model',
    'embedding-model',
    'tokenizer',
    'embedding-store'
];

/** datasource type 前缀 */
export const TYPE_PREFIX = 'langchain4j-';

/** 短类型 → 完整 datasource type */
export function fullType(short) {
    return TYPE_PREFIX + short;
}
