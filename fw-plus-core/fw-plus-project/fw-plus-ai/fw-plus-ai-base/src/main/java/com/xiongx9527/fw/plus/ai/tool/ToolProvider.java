package com.xiongx9527.fw.plus.ai.tool;

import java.util.List;

/** 工具来源端口，镜像 langchain4j 的 {@code ToolProvider}（Spring AI 等价：{@code ToolCallback} 集合 / {@code MethodToolCallback}）。 */
public interface ToolProvider {
    List<ToolSpecification> provide();
}
