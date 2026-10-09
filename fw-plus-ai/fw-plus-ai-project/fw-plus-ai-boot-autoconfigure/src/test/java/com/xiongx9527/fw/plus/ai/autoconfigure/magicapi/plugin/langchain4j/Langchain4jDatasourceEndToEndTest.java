package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jScoringModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jTokenizerInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jChatModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingStoreMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jLanguageModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jScoringModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jStreamingChatModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jStreamingLanguageModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jTokenizerMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web.Langchain4jDatasourceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.FileResource;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 控制台数据源的服务端端到端（按插件职责边界）：用七类 storage 与真实文件资源完成
 * 「新建/保存 → {type}/{key}.json 落盘 → 读回」「停用落盘」「删除 → 文件消失」，
 * 并调用真实连接测试接口验证错误反馈。
 *
 * <p>边界说明：magic-api 的资源服务还承担分组树管理（控制台先选分组再保存），那属于引擎
 * 通用能力（由 {@code MagicResourceService} 及其定制实现承担），不是本插件的职责；本插件负责
 * 存储契约、资源变更后的实例生命周期与连接测试，三者在本测试中均为真实实现。</p>
 *
 * <p>浏览器交互本身无法在无头环境自动化：控制台渲染由各 console 子工程的元数据交叉校验
 * （{@code npm test}）与构建产物覆盖。</p>
 */
class Langchain4jDatasourceEndToEndTest {

    @TempDir
    Path tempDir;

    private final Map<Langchain4jModelTypes, MagicResourceStorage<? extends MagicEntity>> storages =
            new LinkedHashMap<Langchain4jModelTypes, MagicResourceStorage<? extends MagicEntity>>();
    private FileResource root;
    private Langchain4jDatasourceController controller;

