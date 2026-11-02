package com.xiongx9527.fw.plus.ai.model.decision.response;

/**
 * 拒绝回答。
 *
 * <p><b>作用</b>：当模型判定无法基于给定输入做出决策时返回，便于上层区分「无结论」与「有结论」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.response.RefusalAnswer}。</p>
 *
 * @author xiongxiao
 */
public final class RefusalAnswer implements DecisionAnswer {

    private static final RefusalAnswer INSTANCE = new RefusalAnswer();

    private RefusalAnswer() {
    }

    /**
     * 获取单例拒绝答案。
     *
     * @return 拒绝答案
     */
    public static RefusalAnswer of() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "RefusalAnswer{}";
    }
}
