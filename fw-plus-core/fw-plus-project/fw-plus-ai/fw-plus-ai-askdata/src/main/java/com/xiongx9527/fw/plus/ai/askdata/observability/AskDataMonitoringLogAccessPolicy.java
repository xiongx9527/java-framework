package com.xiongx9527.fw.plus.ai.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData 管理员监控日志读取权限 SPI。
 *
 * <p>权限适配层负责将宿主系统的角色或授权结果传入 {@code projectAdministrator}。Core 只
 * 定义厂商无关的授权边界：项目管理员可以读取本项目日志；普通操作人默认不能读取管理员
 * 日志，且任何调用方都不能跨项目读取日志。</p>
 */
public interface AskDataMonitoringLogAccessPolicy {

	/**
	 * 判断请求人是否可以读取一条监控日志。
	 *
	 * <p>实现必须先比较请求人与日志的项目标识。即使请求人具备管理员身份，也仅能读取自己
	 * 所属项目的日志；普通操作人读取管理员日志时应默认拒绝。</p>
	 *
	 * @param requester 请求人的项目和操作人上下文，不能为空
	 * @param projectAdministrator 请求人是否已被宿主授权为该项目管理员
	 * @param log 待读取的已脱敏监控日志，不能为空
	 * @return 已获授权时返回 {@code true}，否则返回 {@code false}
	 */
	boolean canRead(AskDataProjectContext requester, boolean projectAdministrator, AskDataMonitoringLog log);

	/**
	 * 创建默认的项目管理员读取策略。
	 *
	 * <p>该参考实现仅允许已获授权的项目管理员读取同项目日志。它有意不为普通操作人提供
	 * 读取管理员日志的默认通道；需要更细粒度委派时，宿主系统应实现本 SPI 并明确授权。</p>
	 *
	 * @return 仅允许同项目管理员读取的策略实例
	 */
	static AskDataMonitoringLogAccessPolicy projectAdministratorOnly() {
		return new AskDataMonitoringLogAccessPolicy() {
			@Override
			public boolean canRead(AskDataProjectContext requester, boolean projectAdministrator,
					AskDataMonitoringLog log) {
				if (requester == null || log == null || !projectAdministrator) {
					return false;
				}
				return requester.getProjectId().equals(log.getContext().getProjectId());
			}
		};
	}
}
