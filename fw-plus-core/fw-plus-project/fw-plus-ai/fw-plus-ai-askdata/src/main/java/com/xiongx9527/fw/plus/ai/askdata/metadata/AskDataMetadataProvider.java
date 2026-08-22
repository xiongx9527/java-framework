package com.xiongx9527.fw.plus.ai.askdata.metadata;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * 数据源元数据读取 SPI。
 *
 * <p>实现必须以当前项目和已认证操作人为边界，只返回其可见的数据源、表、列和语义资源。调用方可以并发
 * 请求不同数据源，实现应无状态或线程安全；集群缓存必须包含项目、操作人、数据源和授权版本。禁止返回
 * 数据库凭据、未授权样例值，或因元数据服务异常而回退到全量 schema。</p>
 */
public interface AskDataMetadataProvider {

	/**
	 * 加载当前范围内的元数据快照。
	 *
	 * @param context 当前项目和已认证操作人
	 * @param datasourceKey 业务授权的数据源稳定 key
	 * @return 已脱敏且已按权限收窄的不可变元数据
	 */
	AskDataMetadata load(AskDataProjectContext context, String datasourceKey);
}
