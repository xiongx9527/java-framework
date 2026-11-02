package com.xiongx9527.fw.plus.ai.model;

/**
 * 模型提供方枚举。
 *
 * <p><b>作用</b>：让调用方与监听器在不解析模型名的前提下判断「这是哪家的模型」，
 * 用于能力分支（如是否支持图片输入）、计费归类与故障转移策略。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.model.ModelProvider}，枚举项一致。</p>
 *
 * @author xiongxiao
 */
public enum ModelProvider {

    /** Anthropic。 */
    ANTHROPIC,
    /** Amazon Bedrock。 */
    AMAZON_BEDROCK,
    /** GitHub Models。 */
    GITHUB_MODELS,
    /** Azure OpenAI。 */
    AZURE_OPEN_AI,
    /** Google AI Gemini（AI Studio）。 */
    GOOGLE_AI_GEMINI,
    /** Google GenAI。 */
    GOOGLE_GENAI,
    /** Google Vertex AI Gemini。 */
    GOOGLE_VERTEX_AI_GEMINI,
    /** Google Vertex AI Anthropic。 */
    GOOGLE_VERTEX_AI_ANTHROPIC,
    /** Microsoft Foundry。 */
    MICROSOFT_FOUNDRY,
    /** Mistral AI。 */
    MISTRAL_AI,
    /** Ollama。 */
    OLLAMA,
    /** OpenAI 及兼容协议。 */
    OPEN_AI,
    /** IBM watsonx。 */
    WATSONX,
    /** Cohere。 */
    COHERE,
    /** Voyage AI。 */
    VOYAGE_AI,
    /** Jina。 */
    JINA,
    /** Typesafe。 */
    TYPESAFE,
    /** 其它（未归类）。 */
    OTHER
}
