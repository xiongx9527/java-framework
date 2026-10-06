package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSetting;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link KbSettingService} 的默认值与归一化测试。
 *
 * <p>重点两件事：
 * <ol>
 * <li>{@code defaults()} 必须与原 {@code KnowledgeProperties} / {@code kb_dataset} 的 DDL 默认一致——
 *     迁移到页面上配置后，「一行都没配」的新库行为不能因为改了默认值而变样。</li>
 * <li>切分/召回参数的非法取值必须在保存时就被拒：一个 0 或负的重叠长度要到切分时才炸，
 *     现场比保存时难查得多。</li>
 * </ol>
 */
class KbSettingDefaultsTest {

    private final KbSettingService service = new KbSettingService(null);

    @Test
    void 默认值与原yaml一致() {
        KbSetting defaults = KbSettingService.defaults();
        assertThat(defaults.getEnabled()).isTrue();
        assertThat(defaults.getStorageType()).isEqualTo("local");
        assertThat(defaults.getStoragePath()).isNull();
        assertThat(defaults.getS3Key()).isNull();
        assertThat(defaults.getHybridSearch()).isTrue();
        assertThat(defaults.getEsEnabled()).isFalse();
        assertThat(defaults.getEsKey()).isNull();
        assertThat(defaults.getEsIndexPrefix()).isEqualTo(KbSettingService.DEFAULT_ES_INDEX_PREFIX);
    }

    @Test
    void 模型默认值默认留空() {
        // 内置一个 key 只会指向一个多半不存在的资源，不如让页面显式配一次
        KbSetting defaults = KbSettingService.defaults();
        assertThat(defaults.getEmbeddingModelKey()).isNull();
        assertThat(defaults.getEmbeddingStoreKey()).isNull();
        assertThat(defaults.getScoringModelKey()).isNull();
        assertThat(defaults.getChatModelKey()).isNull();
    }

    @Test
    void 切分召回默认值与kb_dataset的DDL默认一致() {
        KbSetting defaults = KbSettingService.defaults();
        assertThat(defaults.getDefaultRetrievalTopK()).isEqualTo(4);
        assertThat(defaults.getDefaultRetrievalScoreThreshold()).isEqualByComparingTo("0.0000");
        assertThat(defaults.getDefaultSegmentMaxLength()).isEqualTo(1000);
        assertThat(defaults.getDefaultSegmentOverlap()).isEqualTo(200);
        assertThat(defaults.getDefaultSegmentDelimiter()).isNull();
        assertThat(defaults.getDefaultSegmentMode()).isEqualTo(KbSettingService.SEGMENT_MODE_GENERAL);
        assertThat(defaults.getDefaultIndexingTechnique())
                .isEqualTo(KbSettingService.INDEXING_TECHNIQUE_HIGH_QUALITY);
    }

    @Test
    void normalize补齐缺失的枚举与开关() {
        KbSetting input = new KbSetting();
        KbSetting out = normalize(input);
        assertThat(out.getEnabled()).isTrue();
        assertThat(out.getHybridSearch()).isTrue();
        assertThat(out.getStorageType()).isEqualTo("local");
        assertThat(out.getEsIndexPrefix()).isEqualTo(KbSettingService.DEFAULT_ES_INDEX_PREFIX);
        // 布尔与枚举有兜底，数值项是「不提供默认」——留 null 才轮到硬编码兜底
        assertThat(out.getDefaultSegmentMode()).isEqualTo(KbSettingService.SEGMENT_MODE_GENERAL);
        assertThat(out.getDefaultIndexingTechnique())
                .isEqualTo(KbSettingService.INDEXING_TECHNIQUE_HIGH_QUALITY);
        assertThat(out.getDefaultRetrievalTopK()).isNull();
        assertThat(out.getDefaultSegmentMaxLength()).isNull();
    }

    @Test
    void normalize去除空白并小写化存储介质() {
        KbSetting input = new KbSetting();
        input.setStorageType("  S3  ");
        input.setStoragePath("  /data/kb  ");
        input.setEmbeddingModelKey("  text-embedding  ");
        input.setDefaultSegmentDelimiter("   ");
        KbSetting out = normalize(input);
        assertThat(out.getStorageType()).isEqualTo("s3");
        assertThat(out.getStoragePath()).isEqualTo("/data/kb");
        assertThat(out.getEmbeddingModelKey()).isEqualTo("text-embedding");
        // 纯空白视为「不提供默认」
        assertThat(out.getDefaultSegmentDelimiter()).isNull();
    }

    @Test
    void normalize拒绝非法枚举与负数() {
        KbSetting badMode = new KbSetting();
        badMode.setDefaultSegmentMode("unknown");
        assertThatThrownBy(() -> normalize(badMode)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的默认分段模式");

        KbSetting badTopK = new KbSetting();
        badTopK.setDefaultRetrievalTopK(0);
        assertThatThrownBy(() -> normalize(badTopK)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("默认召回数量必须大于 0");

        KbSetting badOverlap = new KbSetting();
        badOverlap.setDefaultSegmentOverlap(-1);
        assertThatThrownBy(() -> normalize(badOverlap)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("默认分段重叠不能为负数");

        KbSetting badThreshold = new KbSetting();
        badThreshold.setDefaultRetrievalScoreThreshold(new java.math.BigDecimal("-0.1"));
        assertThatThrownBy(() -> normalize(badThreshold)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("默认分数阈值不能为负数");

        KbSetting badTechnique = new KbSetting();
        badTechnique.setDefaultIndexingTechnique("fast");
        assertThatThrownBy(() -> normalize(badTechnique)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的默认索引技术");
    }

    @Test
    void normalize接受合法的qa与parentChild默认模式() {
        for (String mode : KbSettingService.SEGMENT_MODES) {
            KbSetting input = new KbSetting();
            input.setDefaultSegmentMode(mode);
            assertThat(normalize(input).getDefaultSegmentMode()).isEqualTo(mode);
        }
    }

    @Test
    void 保存非法内容时先归一化再落库() {
        // 无 mapper 时归一化已失败说明落库根本到不了，这里确认 save 的前置校验顺序
        KbSetting input = new KbSetting();
        input.setStorageType("ftp");
        assertThatThrownBy(() -> service.save(input)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知的存储介质");
    }

    @Test
    void 保存内容为null时报明确错误() {
        assertThatThrownBy(() -> service.save(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("设置内容不能为空");
    }

    private KbSetting normalize(KbSetting input) {
        return service.normalize(input);
    }
}
