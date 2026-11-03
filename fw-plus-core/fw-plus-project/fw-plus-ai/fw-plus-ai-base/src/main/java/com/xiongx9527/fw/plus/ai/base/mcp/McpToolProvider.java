package com.xiongx9527.fw.plus.ai.base.mcp;

import com.xiongx9527.fw.plus.ai.base.tool.ToolArgumentCodec;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionException;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 把 MCP 工具包装成普通 {@link ToolProvider}：对上层来说，MCP 工具与本地
 * {@code @Tool} 方法没有区别，工具调用环不需要知道工具有没有跨进程。
 *
 * <p>工具列表懒加载并缓存：MCP 列举是一次远程往返，不该每次调用都做。</p>
 */
public class McpToolProvider implements ToolProvider {

    private final McpClient client;

    private final ToolArgumentCodec codec;

    private final List<McpListener> listeners;

    private volatile List<ToolSpecification> cache;

    public McpToolProvider(McpClient client, ToolArgumentCodec codec) {
        this(client, codec, null);
    }

    public McpToolProvider(McpClient client, ToolArgumentCodec codec, List<McpListener> listeners) {
        if (client == null) {
            throw new IllegalArgumentException("mcp client is required");
        }
        if (codec == null) {
            throw new IllegalArgumentException("tool argument codec is required");
        }
        this.client = client;
        this.codec = codec;
        this.listeners = listeners == null ? new ArrayList<McpListener>() : listeners;
    }

    @Override
    public List<ToolSpecification> specifications() {
        List<ToolSpecification> current = cache;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cache == null) {
                try {
                    List<ToolSpecification> tools = client.listTools();
                    cache = tools == null ? new ArrayList<ToolSpecification>() : tools;
                } catch (RuntimeException e) {
                    notifyError(e);
                    throw e;
                }
                notifyListTools(cache.size());
            }
            return cache;
        }
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        if (request == null || request.getName() == null) {
            throw new ToolExecutionException("tool name is required");
        }
        Map<String, Object> arguments = codec.decode(request.getArguments());
        try {
            String result = client.callTool(request.getName(), arguments);
            notifyToolCall(request.getName());
            return result;
        } catch (RuntimeException e) {
            notifyError(e);
            throw new ToolExecutionException("mcp tool failed: " + request.getName(), e);
        }
    }

    private String clientName() {
        return client.config() == null ? null : client.config().getName();
    }

    private void notifyListTools(int count) {
        for (McpListener listener : listeners) {
            try {
                listener.onListTools(clientName(), count);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyToolCall(String toolName) {
        for (McpListener listener : listeners) {
            try {
                listener.onToolCall(clientName(), toolName);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyError(RuntimeException e) {
        for (McpListener listener : listeners) {
            try {
                listener.onError(e.getMessage());
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
