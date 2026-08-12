package com.xiongx9527.fw.plus.ai.askdata.security;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * 轻量资源权限判定 SPI。
 *
 * <p>实现必须以宿主系统已认证的项目和操作人为输入，缺少身份、未知能力、后端异常或无法判定时均应拒绝。
 * 实例可能被并发调用；集群缓存必须包含项目、操作人、能力、资源和授权版本。该接口只做布尔判定，不能替代
 * 需要对象、行列范围和审计来源的 {@link AskDataGovernanceProvider}，也不能作为 SQL 最终放行器。</p>
 */
public interface AskDataPermissionEvaluator {

	boolean isAllowed(AskDataProjectContext context, String capability, String resource);
}
