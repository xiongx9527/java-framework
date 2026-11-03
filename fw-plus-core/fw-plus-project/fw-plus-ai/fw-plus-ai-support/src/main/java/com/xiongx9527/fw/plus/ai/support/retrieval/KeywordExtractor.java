package com.xiongx9527.fw.plus.ai.support.retrieval;

import java.util.List;

/**
 * 关键词抽取：从问题里取用于关键字匹配的词。
 *
 * <p>默认实现按非字母数字切分；接入 LLM 抽取 / 同义扩展时替换本端口即可。</p>
 */
public interface KeywordExtractor {

    List<String> extract(String query);
}
