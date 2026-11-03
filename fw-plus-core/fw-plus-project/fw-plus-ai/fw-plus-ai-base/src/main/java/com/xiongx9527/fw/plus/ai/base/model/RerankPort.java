package com.xiongx9527.fw.plus.ai.base.model;

import java.util.List;

/**
 * 重排模型端口：对候选文本按与 query 的相关度重新打分。
 *
 * <p>返回列表与 {@code documents} 下标一一对齐，分数取值 0~1。实现应保证长度一致；
 * 长度不足时调用方按「沿用原分」处理。</p>
 */
public interface RerankPort {

    List<Double> score(String query, List<String> documents);
}
