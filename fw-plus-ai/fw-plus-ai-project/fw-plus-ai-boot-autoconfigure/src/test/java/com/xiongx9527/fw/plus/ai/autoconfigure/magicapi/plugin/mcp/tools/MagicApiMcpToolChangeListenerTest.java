package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.event.EventAction;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Group;
import org.ssssssss.magicapi.core.model.PathMagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicApiMcpToolChangeListenerTest {

    private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier notifier =
            mock(com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier.class);

    @Test
    void shouldSyncToolsOnFunctionFileEvent() {
        Fixture fixture = new Fixture(notifier);

        fixture.listener.onFileEvent(new FileEvent("function", EventAction.SAVE, fixture.entity));

        assertThat(fixture.featureMgr.getToolStore()).containsKey("echo");
    }

    @Test
    void shouldSyncToolsOnApiGroupEvent() {
        Fixture fixture = new Fixture(notifier);

        fixture.listener.onGroupEvent(new GroupEvent("api", EventAction.SAVE, new Group()));

        assertThat(fixture.featureMgr.getToolStore()).containsKey("echo");
    }

    @Test
    void shouldNotPublishAgainWhenSnapshotStaysUnchanged() {
        Fixture fixture = new Fixture(notifier);
        fixture.listener.refresh();

        fixture.listener.refresh();

        assertThat(fixture.featureMgr.getToolStore()).hasSize(1);
        verify(notifier).publishToolsChanged();
    }

    @Test
    void shouldRemoveToolFromStoreWhenResourceDisappears() {
        Fixture fixture = new Fixture(notifier);
        fixture.listener.refresh();
        when(fixture.magicResourceService.<PathMagicEntity>files("function")).thenReturn(Collections.emptyList());

        fixture.listener.refresh();

        assertThat(fixture.featureMgr.getToolStore()).doesNotContainKey("echo");
    }

    private static final class Fixture {

        private final com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier passedNotifier;

        private final MagicResourceService magicResourceService = mock(MagicResourceService.class);

        private final PathMagicEntity entity = new PathMagicEntity();

        private final FeatureMgr featureMgr = new FeatureMgr();

        private final McpServer server = spy(new McpServer());

        private final MagicApiMcpToolChangeListener listener;

        private Fixture(
                com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier passedNotifier) {
            this.passedNotifier = passedNotifier;
            entity.setId("function-echo");
            entity.setName("echo");
            entity.setPath("/public/echo");
            when(magicResourceService.<PathMagicEntity>files("function"))
                    .thenReturn(Collections.singletonList(entity));
            when(magicResourceService.<ApiInfo>files("api")).thenReturn(Collections.emptyList());
            when(magicResourceService.getGroupPath(anyString())).thenReturn("/public");
            server.setFeatureMgr(featureMgr);
            server.setTransport(new NoopTransport());

            MagicApiMcpToolRegistry registry =
                    new MagicApiMcpToolRegistry(magicResourceService, new MagicApiMcpProperties());
            MagicApiMcpToolRegistrar registrar = new MagicApiMcpToolRegistrar(registry);
            McpServerRuntime runtime = mock(McpServerRuntime.class);
            when(runtime.getFeatureMgr()).thenReturn(featureMgr);
            when(runtime.getServer()).thenReturn(server);
            listener = new MagicApiMcpToolChangeListener(registrar, Collections.singletonList(runtime), null, passedNotifier);
        }
    }

    private static final class NoopTransport implements com.ajaxjs.mcp.transport.McpTransportSync {

        @Override
        public void start() {
        }

        @Override
        public String handle(String message) {
            return null;
        }

        @Override
        public void initialize() {
        }

        @Override
        public void send(String sessionId, String message) {
        }

        @Override
        public void broadcast(String message) {
        }

        @Override
        public void close() {
        }
    }

}
