package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.model.Langchain4jContentRetrieverInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.rag.content.retriever.ContentRetriever;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * provider 短名 → 工厂的分发表：表外短名在构建期拒绝并给出可用短名。
 */
public class ContentRetrieverProviderRegistry {

	private final Map<String, ContentRetrieverProvider> providers = new LinkedHashMap<>();

	public void register(ContentRetrieverProvider provider) {
		providers.put(provider.provider(), provider);
	}

	/** 已注册的 provider 短名（不可修改视图） */
	public Set<String> providers() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(providers.keySet()));
	}

	public ContentRetriever build(Langchain4jContentRetrieverInfo info, ContentRetrieverContext context) {
		ContentRetrieverProvider provider = providers.get(info.getProvider());
		if (provider == null) {
			throw new IllegalArgumentException(String.format(
					"资源 %s[%s] 的 provider '%s' 未注册，可用 provider: %s",
					Langchain4jContentRetrieverInfo.TYPE, info.getKey(), info.getProvider(), providers()));
		}
		Langchain4jOptionsReader reader = new Langchain4jOptionsReader(
				Langchain4jContentRetrieverInfo.TYPE, info.getKey(), info.getOptions());
		return provider.build(reader, context);
	}
}
