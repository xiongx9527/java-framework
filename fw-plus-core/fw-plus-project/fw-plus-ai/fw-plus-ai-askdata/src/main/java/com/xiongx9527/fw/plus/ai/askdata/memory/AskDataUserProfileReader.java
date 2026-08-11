package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.Map;

/**
 * 当前操作人的脱敏推荐画像读取 SPI。
 *
 * <p>实现必须使用 {@code projectId + operatorId} 作为不可拆分的隔离键，不得按用户名、邮箱等可变字段猜测
 * 身份。返回值只能包含允许参与推荐排序的脱敏特征，不能包含凭据、原始问数历史或其他操作人的聚合特征。
 * 实现应支持并发读取；集群缓存和持久化后端必须执行同样的权限与撤销策略。</p>
 */
public interface AskDataUserProfileReader {

	/**
	 * 读取当前操作人在当前项目内的画像特征。
	 *
	 * @param context 当前项目和已认证操作人
	 * @return 特征名到脱敏值的映射；没有画像时返回空映射
	 */
	Map<String, String> read(AskDataProjectContext context);
}
