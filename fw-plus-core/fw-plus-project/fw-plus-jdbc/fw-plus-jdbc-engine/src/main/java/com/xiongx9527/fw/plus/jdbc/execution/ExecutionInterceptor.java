package com.xiongx9527.fw.plus.jdbc.execution;

/**
 * 执行前后拦截点（§6.38 执行钩子）。用于审计、限流、列级脱敏等横切关注点。
 *
 * <p>拦截器对执行链路只读：{@link #beforeExecute} 可校验/拒绝（抛异常即中断执行），
 * {@link #afterExecute} 可包装或替换结果（如脱敏），默认实现透传。</p>
 */
public interface ExecutionInterceptor {

    /** 执行前触发；可在此做审计登记或显式拒绝（抛 {@link SqlExecutionException}）。 */
    default void beforeExecute(String sql, ExecutionSettings settings) {
    }

    /** 执行后触发；返回（可能被包装的）结果。默认透传。 */
    default QueryResult afterExecute(QueryResult result, ExecutionSettings settings) {
        return result;
    }
}
