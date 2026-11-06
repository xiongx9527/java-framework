package com.xiongx9527.fw.plus.jdbc.execution;

/**
 * 列级脱敏/改写钩子（§6.38 列级权限）。
 *
 * <p>执行器在读出每格值后、收口为 {@link QueryResult} 前调用；返回 null 或脱敏后的替代值。
 * 由产品侧按可见 schema 注入（如身份证/手机号打码），引擎层只负责调用，不内置任何脱敏规则。</p>
 */
@FunctionalInterface
public interface ColumnMasker {

    /**
     * 对单格值脱敏。
     *
     * @param columnName 列名（来自结果集元数据）
     * @param rawValue   原始值（可能为 null）
     * @return 脱敏后的值；返回 null 表示以 null 呈现
     */
    Object mask(String columnName, Object rawValue);
}
