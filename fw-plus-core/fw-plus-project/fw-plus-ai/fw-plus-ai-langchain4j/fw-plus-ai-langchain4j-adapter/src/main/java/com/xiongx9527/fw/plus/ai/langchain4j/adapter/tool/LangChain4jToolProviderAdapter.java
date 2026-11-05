package com.xiongx9527.fw.plus.ai.langchain4j.adapter.tool;

import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionException;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.support.LcTools;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.tool.ToolProviderResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * base 的 {@link ToolProvider} 与 LangChain4j 0.35 的
 * {@code dev.langchain4j.service.tool.ToolProvider} 双向转换。
 *
 * <p>两侧语义差异：</p>
 * <ul>
 *   <li>base：{@code specifications()} + {@code execute(ToolExecutionRequest)}，一次注册静态清单；</li>
 *   <li>LangChain4j：{@code provideTools(ToolProviderRequest)} 按每次请求动态返回
 *       {@code Map<ToolSpecification, ToolExecutor>}，执行器还带 {@code chatMemoryId}。</li>
 * </ul>
 *
 * <p><b>LangChain4j → base</b>：构造时（以及每次 {@link #refresh()}）用给定的
 * {@code chatMemoryId / userMessage} 取一次工具快照。base 的契约没有请求上下文，
 * 动态工具需要业务侧在上下文变化时主动调用 {@link #refresh()}。</p>
 *
 * <p><b>base → LangChain4j</b>：{@link #toUpstream(ToolProvider)} 把静态清单包装成
 * 每次请求返回同一组工具的 {@code ToolProvider}，可直接挂到 LangChain4j 的
 * {@code AiServices}。</p>
 */
public class LangChain4jToolProviderAdapter implements ToolProvider {

    private final dev.langchain4j.service.tool.ToolProvider provider;

    private final Object chatMemoryId;

    private final UserMessage userMessage;

    private volatile List<ToolSpecification> specifications;

    private volatile Map<String, dev.langchain4j.service.tool.ToolExecutor> executors;

    public LangChain4jToolProviderAdapter(dev.langchain4j.service.tool.ToolProvider provider) {
        this(provider, null, null);
    }

    public LangChain4jToolProviderAdapter(dev.langchain4j.service.tool.ToolProvider provider,
                                          Object chatMemoryId, UserMessage userMessage) {
        if (provider == null) {
            throw new IllegalArgumentException("tool provider is required");
        }
        this.provider = provider;
        this.chatMemoryId = chatMemoryId;
        this.userMessage = userMessage;
        refresh();
    }

    /** 重新拉取工具快照；上下文变化后由业务侧主动调用。 */
    public void refresh() {
        ToolProviderResult result = provider.provideTools(
                new dev.langchain4j.service.tool.ToolProviderRequest(chatMemoryId, userMessage));
        Map<dev.langchain4j.agent.tool.ToolSpecification, dev.langchain4j.service.tool.ToolExecutor> tools =
                result == null || result.tools() == null
                        ? Collections.<dev.langchain4j.agent.tool.ToolSpecification,
                                dev.langchain4j.service.tool.ToolExecutor>emptyMap()
                        : result.tools();
        List<ToolSpecification> specs = new ArrayList<ToolSpecification>();
        Map<String, dev.langchain4j.service.tool.ToolExecutor> byName =
                new LinkedHashMap<String, dev.langchain4j.service.tool.ToolExecutor>();
        for (Map.Entry<dev.langchain4j.agent.tool.ToolSpecification,
                dev.langchain4j.service.tool.ToolExecutor> entry : tools.entrySet()) {
            dev.langchain4j.agent.tool.ToolSpecification upstream = entry.getKey();
            if (upstream == null) {
                continue;
            }
            specs.add(toBaseSpec(upstream));
            byName.put(upstream.name(), entry.getValue());
        }
        this.specifications = Collections.unmodifiableList(specs);
        this.executors = byName;
    }

    @Override
    public List<ToolSpecification> specifications() {
        return specifications;
    }

    @Override
    public String execute(ToolExecutionRequest request) {
        if (request == null) {
            throw new ToolExecutionException("tool execution request is required");
        }
        dev.langchain4j.service.tool.ToolExecutor executor = executors.get(request.getName());
        if (executor == null) {
            throw new ToolExecutionException("tool not found: " + request.getName());
        }
        try {
            return executor.execute(LcTools.toUpstreamCall(request), chatMemoryId);
        } catch (ToolExecutionException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ToolExecutionException(
                    "tool " + request.getName() + " failed: " + e.getMessage(), e);
        }
    }

    /** 把 base 的静态工具清单包装成 LangChain4j 的 {@code ToolProvider}。 */
    public static dev.langchain4j.service.tool.ToolProvider toUpstream(final ToolProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("tool provider is required");
        }
        return request -> {
            Map<dev.langchain4j.agent.tool.ToolSpecification,
                    dev.langchain4j.service.tool.ToolExecutor> tools = new LinkedHashMap<>();
            for (ToolSpecification spec : provider.specifications()) {
                if (spec == null) {
                    continue;
                }
                dev.langchain4j.agent.tool.ToolSpecification upstream = LcTools.toUpstreamSpec(spec);
                tools.put(upstream, (call, memoryId) -> provider.execute(LcTools.toBaseCall(call)));
            }
            return new ToolProviderResult(tools);
        };
    }

    private static ToolSpecification toBaseSpec(dev.langchain4j.agent.tool.ToolSpecification spec) {
        ToolParameters params = spec.parameters();
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", params == null || params.type() == null ? "object" : params.type());
        if (params != null) {
            schema.put("properties", params.properties() == null
                    ? new LinkedHashMap<String, Map<String, Object>>()
                    : new LinkedHashMap<String, Map<String, Object>>(params.properties()));
            schema.put("required", params.required() == null
                    ? new ArrayList<String>() : new ArrayList<String>(params.required()));
        }
        return ToolSpecification.builder()
                .name(spec.name())
                .description(spec.description())
                .parameters(schema)
                .build();
    }
}
