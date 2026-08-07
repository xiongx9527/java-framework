package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import java.util.List;

public interface MagicTinyflowKnowledgeOptionProvider {

	List<Option> listModels(int limit);

	List<Option> listVectors(int limit);

	List<Option> listS3(int limit);

	final class Option {
		private final String key;
		private final String name;
		private final String type;
		private final String provider;

		public Option(String key, String name, String type, String provider) {
			this.key = key;
			this.name = name;
			this.type = type;
			this.provider = provider;
		}

		public String getKey() { return key; }
		public String getName() { return name; }
		public String getType() { return type; }
		public String getProvider() { return provider; }

		@Override public String toString() {
			return "Option{key='" + key + "', name='" + name + "', type='" + type
					+ "', provider='" + provider + "'}";
		}
	}
}
