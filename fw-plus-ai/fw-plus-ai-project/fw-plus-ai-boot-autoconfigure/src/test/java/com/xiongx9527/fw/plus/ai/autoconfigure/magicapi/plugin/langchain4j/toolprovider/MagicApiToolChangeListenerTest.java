package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.MagicDynamicLangchain4jToolProviderDataSource;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MagicApiToolChangeListenerTest {

	private final MagicDynamicLangchain4jToolProviderDataSource dataSource =
			mock(MagicDynamicLangchain4jToolProviderDataSource.class);

	private final MagicApiToolChangeListener listener = new MagicApiToolChangeListener(dataSource);

	@Test
	void shouldInvalidateAllOnApiAndFunctionFileEvents() {
		listener.onFileEvent(new FileEvent("api", EventAction.SAVE, new ApiInfo()));
		listener.onFileEvent(new FileEvent("function", EventAction.DELETE, new PathMagicEntity()));

		verify(dataSource, times(2)).invalidateAll();
	}

	@Test
	void shouldInvalidateAllOnGroupEvent() {
		listener.onGroupEvent(new GroupEvent("api", EventAction.DELETE, new Group()));

		verify(dataSource).invalidateAll();
	}
}
