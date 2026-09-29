package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicApiToolProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.MagicToolInvoker;
import dev.langchain4j.service.tool.ToolProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicDynamicLangchain4jToolProviderDataSourceTest {

	private MagicDynamicLangchain4jToolProviderDataSource dataSource;

	@BeforeEach
	void setUp() {
		dataSource = new MagicDynamicLangchain4jToolProviderDataSource(
				mock(MagicResourceService.class), mock(MagicToolInvoker.class));
	}

	@Test
	void shouldCreateProviderPerKey() {
		boolean created = dataSource.create(info("evaluation"));

		assertThat(created).isTrue();
		ToolProvider provider = dataSource.get("evaluation");
		assertThat(provider).isInstanceOf(MagicApiToolProvider.class);
		assertThat(dataSource.keys()).containsExactly("evaluation");
	}

	@Test
	void shouldRejectBlankKey() {
		assertThat(dataSource.create(info(""))).isFalse();
		assertThat(dataSource.create(info(null))).isFalse();
		assertThat(dataSource.keys()).isEmpty();
	}

	@Test
	void shouldDestroyProvider() {
		dataSource.create(info("evaluation"));

		dataSource.destroy("evaluation");

		assertThat(dataSource.get("evaluation")).isNull();
		assertThat(dataSource.keys()).isEmpty();
	}

	@Test
	void shouldInvalidateAllWithoutAffectingProviders() {
		dataSource.create(info("evaluation"));
		dataSource.create(info("report"));

		dataSource.invalidateAll();

		assertThat(dataSource.get("evaluation")).isNotNull();
		assertThat(dataSource.get("report")).isNotNull();
	}

	@Test
	void shouldDisposeAllOnDestroy() {
		dataSource.create(info("evaluation"));

		dataSource.destroy();

		assertThat(dataSource.keys()).isEmpty();
	}

	private Langchain4jToolProviderInfo info(String key) {
		Langchain4jToolProviderInfo info = new Langchain4jToolProviderInfo();
		info.setKey(key);
		info.setName(key);
		return info;
	}
}
