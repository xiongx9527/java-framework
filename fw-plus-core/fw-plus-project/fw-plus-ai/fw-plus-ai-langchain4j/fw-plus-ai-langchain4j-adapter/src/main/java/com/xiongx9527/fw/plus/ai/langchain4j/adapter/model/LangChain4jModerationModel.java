package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationCategory;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationResult;
import dev.langchain4j.model.moderation.Moderation;
import dev.langchain4j.model.output.Response;

/**
 * 把 LangChain4j 0.35 的审核模型适配成 base 的 {@link ModerationModel}。
 *
 * <p>0.35 的 {@link Moderation} 只给出是否命中与命中文本，不带厂商分类，
 * 因此命中统一映射到 {@link ModerationCategory#OTHER}。</p>
 */
public class LangChain4jModerationModel implements ModerationModel {

    private final dev.langchain4j.model.moderation.ModerationModel model;

    public LangChain4jModerationModel(dev.langchain4j.model.moderation.ModerationModel model) {
        if (model == null) {
            throw new IllegalArgumentException("moderation model is required");
        }
        this.model = model;
    }

    @Override
    public ModerationResult moderate(String text) {
        if (text == null) {
            return ModerationResult.clean();
        }
        Response<Moderation> response = model.moderate(text);
        Moderation moderation = response == null ? null : response.content();
        if (moderation == null || !moderation.flagged()) {
            return ModerationResult.clean();
        }
        return ModerationResult.flagged(ModerationCategory.OTHER, moderation.flaggedText());
    }
}
