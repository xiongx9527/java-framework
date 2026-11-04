package com.xiongx9527.fw.plus.ai.interactive;

/**
 * 答案抽取策略（Strategy）：从用户的一句话里抽取出某道题的答案。
 *
 * <p>框架内置规则实现 {@link DefaultAnswerExtractor}（选项按 id/label 匹配、数字解析、文本正则或整句）。
 * 若需要更智能的抽取（例如"下周三"归一为日期），提供一个基于模型的实现替换即可，接口不变。</p>
 */
public interface AnswerExtractor {

    /**
     * 针对单道题从 {@code utterance} 抽取答案。
     *
     * @param question 当前要抽的题
     * @param utterance 用户说的话
     * @return 抽取结果；不确定时 {@link ExtractionResult#isCertain()} 为 {@code false}
     */
    ExtractionResult extract(Question question, String utterance);
}
