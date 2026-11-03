package com.xiongx9527.fw.plus.ai.base.resilience;

import java.util.List;

/**
 * 故障转移：给定当前模型，返回按优先级排列的候选链（含自身，按顺序尝试）。
 */
public interface ModelFailoverPort {

    List<String> candidates(String modelId);
}
