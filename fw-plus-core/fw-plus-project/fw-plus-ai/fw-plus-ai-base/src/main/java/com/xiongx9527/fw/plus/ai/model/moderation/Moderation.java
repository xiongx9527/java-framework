package com.xiongx9527.fw.plus.ai.model.moderation;

import java.io.Serializable;
import java.util.Objects;

/**
 * 内容审核结果。
 *
 * <p><b>作用</b>：只表达「是否被标记」与「被标记的原文」，不承载具体违规类别；
 * 具体类别由各厂商在响应元数据中自行扩展，避免核心模型绑定某一家分类体系。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.moderation.Moderation}。</p>
 *
 * <p><b>实现约定</b>：不可变且可序列化；{@link #flaggedText()} 仅在被标记时有意义。</p>
 *
 * @author xiongxiao
 */
public class Moderation implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean flagged;
    private final String flaggedText;

    /**
     * 构造「未标记」结果。
     */
    public Moderation() {
        this(false, null);
    }

    /**
     * 构造「被标记」结果。
     *
     * @param flaggedText 被标记的原文
     */
    public Moderation(String flaggedText) {
        this(true, flaggedText);
    }

    private Moderation(boolean flagged, String flaggedText) {
        this.flagged = flagged;
        this.flaggedText = flaggedText;
    }

    /**
     * 是否被标记为违规。
     *
     * @return 被标记返回 {@code true}
     */
    public boolean flagged() {
        return flagged;
    }

    /**
     * 被标记的原文。
     *
     * @return 原文；未标记时为 {@code null}
     */
    public String flaggedText() {
        return flaggedText;
    }

    /**
     * 创建「被标记」结果。
     *
     * @param flaggedText 被标记的原文
     * @return 审核结果
     */
    public static Moderation flagged(String flaggedText) {
        return new Moderation(flaggedText);
    }

    /**
     * 创建「未标记」结果。
     *
     * @return 审核结果
     */
    public static Moderation notFlagged() {
        return new Moderation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Moderation)) {
            return false;
        }
        Moderation that = (Moderation) o;
        return flagged == that.flagged && Objects.equals(flaggedText, that.flaggedText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flagged, flaggedText);
    }

    @Override
    public String toString() {
        return "Moderation{flagged=" + flagged + ", flaggedText='" + flaggedText + "'}";
    }
}
