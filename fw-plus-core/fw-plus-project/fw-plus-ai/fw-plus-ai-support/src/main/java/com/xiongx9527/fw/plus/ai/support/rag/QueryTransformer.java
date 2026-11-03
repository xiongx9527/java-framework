package com.xiongx9527.fw.plus.ai.support.rag;

import java.util.List;

/**
 * 查询改写：一个原始问题 → 一个或多个检索用查询（同义扩展 / 子问题拆分）。
 */
public interface QueryTransformer {

    List<String> transform(String query);
}
