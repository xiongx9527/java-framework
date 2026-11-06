package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

/**
 * LLM 端点 baseUrl 规范化工具。
 *
 * <p>各厂商对 {@code baseUrl} 是否需带 {@code /v1} 尾缀的约定不一致（Anthropic 期望不带、
 * OpenAI/Responses 期望带），为避免调用方按单一 {@code apiDomain} 配置时踩坑，统一在此剥掉结尾斜杠
 * 与可选的 {@code /v1} 段，由各端口基于「无尾斜杠、无 /v1」的根地址拼出完整路径（含各自所需的 /v1）。
 * 这样无论调用方传入的地址是否以 {@code /v1} 结尾都能正确，且向后兼容两种写法。</p>
 *
 * <p>注意：本工具只用于期望「根地址 + /v1/...」的端口；Azure OpenAI 的路径为
 * {@code /openai/deployments/...}（无独立 /v1 段，版本由 api-version 查询参数承载），
 * 不使用本规范化，避免改变其既有契约。</p>
 */
final class LlmUrlUtil {

    private LlmUrlUtil() { }

    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) return null;
        String u = baseUrl.replaceAll("/+$", "");
        if (u.endsWith("/v1")) {
            u = u.substring(0, u.length() - 3);
        }
        return u;
    }
}
