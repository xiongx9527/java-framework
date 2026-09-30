package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jScoringModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * {@link ScoringModel} 的动态代理：持有脚本模块与资源 key，每次调用实时解析当前实例，
 * magic 资源热更新/注销后不持有失效引用。解析失败回落容器中的重排模型 bean
 * （key 空取默认、非空按 bean 名），与 {@code MagicDynamicChatMemoryStore} 的兜底语义一致。
 */
public class MagicDynamicScoringModel implements ScoringModel {

    private final Langchain4jScoringModelModule scoringModelModule;

    private final String key;

    public MagicDynamicScoringModel(Langchain4jScoringModelModule scoringModelModule, String key) {
        this.scoringModelModule = scoringModelModule;
        this.key = key;
    }

    private ScoringModel getScoringModel() {
        try {
            return scoringModelModule.getDynamicAttribute(key);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(ScoringModel.class);
            }
            return SpringUtils.getBean(key, ScoringModel.class);
        }
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        return getScoringModel().scoreAll(segments, query);
    }
}
