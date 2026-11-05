package com.xiongx9527.fw.plus.jdbc.execution;

import java.util.List;

/**
 * 执行后结果自检（VERIFY，§6.20.5 / P1 #18）：空结果 / 截断 / 主键重复。
 * 仅产出可疑项提示，不修改结果，供上游决定反问还是降级。
 */
public interface ResultVerifier {
    List<String> verify(QueryResult result, ExecutionSettings settings);
}

