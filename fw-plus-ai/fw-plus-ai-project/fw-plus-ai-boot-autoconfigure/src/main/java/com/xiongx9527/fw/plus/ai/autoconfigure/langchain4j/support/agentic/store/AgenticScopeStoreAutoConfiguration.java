package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.agentic.scope.AgenticScopeSerializer;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

/**
 * agentic scope 存储自动配置（经 {@code LangChain4jSupportAutoConfiguration} {@code @Import} 收口，
 * 不单独登记 {@code AutoConfiguration.imports}）。
 *
 * <p>按 {@code fw.plus.ai.langchain4j.agentic.scope-store.type} 装配
 * {@link AgenticScopeStore} 并<b>安装</b>到进程级 {@code AgenticScopePersister}：</p>
 * <ul>
 *     <li>{@code in-memory}（缺省）：{@link InMemoryAgenticScopeStore}；</li>
 *     <li>{@code mongo}：{@link MongoAgenticScopeStore}（需 {@code org.mongodb:mongodb-driver-sync} 在类路径）；</li>
 *     <li>{@code none}：不装配不安装，保持上游默认。</li>
 * </ul>
 *
 * <p><b>覆盖语义</b>：安装会覆盖上游 ServiceLoader（{@code META-INF/services}）发现的 store；
 * 使用方自定义 {@code AgenticScopeStore} Bean 时本配置退让（{@code @ConditionalOnMissingBean}），
 * 并把使用方的 Bean 安装为全局实现。反序列化白名单在安装前由
 * {@code serialization.allow-package-prefixes / allow-types} 统一注册（进程级永久生效）。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgenticScopeStoreProperties.class)
public class AgenticScopeStoreAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(AgenticScopeStoreAutoConfiguration.class);

    /**
     * {@code type} 取值校验：未知取值启动即失败，避免条件全不匹配导致 store 静默缺失、
     * 持久化配置无声失效。
     */
    @Bean
    public InitializingBean agenticScopeStoreTypeValidator(AgenticScopeStoreProperties properties) {
        return () -> {
            String type = properties.getType() == null
                    ? AgenticScopeStoreProperties.TYPE_IN_MEMORY
                    : properties.getType().trim();
            if (!AgenticScopeStoreProperties.TYPE_IN_MEMORY.equals(type)
                    && !AgenticScopeStoreProperties.TYPE_MONGO.equals(type)
                    && !AgenticScopeStoreProperties.TYPE_NONE.equals(type)) {
                throw new IllegalStateException(I18nMessageResolver.resolveKey(
                        "fw.ai.AgenticScopeStoreAutoConfiguration.typeInvalid",
                        AgenticScopeStoreProperties.PREFIX, properties.getType(),
                        AgenticScopeStoreProperties.TYPE_IN_MEMORY, AgenticScopeStoreProperties.TYPE_MONGO,
                        AgenticScopeStoreProperties.TYPE_NONE));
            }
        };
    }

    /** 安装器：应用反序列化白名单，并把装配的 store 安装为全局 {@code AgenticScopePersister}。 */
    @Bean
    public InitializingBean agenticScopeStoreInstaller(ObjectProvider<AgenticScopeStore> stores,
                                                       AgenticScopeStoreProperties properties) {
        return new AgenticScopeStoreInstaller(stores, properties);
    }

    /** 缺省进程内实现。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = AgenticScopeStoreProperties.PREFIX, name = "type",
            havingValue = AgenticScopeStoreProperties.TYPE_IN_MEMORY, matchIfMissing = true)
    @ConditionalOnMissingBean(AgenticScopeStore.class)
    static class InMemoryAgenticScopeStoreConfiguration {

        @Bean
        public InMemoryAgenticScopeStore agenticScopeStore() {
            return new InMemoryAgenticScopeStore();
        }
    }

    /** MongoDB 持久化实现（驱动缺失时跳过，由安装器 fail-fast 提示）。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.mongodb.client.MongoClient")
    @ConditionalOnProperty(prefix = AgenticScopeStoreProperties.PREFIX, name = "type",
            havingValue = AgenticScopeStoreProperties.TYPE_MONGO)
    @ConditionalOnMissingBean(AgenticScopeStore.class)
    static class MongoAgenticScopeStoreConfiguration {

        @Bean
        public MongoAgenticScopeStore agenticScopeStore(AgenticScopeStoreProperties properties) {
            AgenticScopeStoreProperties.Mongo mongo = properties.getMongo();
            return MongoAgenticScopeStore.builder()
                    .connectionString(mongo.getConnectionString())
                    .database(mongo.getDatabase())
                    .collection(mongo.getCollection())
                    .ttl(mongo.getTtl())
                    .build();
        }
    }

    static final class AgenticScopeStoreInstaller implements InitializingBean {

        private final ObjectProvider<AgenticScopeStore> stores;

        private final AgenticScopeStoreProperties properties;

        AgenticScopeStoreInstaller(ObjectProvider<AgenticScopeStore> stores, AgenticScopeStoreProperties properties) {
            this.stores = stores;
            this.properties = properties;
        }

        @Override
        public void afterPropertiesSet() {
            String type = properties.getType() == null
                    ? AgenticScopeStoreProperties.TYPE_IN_MEMORY
                    : properties.getType().trim();
            if (AgenticScopeStoreProperties.TYPE_NONE.equals(type)) {
                return;
            }
            applySerializationWhitelist(properties.getSerialization());
            AgenticScopeStore store = stores.getIfAvailable();
            if (store == null) {
                throw new IllegalStateException(I18nMessageResolver.resolveKey(
                        "fw.ai.AgenticScopeStoreAutoConfiguration.storeBeanMissing",
                        AgenticScopeStoreProperties.PREFIX, properties.getType()));
            }
            AgenticScopePersister.setStore(store);
            LOG.info("agentic scope store 已装配并安装为全局 AgenticScopePersister: {} (type={})",
                    store.getClass().getName(), type);
        }

        private static void applySerializationWhitelist(AgenticScopeStoreProperties.Serialization serialization) {
            if (serialization == null) {
                return;
            }
            List<String> prefixes = serialization.getAllowPackagePrefixes();
            if (prefixes != null) {
                prefixes.stream()
                        .filter(prefix -> prefix != null && !prefix.trim().isEmpty())
                        .forEach(AgenticScopeSerializer::allowDeserializationPackagePrefix);
            }
            List<Class<?>> types = serialization.getAllowTypes();
            if (types != null) {
                types.stream().filter(Objects::nonNull).forEach(AgenticScopeSerializer::allowDeserializationType);
            }
        }
    }
}
