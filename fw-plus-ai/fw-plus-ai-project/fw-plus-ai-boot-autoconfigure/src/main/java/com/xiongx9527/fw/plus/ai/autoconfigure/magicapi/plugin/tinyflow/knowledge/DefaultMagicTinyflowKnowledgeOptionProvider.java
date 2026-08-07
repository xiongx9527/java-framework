package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.VectorDataSourceInfo;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefaultMagicTinyflowKnowledgeOptionProvider implements MagicTinyflowKnowledgeOptionProvider {

	private static final int MAX_LIMIT = 200;
	private final MagicDynamicModelDataSource modelDataSource;
	private final MagicDynamicVectorDataSource vectorDataSource;
	private final MagicDynamicS3DataSource s3DataSource;

	public DefaultMagicTinyflowKnowledgeOptionProvider(MagicDynamicModelDataSource modelDataSource,
			MagicDynamicVectorDataSource vectorDataSource, MagicDynamicS3DataSource s3DataSource) {
		if (modelDataSource == null || vectorDataSource == null || s3DataSource == null) {
			throw new IllegalArgumentException("Knowledge option data sources must not be null");
		}
		this.modelDataSource = modelDataSource;
		this.vectorDataSource = vectorDataSource;
		this.s3DataSource = s3DataSource;
	}

	@Override
	public List<Option> listModels(int limit) {
		validateLimit(limit);
		Map<String, String> names = modelNames(modelDataSource.datasourceNodes());
		List<Option> options = new ArrayList<Option>();
		for (String key : modelDataSource.keys()) {
			AiModel model = modelDataSource.getModel(key);
			if (model != null && model.getType() == AiModelType.EMBEDDING) {
				options.add(new Option(key, name(names, key), model.getType().name(), model.getProvider()));
			}
		}
		return sortedLimited(options, limit);
	}

	@Override
	public List<Option> listVectors(int limit) {
		validateLimit(limit);
		Map<String, String> names = vectorNames(vectorDataSource.datasourceNodes());
		List<Option> options = new ArrayList<Option>();
		for (String key : vectorDataSource.keys()) {
			AiEmbeddingStore store = vectorDataSource.get(key);
			if (store != null) {
				String type = store.getType() == null ? "VECTOR" : store.getType().name();
				options.add(new Option(key, name(names, key), type, store.getProvider()));
			}
		}
		return sortedLimited(options, limit);
	}

	@Override
	public List<Option> listS3(int limit) {
		validateLimit(limit);
		List<Option> options = new ArrayList<Option>();
		for (S3DataSourceInfo info : safe(s3DataSource.datasourceNodes())) {
			if (info != null && text(info.getKey()) && !"false".equalsIgnoreCase(info.getEnableStorage())) {
				options.add(new Option(info.getKey(), text(info.getName()) ? info.getName() : info.getKey(),
						"OBJECT_STORAGE", info.getPlatform()));
			}
		}
		return sortedLimited(options, limit);
	}

	private Map<String, String> modelNames(List<ModelDataSourceInfo> infos) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (ModelDataSourceInfo info : safe(infos)) {
			if (info != null && text(info.getKey()) && text(info.getName())) result.put(info.getKey(), info.getName());
		}
		return result;
	}

	private Map<String, String> vectorNames(List<VectorDataSourceInfo> infos) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (VectorDataSourceInfo info : safe(infos)) {
			if (info != null && info.isEnabled() && text(info.getKey()) && text(info.getName())) {
				result.put(info.getKey(), info.getName());
			}
		}
		return result;
	}

	private String name(Map<String, String> names, String key) {
		String exact = names.get(key);
		if (text(exact)) return exact;
		for (Map.Entry<String, String> entry : names.entrySet()) {
			if (key.startsWith(entry.getKey() + ".")) return entry.getValue() + " / " + key.substring(entry.getKey().length() + 1);
		}
		return key;
	}

	private List<Option> sortedLimited(List<Option> source, int limit) {
		Collections.sort(source, new Comparator<Option>() {
			@Override
			public int compare(Option left, Option right) {
				return left.getKey().compareTo(right.getKey());
			}
		});
		if (source.size() > limit) source = new ArrayList<Option>(source.subList(0, limit));
		return Collections.unmodifiableList(source);
	}

	private void validateLimit(int limit) {
		if (limit < 1 || limit > MAX_LIMIT) throw new IllegalArgumentException("limit must be between 1 and 200");
	}

	private boolean text(String value) {
		return value != null && value.trim().length() > 0;
	}

	private <T> List<T> safe(List<T> value) {
		return value == null ? Collections.<T>emptyList() : value;
	}
}
