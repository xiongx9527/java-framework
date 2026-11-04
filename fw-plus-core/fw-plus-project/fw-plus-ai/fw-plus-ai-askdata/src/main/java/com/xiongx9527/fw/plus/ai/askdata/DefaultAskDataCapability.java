package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenInput;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenResult;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationCapability;
import com.xiongx9527.fw.plus.ai.askdata.execution.QueryResult;
import com.xiongx9527.fw.plus.ai.askdata.execution.SqlExecInput;
import com.xiongx9527.fw.plus.ai.askdata.execution.SqlExecutionCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallInput;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.Collections;

/**
 * Default AskDataCapability: recall schema (optional), generate SQL with the NL2SQL capability,
 * and execute it with the execution capability. Returns the executed result plus the SQL and
 * confidence so callers can present or re-ask.
 */
public class DefaultAskDataCapability implements AskDataCapability {

    private final SchemaRecallCapability recall;
    private final SqlGenerationCapability generation;
    private final SqlExecutionCapability execution;

    public DefaultAskDataCapability(SqlGenerationCapability generation, SqlExecutionCapability execution) {
        this(null, generation, execution);
    }

    public DefaultAskDataCapability(SchemaRecallCapability recall, SqlGenerationCapability generation,
                                   SqlExecutionCapability execution) {
        if (generation == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.gen_capability_required"));
        }
        if (execution == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.exec_capability_required"));
        }
        this.recall = recall;
        this.generation = generation;
        this.execution = execution;
    }

    @Override
    public AskDataResult ask(AskDataInput input) {
        if (input == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.ask_data_input_required"));
        }
        SchemaContext schema;
        if (recall != null) {
            schema = recall.recall(SchemaRecallInput.builder()
                    .question(input.getQuestion())
                    .candidates(input.getCandidates())
                    .build());
        } else {
            schema = SchemaContext.builder()
                    .tables(input.getCandidates() == null ? Collections.emptyList() : input.getCandidates())
                    .build();
        }
        SqlGenInput.Builder gb = SqlGenInput.builder()
                .question(input.getQuestion())
                .schemaContext(schema)
                .dialectHint(input.getDialectHint())
                .priorSql(input.getPriorSql())
                .refResolution(input.getRefResolution())
                .constraints(input.getConstraints())
                .seed(input.getSeed())
                .temperature(input.getTemperature())
                .topP(input.getTopP())
                .extraParams(input.getExtraParams());
        SqlGenResult gen = generation.generate(gb.build());
        if (!gen.isValid()) {
            return AskDataResult.builder()
                    .sql(gen.getSql())
                    .explanation(gen.getExplanation())
                    .confidence(gen.getConfidence())
                    .valid(false)
                    .referencedTables(gen.getReferencedTables())
                    .alternatives(gen.getAlternatives())
                    .executed(false)
                    .build();
        }
        SqlExecInput.Builder eb = SqlExecInput.builder()
                .sql(gen.getSql())
                .dialectHint(input.getDialectHint())
                .maxRows(input.getMaxRows())
                .queryTimeoutSeconds(input.getQueryTimeoutSeconds())
                .readOnly(input.isReadOnly());
        QueryResult qr = execution.execute(eb.build());
        return AskDataResult.builder()
                .sql(gen.getSql())
                .explanation(gen.getExplanation())
                .queryResult(qr)
                .confidence(gen.getConfidence())
                .valid(true)
                .referencedTables(gen.getReferencedTables())
                .alternatives(gen.getAlternatives())
                .warnings(qr.getWarnings())
                .executed(true)
                .build();
    }
}