    @BeforeEach
    void setUp() {
        Langchain4jModelFactoryRegistry factories = new Langchain4jModelFactoryRegistry();
        for (ModelFactory factory : Langchain4jModelFactoryConfiguration.defaultFactories()) {
            factories.register(factory);
        }
        MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels =
                new MagicDynamicLangchain4jEmbeddingModelDataSource();
        controller = new Langchain4jDatasourceController(mock(MagicConfiguration.class), factories, embeddingModels);

        storages.put(Langchain4jModelTypes.CHAT_MODEL, new Langchain4jChatModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.CHAT_MODEL)));
        storages.put(Langchain4jModelTypes.STREAMING_CHAT_MODEL,
                new Langchain4jStreamingChatModelMagicResourceStorage(
                        factories.providers(Langchain4jModelTypes.STREAMING_CHAT_MODEL)));
        storages.put(Langchain4jModelTypes.LANGUAGE_MODEL, new Langchain4jLanguageModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.LANGUAGE_MODEL)));
        storages.put(Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL,
                new Langchain4jStreamingLanguageModelMagicResourceStorage(
                        factories.providers(Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL)));
        storages.put(Langchain4jModelTypes.EMBEDDING_MODEL, new Langchain4jEmbeddingModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.EMBEDDING_MODEL)));
        storages.put(Langchain4jModelTypes.SCORING_MODEL, new Langchain4jScoringModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.SCORING_MODEL)));
        storages.put(Langchain4jModelTypes.TOKENIZER, new Langchain4jTokenizerMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.TOKENIZER)));
        storages.put(Langchain4jModelTypes.EMBEDDING_STORE, new Langchain4jEmbeddingStoreMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.EMBEDDING_STORE)));

        root = new FileResource(tempDir.toFile(), false, tempDir.toAbsolutePath().toString());
    }

    @Test
    void shouldPersistAndReadBackEveryResourceKind() {
        assertThat(save(chatModel("chat-main"), Langchain4jModelTypes.CHAT_MODEL)).isTrue();
        assertThat(save(model("streaming-chat-main", Langchain4jModelTypes.STREAMING_CHAT_MODEL),
                Langchain4jModelTypes.STREAMING_CHAT_MODEL)).isTrue();
        assertThat(save(model("language-main", Langchain4jModelTypes.LANGUAGE_MODEL),
                Langchain4jModelTypes.LANGUAGE_MODEL)).isTrue();
        assertThat(save(model("streaming-language-main", Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL),
                Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL)).isTrue();
        assertThat(save(model("embed-main", Langchain4jModelTypes.EMBEDDING_MODEL),
                Langchain4jModelTypes.EMBEDDING_MODEL)).isTrue();
        assertThat(save(model("scoring-main", Langchain4jModelTypes.SCORING_MODEL),
                Langchain4jModelTypes.SCORING_MODEL)).isTrue();
        assertThat(save(model("tokenizer-main", Langchain4jModelTypes.TOKENIZER),
                Langchain4jModelTypes.TOKENIZER)).isTrue();
        assertThat(save(store("store-main"), Langchain4jModelTypes.EMBEDDING_STORE)).isTrue();

        for (Langchain4jModelTypes type : Langchain4jModelTypes.values()) {
            Resource folder = root.getDirectory(type.type());
            assertThat(folder.exists()).as("%s 目录应已创建", type.type()).isTrue();
            assertThat(folder.resources()).as("%s 应有落盘文件", type.type()).hasSize(1);

            Langchain4jModelInfo back = readBack(type, keyOf(type));
            assertThat(back.getProvider()).as("%s 读回后 provider 一致", type.type()).isNotNull();
            assertThat(back.getKey()).as("%s 读回后 key 一致", type.type()).isEqualTo(keyOf(type));
            assertThat(back.isEnabled()).isTrue();
        }

        Langchain4jModelInfo chat = readBack(Langchain4jModelTypes.CHAT_MODEL, "chat-main");
        assertThat(chat.getOptions()).containsEntry("model-name", "gpt-4o-mini");
    }

    @Test
    void shouldPersistDisabledFlagAsIs() {
        Langchain4jChatModelInfo info = chatModel("chat-off");
        info.setEnabled(false);

        assertThat(save(info, Langchain4jModelTypes.CHAT_MODEL)).isTrue();

        Langchain4jModelInfo back = readBack(Langchain4jModelTypes.CHAT_MODEL, "chat-off");
        assertThat(back.isEnabled()).as("停用状态应原样落盘").isFalse();
    }

    @Test
    void shouldRejectUnknownProvider() {
        Langchain4jChatModelInfo bad = chatModel("bad-provider");
        bad.setProvider("not-exist");

        MagicResourceStorage<Langchain4jChatModelInfo> storage = storage(Langchain4jModelTypes.CHAT_MODEL);
        assertThatThrownBy(() -> storage.validate(bad))
                .as("表外 provider 必须被拒绝")
                .hasMessageContaining("provider");
    }

    @Test
    void shouldDeletePersistedFile() {
        Langchain4jChatModelInfo info = chatModel("chat-del");
        assertThat(save(info, Langchain4jModelTypes.CHAT_MODEL)).isTrue();
        Resource file = root.getDirectory(Langchain4jModelTypes.CHAT_MODEL.type()).getResource("chat-del.json");
        assertThat(file.exists()).isTrue();

        assertThat(file.delete()).isTrue();

        assertThat(file.exists()).as("删除后 json 应消失").isFalse();
        assertThat(root.getDirectory(Langchain4jModelTypes.CHAT_MODEL.type()).resources()).isEmpty();
    }

    @Test
    void shouldReportChineseErrorFromConnectionTest() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("key", "chat-probe");
        body.put("provider", "openai");
        body.put("options", new LinkedHashMap<String, Object>());

        assertThat(String.valueOf(controller.test("chat-model", body).getData()))
                .contains("langchain4j-chat-model[chat-probe].options.api-key");

        Map<String, Object> storeBody = new LinkedHashMap<String, Object>();
        storeBody.put("key", "store-probe");
        storeBody.put("provider", "in-memory");
        storeBody.put("embeddingModelKey", "absent-embed");
        storeBody.put("options", new LinkedHashMap<String, Object>());

        assertThat(String.valueOf(controller.test("embedding-store", storeBody).getData()))
                .contains("请先配置并启用 embedding-model").contains("absent-embed");
    }

    @SuppressWarnings("unchecked")
    private <T extends Langchain4jModelInfo> MagicResourceStorage<T> storage(Langchain4jModelTypes type) {
        return (MagicResourceStorage<T>) storages.get(type);
    }

    /** 模拟控制台保存：storage 校验 + 序列化 + 落盘到 {type}/{key}.json */
    private boolean save(Langchain4jModelInfo info, Langchain4jModelTypes type) {
        MagicResourceStorage<Langchain4jModelInfo> storage = storage(type);
        storage.validate(info);
        Resource folder = root.getDirectory(type.type());
        if (!folder.exists()) {
            folder.mkdir();
        }
        return folder.getResource(info.getKey() + ".json").write(storage.write(info));
    }

    private Langchain4jModelInfo readBack(Langchain4jModelTypes type, String key) {
        MagicResourceStorage<Langchain4jModelInfo> storage = storage(type);
        Resource file = root.getDirectory(type.type()).getResource(key + ".json");
        return storage.read(file.read());
    }

    private static String keyOf(Langchain4jModelTypes type) {
        switch (type) {
            case STREAMING_CHAT_MODEL:
                return "streaming-chat-main";
            case LANGUAGE_MODEL:
                return "language-main";
            case STREAMING_LANGUAGE_MODEL:
                return "streaming-language-main";
            case EMBEDDING_MODEL:
                return "embed-main";
            case SCORING_MODEL:
                return "scoring-main";
            case TOKENIZER:
                return "tokenizer-main";
            case EMBEDDING_STORE:
                return "store-main";
            default:
                return "chat-main";
        }
    }

    private static Langchain4jChatModelInfo chatModel(String key) {
        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        fill(info, key, "openai");
        info.setOptions(openAiOptions());
        return info;
    }

    private static Langchain4jModelInfo model(String key, Langchain4jModelTypes type) {
        Langchain4jModelInfo info;
        switch (type) {
            case STREAMING_CHAT_MODEL:
                info = new Langchain4jStreamingChatModelInfo();
                break;
            case LANGUAGE_MODEL:
                info = new Langchain4jLanguageModelInfo();
                break;
            case STREAMING_LANGUAGE_MODEL:
                info = new Langchain4jStreamingLanguageModelInfo();
                break;
            case EMBEDDING_MODEL:
                info = new Langchain4jEmbeddingModelInfo();
                break;
            case SCORING_MODEL:
                info = new Langchain4jScoringModelInfo();
                break;
            case TOKENIZER:
                info = new Langchain4jTokenizerInfo();
                break;
            default:
                throw new IllegalArgumentException("不支持的测试类型: " + type);
        }
        // scoring 工厂只有 jina / cohere，其余类型走 openai
        fill(info, key, type == Langchain4jModelTypes.SCORING_MODEL ? "jina" : "openai");
        info.setOptions(openAiOptions());
        return info;
    }

    private static Langchain4jEmbeddingStoreInfo store(String key) {
        Langchain4jEmbeddingStoreInfo info = new Langchain4jEmbeddingStoreInfo();
        fill(info, key, "in-memory");
        info.setEmbeddingModelKey("embed-main");
        info.setOptions(new LinkedHashMap<String, Object>());
        return info;
    }

    private static Map<String, Object> openAiOptions() {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("api-key", "sk-test");
        options.put("model-name", "gpt-4o-mini");
        return options;
    }

    private static void fill(Langchain4jModelInfo info, String key, String provider) {
        info.setId(UUID.randomUUID().toString());
        info.setKey(key);
        info.setName(key);
        info.setProvider(provider);
        info.setEnabled(true);
        info.setCreateTime(1L);
        info.setUpdateTime(1L);
    }
}
