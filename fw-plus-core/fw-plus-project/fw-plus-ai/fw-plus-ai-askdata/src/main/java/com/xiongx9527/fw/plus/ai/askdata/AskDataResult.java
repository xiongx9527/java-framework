package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.jdbc.execution.QueryResult;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Output of the end-to-end data-QA orchestration.
 */
@Getter
@Builder
public class AskDataResult {
    private final String sql;
    private final String explanation;
    private final QueryResult queryResult;
    private final double confidence;
    private final boolean valid;
    @Builder.Default
    private final List<String> referencedTables = Collections.emptyList();
    @Builder.Default
    private final List<String> alternatives = Collections.emptyList();
    @Builder.Default
    private final List<String> warnings = Collections.emptyList();
    @Builder.Default
    private final boolean executed = false;
}
