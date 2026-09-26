package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import com.ajaxjs.mcp.protocol.tools.JsonSchema;
import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.server.feature.model.ServerStoreTool;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicApiMcpToolRegistrarTest {

    private MagicResourceService magicResourceService;

    private PathMagicEntity echo;

    private FeatureMgr featureMgr;

    private McpServer server;

    @BeforeEach
    void setUp() {
        magicResourceService = mock(MagicResourceService.class);
        echo = new PathMagicEntity();
        echo.setId("function-echo");
        echo.setName("echo");
        echo.setPath("/public/echo");
        echo.setAttribute("mcpInputSchema",
                "{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\"}},\"required\":[\"text\"]}");
        when(magicResourceService.<MagicEntity>files("function")).thenReturn(Collections.singletonList(echo));
        when(magicResourceService.<ApiInfo>files("api")).thenReturn(Collections.emptyList());
        when(magicResourceService.getGroupPath(anyString())).thenReturn("/public");
        featureMgr = new FeatureMgr();
        server = spy(new McpServer());
        server.setFeatureMgr(featureMgr);
        server.setTransport(new RecordingTransport());
    }

    private static final class RecordingTransport implements com.ajaxjs.mcp.transport.McpTransportSync {

        private final List<String> broadcasts = new ArrayList<>();

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
            broadcasts.add(message);
        }

        @Override
        public void close() {
        }
    }

    @Test
    void shouldRegisterSnapshotToolsIntoToolStore() {
        registrar().register(featureMgr, server);

        Map<String, ServerStoreTool> store = featureMgr.getToolStore();
        assertThat(store).containsKey("echo");
        ServerStoreTool tool = store.get("echo");
        assertThat(tool.getTool().getName()).isEqualTo("echo");
        JsonSchema inputSchema = tool.getTool().getInputSchema();
        assertThat(inputSchema).isNotNull();
        assertThat(inputSchema.getType()).isEqualTo("object");
        assertThat(inputSchema.getProperties()).containsKey("text");
        assertThat(inputSchema.getRequired()).containsExactly("text");
    }

    @Test
    void shouldRegisterToolWithExecutablePlaceholderMethod() throws Exception {
        registrar().register(featureMgr, server);

        ServerStoreTool tool = featureMgr.getToolStore().get("echo");
        assertThat(tool.getMethod()).isNotNull();
        assertThat(tool.getMethod().getParameterCount()).isZero();
        assertThat(tool.getParamsOrder()).isEmpty();
        assertThat(tool.getMethod().invoke(tool.getInstance())).isEqualTo(MagicApiMcpToolRegistrar.PLACEHOLDER);
    }

    @Test
    void shouldFallbackToEmptyObjectSchema() {
        MagicEntity plain = new MagicEntity();
        plain.setId("function-plain");
        plain.setName("plain");
        when(magicResourceService.<MagicEntity>files("function")).thenReturn(Collections.singletonList(plain));

        registrar().register(featureMgr, server);

        JsonSchema inputSchema = featureMgr.getToolStore().get("plain").getTool().getInputSchema();
        assertThat(inputSchema.getType()).isEqualTo("object");
        assertThat(inputSchema.getProperties()).isNullOrEmpty();
    }

    @Test
    void shouldRegisterApiToolWhenEnabled() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        ApiInfo info = new ApiInfo();
        info.setId("api-list");
        info.setName("订单列表");
        info.setDescription("分页查询订单");
        info.setGroupId("group-admin");
        info.setPath("/orders/list");
        info.setMethod("GET");
        when(magicResourceService.<ApiInfo>files("api")).thenReturn(Collections.singletonList(info));
        when(magicResourceService.getGroupPath("group-admin")).thenReturn("/admin");

        new MagicApiMcpToolRegistrar(new MagicApiMcpToolRegistry(magicResourceService, properties))
                .register(featureMgr, server);

        ServerStoreTool tool = featureMgr.getToolStore().get("GET /orders/list");
        assertThat(tool).isNotNull();
        assertThat(tool.getTool().getDescription()).isEqualTo("分页查询订单");
    }

    @Test
    void shouldPublishToolsChangedAfterRegister() {
        registrar().register(featureMgr, server);

        verify(server, never()).publishToolsChanged();
    }

    @Test
    void shouldRemoveToolsNoLongerInSnapshot() {
        MagicApiMcpToolRegistrar registrar = registrar();
        registrar.register(featureMgr, server);
        assertThat(featureMgr.getToolStore()).containsKey("echo");

        PathMagicEntity other = new PathMagicEntity();
        other.setId("function-other");
        other.setName("other");
        other.setPath("/public/other");
        when(magicResourceService.<MagicEntity>files("function")).thenReturn(Collections.singletonList(other));
        // 快照缓存：资源变更后必须显式失效，否则读到旧快照（生产路径由变更事件触发）
        registrar.invalidateSource();
        registrar.register(featureMgr, server);

        assertThat(featureMgr.getToolStore()).containsOnlyKeys("other");
    }

    @Test
    void shouldKeepForeignToolsUntouched() {
        ServerStoreTool foreign = new ServerStoreTool();
        foreign.setTool(new com.ajaxjs.mcp.protocol.tools.ToolItem());
        foreign.getTool().setName("foreign");
        featureMgr.getToolStore().put("foreign", foreign);

        registrar().register(featureMgr, server);

        assertThat(featureMgr.getToolStore()).containsKeys("foreign", "echo");
    }

    @Test
    void shouldSkipPublishWhenSnapshotUnchanged() {
        MagicApiMcpToolRegistrar registrar = registrar();
        registrar.register(featureMgr, server);
        registrar.register(featureMgr, server);

        verify(server, never()).publishToolsChanged();
    }

    @Test
    void shouldReturnRegisteredToolNames() {
        List<String> names = registrar().register(featureMgr, server);

        assertThat(names).containsExactly("echo");
    }

    @Test
    void shouldReplaceToolAndNotifyWhenInputSchemaChanged() {
        registrar().register(featureMgr, server);
        ServerStoreTool before = featureMgr.getToolStore().get("echo");

        echo.setAttribute("mcpInputSchema", "{\"type\":\"object\"}");
        registrar().register(featureMgr, server);

        ServerStoreTool after = featureMgr.getToolStore().get("echo");
        assertThat(after).isNotSameAs(before);
        assertThat(after.getTool().getInputSchema().getProperties()).isNull();
        verify(server, never()).publishToolsChanged();
    }

    @Test
    void shouldNotDuplicateRegistrationWhenRefreshedRepeatedly() {
        MagicApiMcpToolRegistrar registrar = registrar();
        registrar.register(featureMgr, server);
        ServerStoreTool before = featureMgr.getToolStore().get("echo");

        registrar.register(featureMgr, server);
        registrar.register(featureMgr, server);

        assertThat(featureMgr.getToolStore()).hasSize(1);
        assertThat(featureMgr.getToolStore().get("echo")).isSameAs(before);
        verify(server, never()).publishToolsChanged();
    }

    private MagicApiMcpToolRegistrar registrar() {
        return new MagicApiMcpToolRegistrar(
                new MagicApiMcpToolRegistry(magicResourceService, new MagicApiMcpProperties()));
    }

    @Test
    void shouldScanSnapshotOnceForBatchRegistration() {
        FeatureMgr second = new FeatureMgr();
        MagicApiMcpToolRegistrar.RuntimeTarget firstTarget =
                new MagicApiMcpToolRegistrar.RuntimeTarget(featureMgr, server);
        MagicApiMcpToolRegistrar.RuntimeTarget secondTarget =
                new MagicApiMcpToolRegistrar.RuntimeTarget(second, null);

        Map<FeatureMgr, List<String>> registered = registrar().registerAll(
                java.util.Arrays.asList(firstTarget, secondTarget), new java.util.concurrent.atomic.AtomicBoolean());

        assertThat(registered).containsOnlyKeys(featureMgr, second);
        assertThat(featureMgr.getToolStore()).containsKey("echo");
        assertThat(second.getToolStore()).containsKey("echo");
        verify(magicResourceService, times(1)).<MagicEntity>files("function");
    }
}
