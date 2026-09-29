package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ollama;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingLanguageModel;

import java.util.List;

/**
 * Ollama 工厂：本地模型，无需 api-key。ollama jar 不含 language/tokenizer，对应能力保持默认 Unsupported。
 */
public class Langchain4jOllamaModelFactory implements ModelFactory {

    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_MODEL = "qwen2.5";
    private static final String DEFAULT_EMBEDDING_MODEL = "nomic-embed-text";

    @Override
    public String provider() {
        return "ollama";
    }

    @Override
    public OllamaChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        return OllamaChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.dbl("temperature", 0.7))
                .topK(nullIfZero(o.integer("top-k", null)))
                .topP(o.dbl("top-p", null))
                .repeatPenalty(o.dbl("repeat-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .numPredict(nullIfZero(o.integer("num-predict", null)))
                .numCtx(nullIfZero(o.integer("num-ctx", null)))
                .stop(o.stringList("stop", null))
                .format(o.string("format", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", 3)))
                .listeners(listeners)
                .build();
    }

    @Override
    public OllamaStreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        // 注意：0.35.0 streaming builder 无 maxRetries
        return OllamaStreamingChatModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.dbl("temperature", 0.7))
                .topK(nullIfZero(o.integer("top-k", null)))
                .topP(o.dbl("top-p", null))
                .repeatPenalty(o.dbl("repeat-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .numPredict(nullIfZero(o.integer("num-predict", null)))
                .numCtx(nullIfZero(o.integer("num-ctx", null)))
                .stop(o.stringList("stop", null))
                .format(o.string("format", null))
                .timeout(o.duration("timeout", null))
                .listeners(listeners)
                .build();
    }

    @Override
    public OllamaEmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", DEFAULT_EMBEDDING_MODEL))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", 3)))
                .build();
    }

    @Override
    public OllamaLanguageModel buildLanguageModel(Langchain4jOptionsReader o) {
        return OllamaLanguageModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.dbl("temperature", 0.7))
                .topK(nullIfZero(o.integer("top-k", null)))
                .topP(o.dbl("top-p", null))
                .repeatPenalty(o.dbl("repeat-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .numPredict(nullIfZero(o.integer("num-predict", null)))
                .numCtx(nullIfZero(o.integer("num-ctx", null)))
                .stop(o.stringList("stop", null))
                .format(o.string("format", null))
                .timeout(o.duration("timeout", null))
                .maxRetries(nullIfZero(o.integer("max-retries", 3)))
                .build();
    }

    @Override
    public OllamaStreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) {
        // 注意：0.35.0 streaming builder 无 maxRetries
        return OllamaStreamingLanguageModel.builder()
                .baseUrl(o.string("base-url", DEFAULT_BASE_URL))
                .modelName(o.string("model-name", DEFAULT_MODEL))
                .temperature(o.dbl("temperature", 0.7))
                .topK(nullIfZero(o.integer("top-k", null)))
                .topP(o.dbl("top-p", null))
                .repeatPenalty(o.dbl("repeat-penalty", null))
                .seed(nullIfZero(o.integer("seed", null)))
                .numPredict(nullIfZero(o.integer("num-predict", null)))
                .numCtx(nullIfZero(o.integer("num-ctx", null)))
                .stop(o.stringList("stop", null))
                .format(o.string("format", null))
                .timeout(o.duration("timeout", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
