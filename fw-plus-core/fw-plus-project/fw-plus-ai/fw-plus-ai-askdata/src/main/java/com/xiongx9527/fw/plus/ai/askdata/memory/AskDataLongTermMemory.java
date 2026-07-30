package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AskData 长期记忆契约。
 */
public interface AskDataLongTermMemory {

	/**
	 * 保存一条已绑定隔离上下文的长期记忆。
	 *
	 * <p>实现必须连同条目中的项目、操作人、数据源和语义版本一并保存，不能把任一维度降级为全局记忆。
	 * 生产实现应提供满足部署形态的一致性、持久化和访问控制；本接口不允许以缺失字段或其他操作人的内容
	 * 作为回退。</p>
	 *
	 * @param entry 待保存的完整记忆条目，不能为空
	 * @throws IllegalArgumentException 当 {@code entry} 为 {@code null} 或条目内容不完整时抛出
	 */
	void remember(AskDataMemoryEntry entry);

	/**
	 * 查询与当前项目、操作人、数据源及语义版本完全匹配的长期记忆。
	 *
	 * <p>查询结果必须同时匹配 {@code context} 中的项目和操作人，以及指定的数据源、语义版本；任何一维
	 * 不匹配的条目都不得返回。无匹配时返回空列表，不得为了提高命中率回退到其他操作人、其他项目、
	 * 其他数据源或其他语义版本的记忆。</p>
	 *
	 * @param context 当前请求的项目与操作人隔离上下文，不能为空
	 * @param datasourceKey 数据源唯一标识，不能为空白
	 * @param semanticVersion 语义模型版本，不能为空白
	 * @return 完全匹配的记忆条目；没有匹配项时返回空列表
	 * @throws IllegalArgumentException 当任一参数为空或空白时抛出
	 */
	List<AskDataMemoryEntry> find(AskDataProjectContext context, String datasourceKey, String semanticVersion);

	/**
	 * 创建用于契约测试和单节点开发的内存参考实现。
	 *
	 * @return 不具备跨进程持久化能力的内存实现
	 */
	static AskDataLongTermMemory inMemory() {
		return new InMemory();
	}

	/**
	 * 用于契约测试和单节点开发的内存参考实现。
	 */
	final class InMemory implements AskDataLongTermMemory {

		private final List<AskDataMemoryEntry> entries = Collections.synchronizedList(new ArrayList<AskDataMemoryEntry>());

		@Override
		public void remember(AskDataMemoryEntry entry) {
			if (entry == null) {
				throw new IllegalArgumentException("entry 不能为空");
			}
			entries.add(entry);
		}

		@Override
		public List<AskDataMemoryEntry> find(AskDataProjectContext context, String datasourceKey, String semanticVersion) {
			if (context == null) {
				throw new IllegalArgumentException("context 不能为空");
			}
			String normalizedDatasourceKey = requireNonBlank(datasourceKey, "datasourceKey");
			String normalizedSemanticVersion = requireNonBlank(semanticVersion, "semanticVersion");
			List<AskDataMemoryEntry> matched = new ArrayList<AskDataMemoryEntry>();
			synchronized (entries) {
				for (AskDataMemoryEntry entry : entries) {
					AskDataProjectContext entryContext = entry.getContext();
					if (context.getProjectId().equals(entryContext.getProjectId())
							&& context.getOperatorId().equals(entryContext.getOperatorId())
							&& normalizedDatasourceKey.equals(entry.getDatasourceKey())
							&& normalizedSemanticVersion.equals(entry.getSemanticVersion())) {
						matched.add(entry);
					}
				}
			}
			return matched;
		}

		private static String requireNonBlank(String value, String name) {
			if (value == null || value.trim().length() == 0) {
				throw new IllegalArgumentException(name + " 不能为空白");
			}
			return value.trim();
		}
	}
}
