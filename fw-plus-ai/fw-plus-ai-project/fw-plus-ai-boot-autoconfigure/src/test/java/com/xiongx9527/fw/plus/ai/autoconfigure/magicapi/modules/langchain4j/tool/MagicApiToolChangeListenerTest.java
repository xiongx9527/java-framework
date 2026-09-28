package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool;

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

	private final MagicApiToolRegistry registry = mock(MagicApiToolRegistry.class);

	private final MagicApiToolChangeListener listener = new MagicApiToolChangeListener(registry);

	@Test
	void shouldInvalidateSnapshotOnApiAndFunctionFileEvents() {
		listener.onFileEvent(new FileEvent("api", EventAction.SAVE, new ApiInfo()));
		listener.onFileEvent(new FileEvent("function", EventAction.DELETE, new PathMagicEntity()));

		verify(registry, times(2)).invalidateSnapshot();
	}

	@Test
	void shouldInvalidateSnapshotOnGroupEvent() {
		listener.onGroupEvent(new GroupEvent("api", EventAction.DELETE, new Group()));

		verify(registry).invalidateSnapshot();
	}
}
