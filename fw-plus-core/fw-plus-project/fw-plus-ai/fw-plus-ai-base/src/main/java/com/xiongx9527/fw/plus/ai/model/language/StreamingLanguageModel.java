package com.xiongx9527.fw.plus.ai.model.language;

import com.xiongx9527.fw.plus.ai.StreamingResponseHandler;
import java.util.Collections;
import java.util.List;

/** 旧式流式文本补全模型端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.language.StreamingLanguageModel}（legacy）。 */
public interface StreamingLanguageModel {
    default void generate(String prompt, StreamingResponseHandler<String> handler) { generate(Collections.singletonList(prompt), handler); }
    void generate(List<String> prompts, StreamingResponseHandler<String> handler);
}
