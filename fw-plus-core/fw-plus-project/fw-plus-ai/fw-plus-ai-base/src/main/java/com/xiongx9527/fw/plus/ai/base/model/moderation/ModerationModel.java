package com.xiongx9527.fw.plus.ai.base.model.moderation;

import java.util.List;

/**
 * 审核模型端口：判断一段文本是否违规。
 *
 * <p>默认关闭的能力：主链不调用它，需要时由 adapter 注入实现（本地词表 / 厂商审核接口）。</p>
 */
public interface ModerationModel {

    ModerationResult moderate(String text);

    /** 批量审核；默认逐个调用，实现方可覆盖为一次请求。 */
    default List<ModerationResult> moderateAll(List<String> texts) {
        java.util.List<ModerationResult> results = new java.util.ArrayList<ModerationResult>();
        if (texts == null) {
            return results;
        }
        for (String text : texts) {
            results.add(moderate(text));
        }
        return results;
    }
}
