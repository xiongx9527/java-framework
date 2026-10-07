/**
 * redis datasource 表单字段元数据（本类型：redis-datasource）。
 *
 * 字段与后端 RedisDataSourceInfo（含 MagicEntity 继承的 name）实际字段一一对应，
 * 必填口径与后端 BaseService.validateField 一致（name/key/hostname/port）。
 * 字段类型：text / number / select / json。
 * 敏感字段（password 等）由组件统一按密码框渲染。
 * 枚举候选值集中在 SELECT_HINTS，展示名由 i18n redis.datasource.extra.* 提供。
 * 与后端的一致性由 test/console-metadata.test.cjs 交叉校验。
 */
export const DATASOURCE_FIELDS = [
    { name: 'name', type: 'text', required: true },
    { name: 'key', type: 'text', required: true },
    { name: 'hostname', type: 'text', required: true },
    { name: 'port', type: 'number', required: true },
    { name: 'username', type: 'text' },
    { name: 'password', type: 'text' },
    { name: 'database', type: 'text' },
    { name: 'mode', type: 'select' },
    { name: 'clientType', type: 'select' },
    { name: 'properties', type: 'json' }
];

/** 敏感字段名单：统一按密码框渲染，不回显 */
const SENSITIVE = ['password'];

/** 字段名是否需要按密码输入渲染 */
export function isSensitive(name) {
    return SENSITIVE.indexOf(name) >= 0;
}

/**
 * 枚举型字段的候选值：与后端运行时支持的取值一致；
 * 展示名由组件经 i18n redis.datasource.extra.<field>.<value> 解析。
 */
export const SELECT_HINTS = {
    mode: ['standalone', 'sentinel', 'cluster'],
    clientType: ['lettuce', 'jedis']
};
