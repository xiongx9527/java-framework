package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.provider.ReflectiveLangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.DefaultLangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Configuration
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.ai.langchain4j.adapter.DefaultLangChain4jModelFactory")
public class LangChain4jModelAdapterAutoConfiguration {

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.openai.OpenAiChatModel")
	@ConditionalOnMissingBean(name = "openAiChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter openAiChatModelProviderAdapter() { return adapter("openai", LangChain4jModelType.CHAT, "dev.langchain4j.model.openai.OpenAiChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.openai.OpenAiStreamingChatModel")
	@ConditionalOnMissingBean(name = "openAiStreamingChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter openAiStreamingChatModelProviderAdapter() { return adapter("openai", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.model.openai.OpenAiStreamingChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.openai.OpenAiEmbeddingModel")
	@ConditionalOnMissingBean(name = "openAiEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter openAiEmbeddingModelProviderAdapter() { return adapter("openai", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.openai.OpenAiEmbeddingModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.openai.OpenAiImageModel")
	@ConditionalOnMissingBean(name = "openAiImageModelProviderAdapter")
	public LangChain4jModelProviderAdapter openAiImageModelProviderAdapter() { return adapter("openai", LangChain4jModelType.IMAGE, "dev.langchain4j.model.openai.OpenAiImageModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.openai.OpenAiLanguageModel")
	@ConditionalOnMissingBean(name = "openAiLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter openAiLanguageModelProviderAdapter() { return adapter("openai", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.openai.OpenAiLanguageModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.openai.OpenAiStreamingLanguageModel")
	@ConditionalOnMissingBean(name = "openAiStreamingLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter openAiStreamingLanguageModelProviderAdapter() { return adapter("openai", LangChain4jModelType.STREAMING_LANGUAGE, "dev.langchain4j.model.openai.OpenAiStreamingLanguageModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.azure.AzureOpenAiChatModel")
	@ConditionalOnMissingBean(name = "azureOpenAiChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter azureOpenAiChatModelProviderAdapter() { return azureAdapter(LangChain4jModelType.CHAT, "dev.langchain4j.model.azure.AzureOpenAiChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel")
	@ConditionalOnMissingBean(name = "azureOpenAiStreamingChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter azureOpenAiStreamingChatModelProviderAdapter() { return azureAdapter(LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel")
	@ConditionalOnMissingBean(name = "azureOpenAiEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter azureOpenAiEmbeddingModelProviderAdapter() { return azureAdapter(LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.azure.AzureOpenAiImageModel")
	@ConditionalOnMissingBean(name = "azureOpenAiImageModelProviderAdapter")
	public LangChain4jModelProviderAdapter azureOpenAiImageModelProviderAdapter() { return azureAdapter(LangChain4jModelType.IMAGE, "dev.langchain4j.model.azure.AzureOpenAiImageModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.azure.AzureOpenAiLanguageModel")
	@ConditionalOnMissingBean(name = "azureOpenAiLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter azureOpenAiLanguageModelProviderAdapter() { return azureAdapter(LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.azure.AzureOpenAiLanguageModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.azure.AzureOpenAiStreamingLanguageModel")
	@ConditionalOnMissingBean(name = "azureOpenAiStreamingLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter azureOpenAiStreamingLanguageModelProviderAdapter() { return azureAdapter(LangChain4jModelType.STREAMING_LANGUAGE, "dev.langchain4j.model.azure.AzureOpenAiStreamingLanguageModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.dashscope.QwenChatModel")
	@ConditionalOnMissingBean(name = "qwenChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter qwenChatModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.CHAT, "dev.langchain4j.community.model.dashscope.QwenChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.dashscope.QwenStreamingChatModel")
	@ConditionalOnMissingBean(name = "qwenStreamingChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter qwenStreamingChatModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.community.model.dashscope.QwenStreamingChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.dashscope.QwenEmbeddingModel")
	@ConditionalOnMissingBean(name = "qwenEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter qwenEmbeddingModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.EMBEDDING, "dev.langchain4j.community.model.dashscope.QwenEmbeddingModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.dashscope.WanxImageModel")
	@ConditionalOnMissingBean(name = "qwenImageModelProviderAdapter")
	public LangChain4jModelProviderAdapter qwenImageModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.IMAGE, "dev.langchain4j.community.model.dashscope.WanxImageModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.dashscope.QwenLanguageModel")
	@ConditionalOnMissingBean(name = "qwenLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter qwenLanguageModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.LANGUAGE, "dev.langchain4j.community.model.dashscope.QwenLanguageModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.dashscope.QwenStreamingLanguageModel")
	@ConditionalOnMissingBean(name = "qwenStreamingLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter qwenStreamingLanguageModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.STREAMING_LANGUAGE, "dev.langchain4j.community.model.dashscope.QwenStreamingLanguageModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.ollama.OllamaChatModel")
	@ConditionalOnMissingBean(name = "ollamaChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter ollamaChatModelProviderAdapter() { return adapter("ollama", LangChain4jModelType.CHAT, "dev.langchain4j.model.ollama.OllamaChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.ollama.OllamaEmbeddingModel")
	@ConditionalOnMissingBean(name = "ollamaEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter ollamaEmbeddingModelProviderAdapter() { return adapter("ollama", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.ollama.OllamaEmbeddingModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.ollama.OllamaLanguageModel")
	@ConditionalOnMissingBean(name = "ollamaLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter ollamaLanguageModelProviderAdapter() { return adapter("ollama", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.ollama.OllamaLanguageModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.anthropic.AnthropicChatModel")
	@ConditionalOnMissingBean(name = "anthropicChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter anthropicChatModelProviderAdapter() { return adapter("anthropic", LangChain4jModelType.CHAT, "dev.langchain4j.model.anthropic.AnthropicChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.anthropic.AnthropicStreamingChatModel")
	@ConditionalOnMissingBean(name = "anthropicStreamingChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter anthropicStreamingChatModelProviderAdapter() { return adapter("anthropic", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.model.anthropic.AnthropicStreamingChatModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.qianfan.QianfanChatModel")
	@ConditionalOnMissingBean(name = "qianfanChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter qianfanChatModelProviderAdapter() { return adapter("qianfan", LangChain4jModelType.CHAT, "dev.langchain4j.community.model.qianfan.QianfanChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.qianfan.QianfanStreamingChatModel")
	@ConditionalOnMissingBean(name = "qianfanStreamingChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter qianfanStreamingChatModelProviderAdapter() { return adapter("qianfan", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.community.model.qianfan.QianfanStreamingChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.qianfan.QianfanEmbeddingModel")
	@ConditionalOnMissingBean(name = "qianfanEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter qianfanEmbeddingModelProviderAdapter() { return adapter("qianfan", LangChain4jModelType.EMBEDDING, "dev.langchain4j.community.model.qianfan.QianfanEmbeddingModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.qianfan.QianfanLanguageModel")
	@ConditionalOnMissingBean(name = "qianfanLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter qianfanLanguageModelProviderAdapter() { return adapter("qianfan", LangChain4jModelType.LANGUAGE, "dev.langchain4j.community.model.qianfan.QianfanLanguageModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.zhipu.ZhipuAiChatModel")
	@ConditionalOnMissingBean(name = "zhipuChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter zhipuChatModelProviderAdapter() { return adapter("zhipu", LangChain4jModelType.CHAT, "dev.langchain4j.community.model.zhipu.ZhipuAiChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel")
	@ConditionalOnMissingBean(name = "zhipuStreamingChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter zhipuStreamingChatModelProviderAdapter() { return adapter("zhipu", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel")
	@ConditionalOnMissingBean(name = "zhipuEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter zhipuEmbeddingModelProviderAdapter() { return adapter("zhipu", LangChain4jModelType.EMBEDDING, "dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.community.model.zhipu.ZhipuAiImageModel")
	@ConditionalOnMissingBean(name = "zhipuImageModelProviderAdapter")
	public LangChain4jModelProviderAdapter zhipuImageModelProviderAdapter() { return adapter("zhipu", LangChain4jModelType.IMAGE, "dev.langchain4j.community.model.zhipu.ZhipuAiImageModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.huggingface.HuggingFaceChatModel")
	@ConditionalOnMissingBean(name = "huggingFaceChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter huggingFaceChatModelProviderAdapter() { return adapter("hugging-face", LangChain4jModelType.CHAT, "dev.langchain4j.model.huggingface.HuggingFaceChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel")
	@ConditionalOnMissingBean(name = "huggingFaceEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter huggingFaceEmbeddingModelProviderAdapter() { return adapter("hugging-face", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.huggingface.HuggingFaceLanguageModel")
	@ConditionalOnMissingBean(name = "huggingFaceLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter huggingFaceLanguageModelProviderAdapter() { return adapter("hugging-face", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.huggingface.HuggingFaceLanguageModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.localai.LocalAiChatModel")
	@ConditionalOnMissingBean(name = "localAiChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter localAiChatModelProviderAdapter() { return adapter("local-ai", LangChain4jModelType.CHAT, "dev.langchain4j.model.localai.LocalAiChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.localai.LocalAiEmbeddingModel")
	@ConditionalOnMissingBean(name = "localAiEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter localAiEmbeddingModelProviderAdapter() { return adapter("local-ai", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.localai.LocalAiEmbeddingModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.localai.LocalAiLanguageModel")
	@ConditionalOnMissingBean(name = "localAiLanguageModelProviderAdapter")
	public LangChain4jModelProviderAdapter localAiLanguageModelProviderAdapter() { return adapter("local-ai", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.localai.LocalAiLanguageModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.googleai.GoogleAiGeminiChatModel")
	@ConditionalOnMissingBean(name = "geminiChatModelProviderAdapter")
	public LangChain4jModelProviderAdapter geminiChatModelProviderAdapter() { return adapter("gemini", LangChain4jModelType.CHAT, "dev.langchain4j.model.googleai.GoogleAiGeminiChatModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.googleai.GoogleAiEmbeddingModel")
	@ConditionalOnMissingBean(name = "geminiEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter geminiEmbeddingModelProviderAdapter() { return adapter("gemini", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.googleai.GoogleAiEmbeddingModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel")
	@ConditionalOnMissingBean(name = "voyageAiEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter voyageAiEmbeddingModelProviderAdapter() { return adapter("voyage-ai", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel"); }
	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.voyageai.VoyageAiScoringModel")
	@ConditionalOnMissingBean(name = "voyageAiScoringModelProviderAdapter")
	public LangChain4jModelProviderAdapter voyageAiScoringModelProviderAdapter() { return adapter("voyage-ai", LangChain4jModelType.SCORING, "dev.langchain4j.model.voyageai.VoyageAiScoringModel"); }

	@Bean
	@ConditionalOnClass(name = "dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel")
	@ConditionalOnMissingBean(name = "allMiniLmL6V2QuantizedEmbeddingModelProviderAdapter")
	public LangChain4jModelProviderAdapter allMiniLmL6V2QuantizedEmbeddingModelProviderAdapter() {
		return new ReflectiveLangChain4jModelProviderAdapter("all-mini-lm-l6-v2-quantized",
				LangChain4jModelType.EMBEDDING,
				"dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel", null);
	}

	@Bean
	@ConditionalOnMissingBean(name = "langChain4jAiModelAdapter")
	public AiModelAdapter langChain4jAiModelAdapter(List<LangChain4jModelProviderAdapter> adapters) {
		return new DefaultLangChain4jModelFactory(adapters);
	}

	private LangChain4jModelProviderAdapter adapter(String provider, LangChain4jModelType modelType,
													 String builderClassName) {
		return new ReflectiveLangChain4jModelProviderAdapter(provider, modelType, builderClassName, "builder");
	}

	private LangChain4jModelProviderAdapter azureAdapter(LangChain4jModelType modelType, String builderClassName) {
		return new ReflectiveLangChain4jModelProviderAdapter("azure-openai", modelType, builderClassName, "builder",
				Collections.singletonMap("endpoint", "baseUrl"));
	}
}
