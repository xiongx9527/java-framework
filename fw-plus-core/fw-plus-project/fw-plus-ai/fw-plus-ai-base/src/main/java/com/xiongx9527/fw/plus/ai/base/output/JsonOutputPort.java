package com.xiongx9527.fw.plus.ai.base.output;

import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import java.util.function.Function;

/**
 * 让模型「稳定输出 JSON」的端口。
 *
 * <p>落地三件事，使模型尽量只吐 JSON、且我们能可靠取回：</p>
 * <ol>
 *   <li><b>指令约束</b>：向 system 消息注入「只输出 JSON」的强指令；</li>
 *   <li><b>协议约束</b>：可选地把后端 JSON 模式的私有参数（如 {@code response_format}）并入请求，
 *       由 adapter 原样下发（见 {@link DefaultJsonOutputPort}）；</li>
 *   <li><b>兜底解析</b>：用零依赖的 {@link JsonSupport} 从返回文本中剥离 markdown 围栏、提取首个
 *       平衡括号的 JSON 子串；提取失败时按配置重试并附带纠错提示。</li>
 * </ol>
 *
 * <p>反序列化交由调用方注入的解析器完成（真正的 JSON 库绑定在 {@code support} / 具体 adapter 侧），
 * 这样 {@code base} 保持零 JSON 依赖。</p>
 */
public interface JsonOutputPort {

    /**
     * 强制模型只输出 JSON：注入指令、必要时下发 JSON 模式参数、聚合并用 {@link JsonSupport} 提取。
     *
     * @return 提取出的 JSON 文本（保证可被括号匹配解析）；无法产出则抛 {@link JsonOutputException}
     */
    String completeJson(LlmRequest request);

    /**
     * 在 {@link #completeJson(LlmRequest)} 基础上用调用方提供的解析器反序列化。
     */
    default <T> T completeJson(LlmRequest request, Function<String, T> parser) {
        String json = completeJson(request);
        if (json == null) {
            return null;
        }
        return parser.apply(json);
    }

    /** 单轮 system + user 的快捷方式。 */
    default String completeJson(String systemPrompt, String userPrompt) {
        return completeJson(LlmRequest.systemUser(systemPrompt, userPrompt));
    }

    /** 单轮 system + user 并反序列化的快捷方式。 */
    default <T> T completeJson(String systemPrompt, String userPrompt, Function<String, T> parser) {
        return completeJson(LlmRequest.systemUser(systemPrompt, userPrompt), parser);
    }
}
