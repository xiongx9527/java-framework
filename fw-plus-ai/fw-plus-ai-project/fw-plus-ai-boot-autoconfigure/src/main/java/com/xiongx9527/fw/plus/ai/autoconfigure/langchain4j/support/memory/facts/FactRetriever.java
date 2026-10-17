package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import java.util.List;

/**
 * 事实召回策略：按本轮查询从用户维度事实集合中取回相关事实（服务端排序或本地评分）。
 *
 * <p>失败语义：可预期失败（无命中、空输入）返回空列表；检索源异常允许向上抛出，
 * 由读路径统一隔离（{@code FactContextProvider} 视为空来源，不中断本轮）。</p>
 */
public interface FactRetriever {

	/**
	 * 召回与查询相关的事实。
	 *
	 * @param query  本轮用户输入（原样传入，策略自行处理）
	 * @param userId memId 段[3]（显式用户段）
	 * @param topK   返回条数上限（&gt;= 1）
	 */
	List<String> retrieve(String query, String userId, int topK);

}
