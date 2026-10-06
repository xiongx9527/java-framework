package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbDataset;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.entity.KbSetting;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code KbDatasetService.applyDatasetDefaults} 的默认值回落测试。
 *
 * <p>这是「设置页配一次、建模时留空」能成立的全部依据，所以直接反射调私有方法验证三条回落规则：
 * <ol>
 * <li>调用方显式给的值永远不被覆盖；</li>
 * <li>更新场景下先回落数据集现有值，再回落全局设置；</li>
 * <li>设置也没配时落到原有硬编码常量——保证「设置表一行都没配」的老库行为不变。</li>
 * </ol>
 */
class KbDatasetDefaultsTest {

    private final KbDatasetService service = new KbDatasetService(null, null, null, null, null, null, null, null,
            null, settingService(new KbSetting()));

    @Test
    void 设置已配时补齐留空的切分与召回参数() {
        KbSetting setting = new KbSetting();
        setting.setDefaultRetrievalTopK(8);
        setting.setDefaultRetrievalScoreThreshold(new java.math.BigDecimal("0.2500"));
        setting.setDefaultSegmentMaxLength(600);
        setting.setDefaultSegmentOverlap(80);
        setting.setDefaultSegmentDelimiter("##");
        setting.setDefaultSegmentMode(KbSettingService.SEGMENT_MODE_PARENT_CHILD);
        KbDatasetService svc = new KbDatasetService(null, null, null, null, null, null, null, null,
                null, settingService(setting));

        KbDataset ds = new KbDataset();
        ds.setName("d");
        apply(svc, ds, null);

        assertThat(ds.getRetrievalTopK()).isEqualTo(8);
        assertThat(ds.getRetrievalScoreThreshold()).isEqualByComparingTo("0.2500");
        assertThat(ds.getSegmentMaxLength()).isEqualTo(600);
        assertThat(ds.getSegmentOverlap()).isEqualTo(80);
        assertThat(ds.getSegmentDelimiter()).isEqualTo("##");
        assertThat(ds.getSegmentMode()).isEqualTo(KbSettingService.SEGMENT_MODE_PARENT_CHILD);
    }

    @Test
    void 设置未配时落到硬编码兜底() {
        KbDataset ds = new KbDataset();
        ds.setName("d");
        apply(service, ds, null);

        assertThat(ds.getRetrievalTopK()).isEqualTo(KbSettingService.DEFAULT_RETRIEVAL_TOP_K);
        assertThat(ds.getRetrievalScoreThreshold()).isEqualByComparingTo("0.0000");
        assertThat(ds.getSegmentMaxLength()).isEqualTo(KbSettingService.DEFAULT_SEGMENT_MAX_LENGTH);
        assertThat(ds.getSegmentOverlap()).isEqualTo(KbSettingService.DEFAULT_SEGMENT_OVERLAP);
        assertThat(ds.getSegmentMode()).isEqualTo(KbSettingService.SEGMENT_MODE_GENERAL);
        assertThat(ds.getSegmentDelimiter()).isNull();
    }

    @Test
    void 显式值不被设置覆盖() {
        KbSetting setting = new KbSetting();
        setting.setDefaultRetrievalTopK(8);
        setting.setDefaultSegmentMaxLength(600);
        setting.setDefaultSegmentMode(KbSettingService.SEGMENT_MODE_PARENT_CHILD);
        setting.setEmbeddingModelKey("setting-embedding");
        setting.setEmbeddingStoreKey("setting-store");
        KbDatasetService svc = new KbDatasetService(null, null, null, null, null, null, null, null,
                null, settingService(setting));

        KbDataset ds = new KbDataset();
        ds.setName("d");
        ds.setRetrievalTopK(2);
        ds.setSegmentMaxLength(2000);
        ds.setSegmentMode(KbSettingService.SEGMENT_MODE_QA);
        ds.setEmbeddingModelKey("mine-embedding");
        ds.setEmbeddingStoreKey("mine-store");
        apply(svc, ds, null);

        assertThat(ds.getRetrievalTopK()).isEqualTo(2);
        assertThat(ds.getSegmentMaxLength()).isEqualTo(2000);
        assertThat(ds.getSegmentMode()).isEqualTo(KbSettingService.SEGMENT_MODE_QA);
        // 设置页改的是新建默认，不该篡改调用方显式指定的模型绑定
        assertThat(ds.getEmbeddingModelKey()).isEqualTo("mine-embedding");
        assertThat(ds.getEmbeddingStoreKey()).isEqualTo("mine-store");
    }

    @Test
    void 更新场景先回落数据集现有值再回落设置() {
        KbSetting setting = new KbSetting();
        setting.setDefaultRetrievalTopK(8);
        KbDatasetService svc = new KbDatasetService(null, null, null, null, null, null, null, null,
                null, settingService(setting));

        KbDataset existing = new KbDataset();
        existing.setRetrievalTopK(3);
        KbDataset update = new KbDataset();
        update.setRetrievalTopK(null);
        apply(svc, update, existing);

        // 现有值 3 优先于设置里的 8
        assertThat(update.getRetrievalTopK()).isEqualTo(3);
    }

    @Test
    void 更新场景现有值也为空时回落设置() {
        KbSetting setting = new KbSetting();
        setting.setDefaultRetrievalTopK(9);
        KbDatasetService svc = new KbDatasetService(null, null, null, null, null, null, null, null,
                null, settingService(setting));

        KbDataset existing = new KbDataset();
        KbDataset update = new KbDataset();
        apply(svc, update, existing);

        assertThat(update.getRetrievalTopK()).isEqualTo(9);
    }

    @Test
    void 模型key按传入值现有值设置顺序回落() {
        KbSetting setting = new KbSetting();
        setting.setEmbeddingModelKey("s-model");
        setting.setEmbeddingStoreKey("s-store");
        setting.setScoringModelKey("s-scoring");
        setting.setChatModelKey("s-chat");
        KbDatasetService svc = new KbDatasetService(null, null, null, null, null, null, null, null,
                null, settingService(setting));

        KbDataset existing = new KbDataset();
        existing.setEmbeddingModelKey("old-model");
        KbDataset update = new KbDataset();
        update.setEmbeddingModelKey("new-model");
        apply(svc, update, existing);

        assertThat(update.getEmbeddingModelKey()).isEqualTo("new-model");
        // existing 为空 → 用设置
        assertThat(update.getEmbeddingStoreKey()).isEqualTo("s-store");
        assertThat(update.getScoringModelKey()).isEqualTo("s-scoring");
        assertThat(update.getChatModelKey()).isEqualTo("s-chat");
    }

    private KbSettingService settingService(KbSetting setting) {
        // 直接把快照灌进 current 字段，省掉 mapper：单测只关心回落顺序，不关心读表
        KbSettingService svc = new KbSettingService(null);
        try {
            java.lang.reflect.Field currentField = KbSettingService.class.getDeclaredField("current");
            currentField.setAccessible(true);
            currentField.set(svc, setting);
            java.lang.reflect.Field loadedField = KbSettingService.class.getDeclaredField("loaded");
            loadedField.setAccessible(true);
            loadedField.setBoolean(svc, true);
            java.lang.reflect.Field lastLoadField = KbSettingService.class.getDeclaredField("lastLoadTime");
            lastLoadField.setAccessible(true);
            lastLoadField.setLong(svc, System.currentTimeMillis());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return svc;
    }

    private void apply(KbDatasetService svc, KbDataset dataset, KbDataset fallback) {
        svc.applyDatasetDefaults(dataset, fallback);
    }
}
