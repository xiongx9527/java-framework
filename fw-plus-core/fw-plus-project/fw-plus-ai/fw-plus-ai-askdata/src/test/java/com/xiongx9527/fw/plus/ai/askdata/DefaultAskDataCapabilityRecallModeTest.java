package com.xiongx9527.fw.plus.ai.askdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenInput;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenResult;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationCapability;
import com.xiongx9527.fw.plus.jdbc.execution.QueryResult;
import com.xiongx9527.fw.plus.jdbc.execution.SqlExecInput;
import com.xiongx9527.fw.plus.jdbc.execution.SqlExecutionCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallInput;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaCandidateProvider;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.support.retrieval.IndexMode;
import org.junit.jupiter.api.Test;

/**
 * Verifies AskDataInput.recallIndexMode is threaded into the recall step (SchemaRecallInput.indexMode),
 * defaulting to HIGH_QUALITY when not set.
 */
public class DefaultAskDataCapabilityRecallModeTest {

    static class SpyRecall implements SchemaRecallCapability {
        SchemaRecallInput last;

        @Override
        public SchemaContext recall(SchemaRecallInput input) {
            last = input;
            return SchemaContext.builder().build();
        }
    }

    static class StubGen implements SqlGenerationCapability {
        @Override
        public SqlGenResult generate(SqlGenInput in) {
            return SqlGenResult.builder().valid(false).sql("x").build();
        }
    }

    static class StubExec implements SqlExecutionCapability {
        @Override
        public QueryResult execute(SqlExecInput in) {
            return QueryResult.builder().build();
        }
    }

    @Test
    void economicalModeIsPlumbed() {
        SpyRecall recall = new SpyRecall();
        DefaultAskDataCapability cap = new DefaultAskDataCapability(emptyCandidates(), recall, new StubGen(), new StubExec());
        cap.ask(AskDataInput.builder().question("q").recallIndexMode(IndexMode.ECONOMICAL).build());
        assertEquals(IndexMode.ECONOMICAL, recall.last.getIndexMode());
    }

    @Test
    void defaultModeIsHighQuality() {
        SpyRecall recall = new SpyRecall();
        DefaultAskDataCapability cap = new DefaultAskDataCapability(emptyCandidates(), recall, new StubGen(), new StubExec());
        cap.ask(AskDataInput.builder().question("q").build());
        assertEquals(IndexMode.HIGH_QUALITY, recall.last.getIndexMode());
    }

    private SchemaCandidateProvider emptyCandidates() {
        return query -> java.util.Collections.emptyList();
    }
}
