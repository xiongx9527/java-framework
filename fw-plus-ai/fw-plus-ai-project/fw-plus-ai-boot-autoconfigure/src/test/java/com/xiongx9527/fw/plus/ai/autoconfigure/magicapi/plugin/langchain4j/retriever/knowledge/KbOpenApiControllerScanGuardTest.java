package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.auth.KbSaTokenAuthChecker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDatasetService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbDocumentService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbMetadataFieldService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSearchService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSegmentService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web.KbOpenApiController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 对外 API 控制器的组件扫描护栏（回归：应用把 autoconfigure 包纳入 @ComponentScan 后启动失败）。
 *
 * <p>{@link KbOpenApiController} 带 {@code @RestController}，一旦被应用的组件扫描直接注册，而知识库
 * 服务层整体未装配（无 SqlSessionFactory 或总开关关闭），就会在启动期报
 * {@code Parameter 0 of constructor in KbOpenApiController required a bean of type 'KbDocumentService'
 * that could not be found}。类级条件与 {@link KnowledgeConfiguration} 对齐后，误扫描不再产生该类 Bean。</p>
 */
class KbOpenApiControllerScanGuardTest {

    private static final String WEB_PACKAGE =
            "com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.web";

    private final ApplicationContextRunner scanRunner = new ApplicationContextRunner()
            .withUserConfiguration(ScanKnowledgeWebPackage.class);

    @Test
    void 知识库未装配时误扫描不注册控制器() {
        scanRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(KbOpenApiController.class);
        });
    }

    @Test
    void 开关关闭时误扫描不注册控制器() {
        scanRunner.withPropertyValues("fw.plus.ai.knowledge.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(KbOpenApiController.class);
        });
    }

    @Test
    void 服务层齐备时误扫描仍能注册控制器() {
        scanRunner.withBean(KbDocumentService.class, () -> mock(KbDocumentService.class))
                .withBean(KbSearchService.class, () -> mock(KbSearchService.class))
                .withBean(KbDatasetService.class, () -> mock(KbDatasetService.class))
                .withBean(KbSegmentService.class, () -> mock(KbSegmentService.class))
                .withBean(KbMetadataFieldService.class, () -> mock(KbMetadataFieldService.class))
                .withBean(KbSaTokenAuthChecker.class, () -> mock(KbSaTokenAuthChecker.class))
                .withBean(com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService.class,
                        () -> mock(com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(KbOpenApiController.class);
                });
    }

    @Test
    void 控制器携带与知识库装配一致的类级条件() {
        assertThat(KbOpenApiController.class.getAnnotation(ConditionalOnClass.class))
                .as("与 KnowledgeConfiguration 同源的类型条件").isNotNull();

        ConditionalOnBean onBean = KbOpenApiController.class.getAnnotation(ConditionalOnBean.class);
        assertThat(onBean).as("服务层未装配时不注册").isNotNull();
        assertThat(onBean.value()).contains(KbDocumentService.class);
    }

    /** 模拟应用误把 autoconfigure 包纳入组件扫描。 */
    @Configuration(proxyBeanMethods = false)
    @ComponentScan(WEB_PACKAGE)
    static class ScanKnowledgeWebPackage {
    }
}