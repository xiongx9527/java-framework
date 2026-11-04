package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.support.retrieval.IndexMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Explicit input for the end-to-end data-QA orchestration (recall -> generate -> execute).
 */
@Getter
@Builder
public class AskDataInput {
    private final String question;
    private final List<TableSchema> candidates;
    private final String dialectHint;
    private final String priorSql;
    private final String refResolution;
    @Builder.Default
    private final List<String> constraints = Collections.emptyList();
    private final Integer seed;
    private final Double temperature;
    private final Double topP;
    @Builder.Default
    private final Map<String, Object> extraParams = Collections.emptyMap();
    @Builder.Default
    private final int maxRows = 1000;
    @Builder.Default
    private final int queryTimeoutSeconds = 30;
    @Builder.Default
    private final boolean readOnly = true;

    /** Recall index strategy: HIGH_QUALITY (quality) or ECONOMICAL. Default quality. */
    @Builder.Default
    private final IndexMode recallIndexMode = IndexMode.HIGH_QUALITY;
}
