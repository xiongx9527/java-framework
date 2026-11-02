package com.xiongx9527.fw.plus.ai.support.cost;

import java.math.BigDecimal;

/**
 * 成本端口：把每次模型调用的 token 与金额记账。
 *
 * <p><b>作用</b>：单价表随厂商与型号变化，金额折算不该散落在业务代码里。端口负责「按模型取单价」与
 * 「落一笔账」，实现决定单价来自配置、库表还是远端；落库粒度通常是「一次调用一行」，便于按租户 / 模型聚合。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer} 增强清单「成本核算」与 SLO 告警。
 * langchain4j / Spring AI 均无成本端口（只提供 token 用量）。</p>
 *
 * <p><b>实现约定</b>：{@link #record(CostUsage)} 不得抛异常阻塞主链路——记账失败只记日志；
 * 单价未知时 {@link #priceOf(String)} 返回 {@code null}，由调用方跳过金额。</p>
 *
 * @author xiongxiao
 */
public interface CostPort {

    /**
     * 记录一次调用的用量与金额。
     *
     * @param usage 用量
     */
    void record(CostUsage usage);

    /**
     * 查询模型单价（每千 token）。
     *
     * @param modelId 模型标识
     * @return 单价，未知时返回 {@code null}
     */
    BigDecimal priceOf(String modelId);
}
