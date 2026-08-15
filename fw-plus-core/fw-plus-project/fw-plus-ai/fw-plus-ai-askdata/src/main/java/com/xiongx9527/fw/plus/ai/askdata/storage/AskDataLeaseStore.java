package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * 以项目和操作人隔离的执行租约 SPI。
 *
 * <p>集群实现必须以 {@code projectId + operatorId + resourceId} 为键，通过数据库条件更新、Redis SET NX/Lua
 * 或等价原子操作竞争租约。释放时必须校验 owner，禁止一个实例释放另一个实例的租约。默认内存实现仅适合
 * 单进程；租约过期只允许恢复可重入节点，不能据此重放可能已产生副作用的 SQL 或工具。</p>
 */
public interface AskDataLeaseStore {

	/** 原子取得或续租当前 owner 的执行权。 */
	boolean tryAcquire(AskDataProjectContext context, String resourceId, String ownerId,
			long leaseDurationMillis);

	/** 仅在 owner 匹配时原子释放租约。 */
	boolean release(AskDataProjectContext context, String resourceId, String ownerId);
}
