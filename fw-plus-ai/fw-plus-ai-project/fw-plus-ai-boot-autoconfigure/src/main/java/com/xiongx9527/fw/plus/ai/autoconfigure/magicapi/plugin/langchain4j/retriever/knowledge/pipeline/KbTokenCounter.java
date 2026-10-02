package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline;

/**
 * token 计数单一封装点（升级兼容约束，设计 D9）：
 * M1 用字符近似估计（1 token ≈ 1.6 字符，中文场景偏保守），不引入 Tokenizer 词表依赖；
 * 升级 langchain4j 1.x 时替换为 TokenCountEstimator 只改本类。
 */
public final class KbTokenCounter {

    private KbTokenCounter() {
    }

    /** 近似估算 token 数（JDK8 兼容，中文按 1 字 ≈ 1 token、英文按 4 字符 ≈ 1 token 的混合近似）。 */
    public static int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeScript.of(text.charAt(i)) == Character.UnicodeScript.HAN) {
                cjk++;
            } else {
                other++;
            }
        }
        return cjk + (int) Math.ceil(other / 4.0);
    }
}
