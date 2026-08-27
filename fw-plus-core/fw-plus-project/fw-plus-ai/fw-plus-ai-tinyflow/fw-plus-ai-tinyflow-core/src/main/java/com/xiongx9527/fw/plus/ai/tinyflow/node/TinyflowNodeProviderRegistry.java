package com.xiongx9527.fw.plus.ai.tinyflow.node;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.tinyflow.core.parser.NodeParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TinyflowNodeProviderRegistry {

	private final Map<String, TinyflowNodeProvider> providers;
	private final Map<String, NodeParser<?>> parsers;

	public TinyflowNodeProviderRegistry(Collection<TinyflowNodeProvider> candidates) {
		Map<String, List<TinyflowNodeProvider>> providersByType = groupByType(candidates);
		Map<String, TinyflowNodeProvider> selectedProviders = new LinkedHashMap<String, TinyflowNodeProvider>();
		Map<String, NodeParser<?>> selectedParsers = new LinkedHashMap<String, NodeParser<?>>();
		for (Map.Entry<String, List<TinyflowNodeProvider>> entry : providersByType.entrySet()) {
			TinyflowNodeProvider selected = select(entry.getKey(), entry.getValue());
			NodeParser<?> parser = selected.getParser();
			if (parser == null) {
				throw new TinyflowException("Tinyflow node provider parser is null: " + entry.getKey());
			}
			selectedProviders.put(entry.getKey(), selected);
			selectedParsers.put(entry.getKey(), parser);
		}
		this.providers = Collections.unmodifiableMap(selectedProviders);
		this.parsers = Collections.unmodifiableMap(selectedParsers);
	}

	public TinyflowNodeProvider getProvider(String type) {
		return providers.get(type);
	}

	public Map<String, TinyflowNodeProvider> getProviders() {
		return providers;
	}

	public Map<String, NodeParser<?>> getParsers() {
		return parsers;
	}

	private Map<String, List<TinyflowNodeProvider>> groupByType(Collection<TinyflowNodeProvider> candidates) {
		Map<String, List<TinyflowNodeProvider>> grouped = new LinkedHashMap<String, List<TinyflowNodeProvider>>();
		if (candidates == null) {
			return grouped;
		}
		for (TinyflowNodeProvider provider : candidates) {
			if (provider == null) {
				throw new TinyflowException("Tinyflow node provider must not be null");
			}
			String type = provider.getType();
			if (type == null || type.trim().length() == 0) {
				throw new TinyflowException("Tinyflow node provider type must not be blank: "
						+ provider.getClass().getName());
			}
			type = type.trim();
			List<TinyflowNodeProvider> typedProviders = grouped.get(type);
			if (typedProviders == null) {
				typedProviders = new ArrayList<TinyflowNodeProvider>();
				grouped.put(type, typedProviders);
			}
			typedProviders.add(provider);
		}
		return grouped;
	}

	private TinyflowNodeProvider select(String type, List<TinyflowNodeProvider> candidates) {
		TinyflowNodeProvider selected = null;
		int highestOrder = Integer.MIN_VALUE;
		List<TinyflowNodeProvider> highest = new ArrayList<TinyflowNodeProvider>();
		for (TinyflowNodeProvider candidate : candidates) {
			if (selected == null || candidate.getOrder() > highestOrder) {
				selected = candidate;
				highestOrder = candidate.getOrder();
				highest.clear();
				highest.add(candidate);
			} else if (candidate.getOrder() == highestOrder) {
				highest.add(candidate);
			}
		}
		if (highest.size() > 1) {
			List<String> names = new ArrayList<String>();
			for (TinyflowNodeProvider provider : highest) {
				names.add(provider.getClass().getName());
			}
			throw new TinyflowException("Conflicting Tinyflow node providers for type " + type
					+ " at order " + highestOrder + ": " + names);
		}
		return selected;
	}
}
