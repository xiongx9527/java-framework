package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AskData 长期记忆契约。
 */
public interface AskDataLongTermMemory {

	void remember(AskDataMemoryEntry entry);

	List<AskDataMemoryEntry> find(AskDataProjectContext context, String datasourceKey, String semanticVersion);

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
