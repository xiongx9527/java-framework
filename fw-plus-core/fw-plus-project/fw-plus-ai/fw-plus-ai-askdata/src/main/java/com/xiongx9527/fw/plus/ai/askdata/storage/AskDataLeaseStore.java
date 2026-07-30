package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/** 以项目和操作人隔离的执行租约 SPI。 */
public interface AskDataLeaseStore {

	boolean tryAcquire(AskDataProjectContext context, String resourceId, String ownerId, long leaseDurationMillis);

	boolean release(AskDataProjectContext context, String resourceId, String ownerId);
}
