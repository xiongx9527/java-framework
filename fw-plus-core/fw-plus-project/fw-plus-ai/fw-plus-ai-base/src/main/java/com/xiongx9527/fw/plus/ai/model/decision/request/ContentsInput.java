package com.xiongx9527.fw.plus.ai.model.decision.request;

import com.xiongx9527.fw.plus.ai.message.Content;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 多模态内容型决策输入。
 *
 * <p><b>作用</b>：以图片 / 音频 / 文本等内容块作为决策依据，支持多模态决策模型。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.ContentsInput}。</p>
 *
 * @author xiongxiao
 */
public final class ContentsInput implements DecisionInput {

    private final List<Content> contents;

    private ContentsInput(List<? extends Content> contents) {
        this.contents = Collections.unmodifiableList(new java.util.ArrayList<Content>(contents));
    }

    /**
     * 由内容块创建。
     *
     * @param contents 内容块列表
     * @return 输入对象
     */
    public static ContentsInput from(List<? extends Content> contents) {
        return new ContentsInput(contents);
    }

    /**
     * 内容块列表。
     *
     * @return 只读列表，非 {@code null}
     */
    public List<Content> contents() {
        return contents;
    }

    @Override
    public DecisionInputType type() {
        return DecisionInputType.CONTENTS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContentsInput)) {
            return false;
        }
        return Objects.equals(contents, ((ContentsInput) o).contents);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(contents);
    }

    @Override
    public String toString() {
        return "ContentsInput{contents=" + contents.size() + "}";
    }
}
