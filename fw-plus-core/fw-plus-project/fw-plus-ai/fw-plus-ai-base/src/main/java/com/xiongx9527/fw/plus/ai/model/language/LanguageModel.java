package com.xiongx9527.fw.plus.ai.model.language;

import com.xiongx9527.fw.plus.ai.model.output.Response;
import java.util.Arrays;
import java.util.List;

/** 旧式文本补全模型端口，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.language.LanguageModel}（legacy，非聊天；Spring AI 无直接等价）。 */
public interface LanguageModel {
    default Response<String> generate(String prompt) { return generate(Arrays.asList(prompt)); }
    default Response<String> generate(List<String> prompts) { throw new UnsupportedOperationException(); }
}
