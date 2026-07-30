package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataLongTermMemory;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataMemoryEntry;

import java.util.List;

/**
 * Core 长期记忆 SPI 的自动配置层桥接。
 *
 * <p>桥接不重写或降级隔离条件，所有查询均将调用方提供的项目和操作人上下文完整传给 Core SPI。
 * 因此没有匹配记忆时不会回退到同项目的其他操作人。</p>
 */
public class AskDataMemoryRecallAdapter {

    private final AskDataLongTermMemory memory;

    public AskDataMemoryRecallAdapter(AskDataLongTermMemory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("memory 不能为空");
        }
        this.memory = memory;
    }

    /**
     * 按项目、操作人、数据源和语义版本查询长期记忆。
     *
     * @param context 当前请求的项目和操作人上下文
     * @param datasourceKey 数据源标识
     * @param semanticVersion 语义版本
     * @return 完全匹配的记忆条目
     */
    public List<AskDataMemoryEntry> recall(AskDataProjectContext context, String datasourceKey,
            String semanticVersion) {
        return memory.find(context, datasourceKey, semanticVersion);
    }
}
