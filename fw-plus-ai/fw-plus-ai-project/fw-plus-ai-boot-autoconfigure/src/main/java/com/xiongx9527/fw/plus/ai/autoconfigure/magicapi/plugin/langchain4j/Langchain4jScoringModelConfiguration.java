package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jScoringModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jScoringModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jScoringModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jScoringModelMagicResourceStorage;
import dev.langchain4j.model.scoring.ScoringModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** scoring-model 数据源的装配：实例持有者、资源存储与动态注册器。 */
@ConditionalOnClass(name = "dev.langchain4j.model.scoring.ScoringModel")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.scoring-model", name = "enabled",
        havingValue = "true", matchIfMissing = false)
@Configuration(proxyBeanMethods = false)
public class Langchain4jScoringModelConfiguration extends AbstractLangchain4jPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-scoring-model.1.0.0.iife.js";

    public Langchain4jScoringModelConfiguration() {
        super("langchain4j-scoring-model", "MagicLangchain4jScoringModel", JAVASCRIPT_FILENAME, false);
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jScoringModelDataSource langchain4jScoringModelDataSource() {
        return new MagicDynamicLangchain4jScoringModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jScoringModelMagicResourceStorage langchain4jScoringModelMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jScoringModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.SCORING_MODEL));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jScoringModelMagicDynamicRegistry langchain4jScoringModelMagicDynamicRegistry(
            Langchain4jScoringModelMagicResourceStorage storage,
            MagicDynamicLangchain4jScoringModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories,
            ObjectProvider<ScoringModel> defaultInstance) {
        return new Langchain4jScoringModelMagicDynamicRegistry(storage, dataSource, factories, defaultInstance);
    }

    /** 脚本模块：scoringModel['key'] 按 key 取实例；不指定 key 时回落到 Spring 默认实例 */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jScoringModelModule langchain4jScoringModelModule(
            MagicDynamicLangchain4jScoringModelDataSource scoringModels, ObjectProvider<ScoringModel> defaultInstance) {
        return new Langchain4jScoringModelModule(scoringModels, defaultInstance);
    }
}
