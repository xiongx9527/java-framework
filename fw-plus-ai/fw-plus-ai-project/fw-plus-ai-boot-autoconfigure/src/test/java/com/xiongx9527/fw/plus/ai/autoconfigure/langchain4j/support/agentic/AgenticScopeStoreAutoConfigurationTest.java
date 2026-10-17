package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store.AgenticScopeStoreAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store.AgenticScopeStoreProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store.InMemoryAgenticScopeStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store.MongoAgenticScopeStore;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.agentic.scope.AgenticScopeSerializer;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import dev.langchain4j.agentic.scope.UnserializableAgenticScopeException;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * agentic scope 存储自动配置（{@code fw.plus.ai.langchain4j.agentic.scope-store}）：
 * 缺省装配进程内实现并安装为全局 {@code AgenticScopePersister}；
 * mongo / none / 未知取值 / 自定义 Bean 优先 / 反序列化白名单等分支行为。
 */
class AgenticScopeStoreAutoConfigurationTest {

    private static final String TYPE_PROPERTY = AgenticScopeStoreProperties.PREFIX + ".type";

    private static final InMemoryAgenticScopeStore USER_STORE = new InMemoryAgenticScopeStore();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AgenticScopeStoreAutoConfiguration.class));

    @BeforeEach
    void resetUserStore() {
        USER_STORE.clear();
    }

    @AfterEach
    void resetGlobalScopePersister() {
        // 进程级静态单例：清掉本类装配的 store,避免污染其它测试。
        AgenticScopePersister.setStore(null);
    }

    @Test
    void defaultTypeShouldAssembleInMemoryStoreAndInstallIt() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(InMemoryAgenticScopeStore.class);
            InMemoryAgenticScopeStore store = context.getBean(InMemoryAgenticScopeStore.class);

            invokeTinyPipeline();

            assertThat(store.size()).as("缺省 in-memory store 应被安装并参与 scope 持久化").isPositive();
            assertThat(store.getAllKeys())
                    .singleElement()
                    .satisfies(key -> assertThat(key.memoryId()).isEqualTo("scope-store-test"));
        });
    }

    @Test
    void mongoTypeShouldAssembleMongoStore() {
        runner.withPropertyValues(TYPE_PROPERTY + "=mongo").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(InMemoryAgenticScopeStore.class);
            assertThat(context.getBean(AgenticScopeStore.class)).isInstanceOf(MongoAgenticScopeStore.class);
        });
    }

    @Test
    void noneTypeShouldSkipAssemblyAndInstallation() {
        runner.withPropertyValues(TYPE_PROPERTY + "=none").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(AgenticScopeStore.class);
        });
    }

    @Test
    void unknownTypeShouldFailFast() {
        runner.withPropertyValues(TYPE_PROPERTY + "=redis").run(context -> assertThat(context)
                .getFailure()
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasStackTraceContaining("redis"));
    }

    @Test
    void userDefinedStoreShouldWinAndBeInstalled() {
        runner.withUserConfiguration(UserStoreConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(AgenticScopeStore.class);
            assertThat(context.getBean(AgenticScopeStore.class)).isSameAs(USER_STORE);

            invokeTinyPipeline();

            assertThat(USER_STORE.size()).as("使用方自定义 Bean 应被安装为全局 store").isPositive();
        });
    }

    @Test
    void serializationWhitelistShouldBeAppliedAtStartup() {
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        scope.writeState("probe", new WhitelistProbe("hello"));
        String json = AgenticScopeSerializer.toJson(scope);

        assertThatThrownBy(() -> AgenticScopeSerializer.fromJson(json))
                .as("未注册白名单前，业务类型应被反序列化拦截")
                .isInstanceOf(UnserializableAgenticScopeException.class);

        runner.withPropertyValues(AgenticScopeStoreProperties.PREFIX
                        + ".serialization.allow-package-prefixes[0]=" + getClass().getPackageName() + ".")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DefaultAgenticScope restored = AgenticScopeSerializer.fromJson(json);
                    assertThat(((WhitelistProbe) restored.readState("probe")).value()).isEqualTo("hello");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class UserStoreConfiguration {

        @Bean
        AgenticScopeStore userAgenticScopeStore() {
            return USER_STORE;
        }
    }

    /** 白名单探针：非 JDK 类型，序列化可行、反序列化默认被拦。 */
    public static class WhitelistProbe {

        private String value;

        public WhitelistProbe() {
        }

        WhitelistProbe(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /** 带 memoryId 的 typed 根编排：root scope 为 PERSISTENT，调用结束后保留在 store 中。 */
    public interface StoredPipeline {
        @Agent
        String run(@V("q") String q, @MemoryId String memoryId);
    }

    /** tiny 步：直接返回文本并写入 outputKey（不依赖模型调用）。 */
    public static class TinyStep {
        @Agent(name = "tiny", description = "scope 存储用例", outputKey = "answer")
        public String step(@V("q") String q) {
            return "OUT";
        }
    }

    private static void invokeTinyPipeline() {
        StoredPipeline pipeline = AgenticWorkflows.sequence(StoredPipeline.class)
                .subAgents(new TinyStep())
                .outputKey("answer")
                .build();

        pipeline.run("hi", "scope-store-test");
    }
}
