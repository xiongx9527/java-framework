package com.xiongx9527.fw.plus.ai.base.tool;

import java.util.List;

/**
 * 工具提供者：既给出规格（给模型看），也能执行调用。
 */
public interface ToolProvider extends ToolExecutor {

    /** 可用工具规格。 */
    List<ToolSpecification> specifications();
}
