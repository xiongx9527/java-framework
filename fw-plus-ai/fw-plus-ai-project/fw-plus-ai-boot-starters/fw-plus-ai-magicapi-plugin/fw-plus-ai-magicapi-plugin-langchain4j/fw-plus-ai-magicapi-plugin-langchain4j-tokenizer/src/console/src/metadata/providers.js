/**
 * provider → options 字段元数据表（本类型：langchain4j-tokenizer）。
 *
 * 字段由后端各 provider 工厂实际读取的 options 键生成，顺序与工厂读取顺序一致（必填在前）。
 * 字段类型：text / number / switch / select / duration / tags。
 * 敏感字段（api-key、secret-key、password、token 等）由组件统一按密码框渲染。
 * 与后端的一致性由 test/tokenizer-console-metadata.test.cjs 交叉校验。
 */
export const PROVIDER_FIELDS = {
    'azure-openai': [],
    'openai': [{ name: 'api-key', type: 'text', required: true }],
    'qwen': [{ name: 'api-key', type: 'text', required: true }, { name: 'model-name', type: 'text' }],
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
