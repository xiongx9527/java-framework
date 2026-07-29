package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * 宿主系统监控日志管理员授权解析 SPI。
 *
 * <p>调用方提供的访问角色只作为宿主授权判断的输入，不能单独代表管理员资格。宿主应同时根据
 * 请求人、目标 scope 和角色声明返回授权结果。</p>
 */
public interface AskDataMonitoringLogAccessAuthorizer {

    /**
     * 判断请求人是否被宿主系统授权为指定 scope 的管理员。
     *
     * @param requester 请求人的项目和操作人上下文
     * @param scope 本次查询的显式目标 scope
     * @param role 调用方声明的访问角色
     * @return 已被宿主确认时返回 {@code true}
     */
    boolean isAdministrator(AskDataProjectContext requester, AskDataProjectContext scope,
            AskDataMonitoringLogAccessRole role);

    /**
     * 创建默认的拒绝授权解析器，避免未接入宿主 IAM 时信任调用方角色。
     *
     * @return 恒拒绝的授权解析器
     */
    static AskDataMonitoringLogAccessAuthorizer denyAll() {
        return new AskDataMonitoringLogAccessAuthorizer() {
            @Override
            public boolean isAdministrator(AskDataProjectContext requester, AskDataProjectContext scope,
                    AskDataMonitoringLogAccessRole role) {
                return false;
            }
        };
    }
}
