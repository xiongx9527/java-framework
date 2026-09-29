package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.MagicDynamicLangchain4jToolProviderDataSource;
import dev.langchain4j.service.tool.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Langchain4jToolProviderModuleTest {

	private MagicDynamicLangchain4jToolProviderDataSource dataSource;

	private Langchain4jToolProviderModule module;

	@BeforeEach
	void setUp() {
		dataSource = mock(MagicDynamicLangchain4jToolProviderDataSource.class);
		module = new Langchain4jToolProviderModule(dataSource);
	}

	@Test
	void shouldReturnProviderByKey() {
		ToolProvider provider = mock(ToolProvider.class);
		when(dataSource.get("evaluation")).thenReturn(provider);

		assertThat(module.getDynamicAttribute("evaluation")).isSameAs(provider);
	}

	@Test
	void shouldThrowWhenKeyMissing() {
		when(dataSource.keys()).thenReturn(java.util.Collections.singleton("other"));

		assertThatThrownBy(() -> module.getDynamicAttribute("missing"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("missing");
	}

	@Test
	void shouldThrowWhenKeyBlank() {
		assertThatThrownBy(() -> module.getDynamicAttribute(""))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> module.getDynamicAttribute(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectDynamicSet() {
		assertThatThrownBy(() -> module.setDynamicAttribute("evaluation", mock(ToolProvider.class)))
				.isInstanceOf(UnsupportedOperationException.class);
	}
}
