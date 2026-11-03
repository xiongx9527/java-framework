package com.xiongx9527.fw.plus.ai.base.cost;

/**
 * 成本端口：把每次调用的用量落库 / 上报。实现失败不应影响主链路。
 */
public interface CostPort {

    void record(String modelId, CostUsage usage);
}
