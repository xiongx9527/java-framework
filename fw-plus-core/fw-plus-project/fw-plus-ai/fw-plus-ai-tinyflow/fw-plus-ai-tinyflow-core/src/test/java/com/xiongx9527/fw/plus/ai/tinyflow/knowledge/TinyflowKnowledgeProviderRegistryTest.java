package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowKnowledgeProviderRegistryTest {

	@Test
	public void shouldRejectDuplicateProviderKey() {
		assertThrows(TinyflowException.class, () ->
				new TinyflowKnowledgeProviderRegistry(Arrays.asList(provider("dify"), provider("dify"))));
	}

	@Test
	public void shouldRequireRegisteredProvider() {
		TinyflowKnowledgeProvider provider = provider("dify");
		TinyflowKnowledgeProviderRegistry registry = new TinyflowKnowledgeProviderRegistry(
				Collections.singletonList(provider));

		assertSame(provider, registry.require("dify"));
		assertThrows(TinyflowException.class, () -> registry.require("missing"));
	}

	@Test
	public void shouldReportManagementCapability() {
		TinyflowKnowledgeProviderRegistry registry = new TinyflowKnowledgeProviderRegistry(Arrays.asList(
				provider("readonly"), managementProvider("managed")));

		assertFalse(registry.supportsManagement("readonly"));
		assertTrue(registry.supportsManagement("managed"));
	}

	private TinyflowKnowledgeProvider provider(final String key) {
		return new TinyflowKnowledgeProvider() {
			@Override
			public String getKey() {
				return key;
			}

			@Override
			public TinyflowKnowledgeCapabilities capabilities() {
				return TinyflowKnowledgeCapabilities.retrievalOnly();
			}

			@Override
			public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
				return TinyflowKnowledgeResult.empty();
			}
		};
	}

	private TinyflowKnowledgeManagementProvider managementProvider(final String key) {
		return new TinyflowKnowledgeManagementProvider() {
			@Override
			public String getKey() {
				return key;
			}

			@Override
			public TinyflowKnowledgeCapabilities capabilities() {
				return TinyflowKnowledgeCapabilities.managed();
			}

			@Override
			public TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request) {
				return TinyflowKnowledgeResult.empty();
			}

			@Override
			public TinyflowKnowledgeDefinition create(TinyflowKnowledgeDefinition definition) {
				return definition;
			}

			@Override
			public TinyflowKnowledgeDefinition update(TinyflowKnowledgeDefinition definition) {
				return definition;
			}

			@Override
			public void delete(String tenantId, String knowledgeId) {
			}

			@Override
			public TinyflowIngestionTask importDocument(TinyflowDocumentImport request) {
				return null;
			}
		};
	}
}
