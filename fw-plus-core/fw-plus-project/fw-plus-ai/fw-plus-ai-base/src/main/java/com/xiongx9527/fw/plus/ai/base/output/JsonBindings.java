package com.xiongx9527.fw.plus.ai.base.output;

import java.lang.reflect.Type;

/**
 * 中性 JSON 绑定抽象：把 {@link JsonOutputPort} 已提取出的 JSON 文本反序列化为目标类型。
 *
 * <p>真正的 JSON 库（Jackson / Gson / fastjson 等）在 {@code support} 或具体后端 adapter 侧实现，
 * 因此 {@code base} 保持零 JSON 依赖：这里只依赖 JDK 自带的 {@link java.lang.reflect.Type}。</p>
 *
 * <p>约定：</p>
 * <ul>
 *   <li>入参 {@code json} 为 {@code null} 时返回 {@code null}；</li>
 *   <li>反序列化失败抛 {@link JsonOutputException}（而非库自身的异常），便于上层统一处理；</li>
 *   <li>{@code type} 用 {@link Type} 而非 {@link Class}，以便支持 {@code List<Foo>} 这类参数化类型。</li>
 * </ul>
 *
 * @see JsonOutputPort#completeJson(com.xiongx9527.fw.plus.ai.base.model.LlmRequest)
 */
public interface JsonBindings {

    /**
     * 把 JSON 文本绑定为目标类型的实例。
     *
     * @param json 已提取的 JSON 文本（可能为 {@code null}）
     * @param type 目标类型（支持参数化类型，如 {@code List<Foo>}）
     * @param <T>  目标类型
     * @return 绑定后的实例；{@code json} 为 {@code null} 时返回 {@code null}
     * @throws JsonOutputException 反序列化失败时抛出
     */
    <T> T bind(String json, Type type);

    /**
     * 便捷方法：绑定到具体类（等价于 {@code bind(json, (Type) type)}）。
     */
    default <T> T bind(String json, Class<T> type) {
        return bind(json, (Type) type);
    }
}
