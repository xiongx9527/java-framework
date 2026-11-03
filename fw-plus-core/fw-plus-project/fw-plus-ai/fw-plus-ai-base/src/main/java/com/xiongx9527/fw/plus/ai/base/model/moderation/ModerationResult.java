package com.xiongx9527.fw.plus.ai.base.model.moderation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审核结果：是否命中 + 命中的类别 + 各类别分值。
 */
public final class ModerationResult {

    private static final ModerationResult CLEAN = new ModerationResult(false,
            Collections.<ModerationCategory>emptyList(), Collections.<String, Double>emptyMap(), null);

    private final boolean flagged;

    private final List<ModerationCategory> categories;

    private final Map<String, Double> scores;

    private final String reason;

    public ModerationResult(boolean flagged, List<ModerationCategory> categories,
                            Map<String, Double> scores, String reason) {
        this.flagged = flagged;
        this.categories = Collections.unmodifiableList(categories == null
                ? new ArrayList<ModerationCategory>() : new ArrayList<ModerationCategory>(categories));
        this.scores = Collections.unmodifiableMap(scores == null
                ? new LinkedHashMap<String, Double>() : new LinkedHashMap<String, Double>(scores));
        this.reason = reason;
    }

    /** 未命中。 */
    public static ModerationResult clean() {
        return CLEAN;
    }

    /** 命中指定类别。 */
    public static ModerationResult flagged(ModerationCategory category, String reason) {
        List<ModerationCategory> categories = new ArrayList<ModerationCategory>();
        if (category != null) {
            categories.add(category);
        }
        return new ModerationResult(true, categories, null, reason);
    }

    public boolean isFlagged() {
        return flagged;
    }

    public List<ModerationCategory> getCategories() {
        return categories;
    }

    public Map<String, Double> getScores() {
        return scores;
    }

    /** 命中说明；未命中为 {@code null}。 */
    public String getReason() {
        return reason;
    }
}
