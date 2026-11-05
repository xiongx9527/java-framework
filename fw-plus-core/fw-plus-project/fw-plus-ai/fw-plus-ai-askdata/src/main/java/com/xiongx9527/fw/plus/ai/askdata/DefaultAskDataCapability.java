package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenInput;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenResult;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationCapability;
import com.xiongx9527.fw.plus.jdbc.execution.QueryResult;
import com.xiongx9527.fw.plus.jdbc.execution.SqlExecInput;
import com.xiongx9527.fw.plus.jdbc.execution.SqlExecutionCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaCandidateProvider;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallInput;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Default AskDataCapability: resolve authorized schema candidates, recall schema, generate SQL with the NL2SQL capability,
 * and execute it with the execution capability. Returns the executed result plus the SQL and
 * confidence so callers can present or re-ask.
 */
public class DefaultAskDataCapability implements AskDataCapability {

    private final SchemaCandidateProvider candidateProvider;
    private final SchemaRecallCapability recall;
    private final SqlGenerationCapability generation;
    private final SqlExecutionCapability execution;

    public DefaultAskDataCapability(SchemaCandidateProvider candidateProvider, SchemaRecallCapability recall,
                                   SqlGenerationCapability generation, SqlExecutionCapability execution) {
        if (candidateProvider == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.schema_candidate_provider_required"));
        }
        if (generation == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.gen_capability_required"));
        }
        if (execution == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.exec_capability_required"));
        }
        this.candidateProvider = candidateProvider;
        this.recall = recall;
        this.generation = generation;
        this.execution = execution;
    }

    @Override
    public AskDataResult ask(AskDataInput input) {
        if (input == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.ask_data_input_required"));
        }
        List<TableSchema> providedCandidates = candidateProvider.findCandidates(
                RetrievalQuery.builder().text(input.getQuestion()).build());
        List<TableSchema> candidates = new ArrayList<TableSchema>();
        if (providedCandidates != null) {
            for (TableSchema candidate : providedCandidates) {
                if (candidate != null && candidate.getName() != null && !candidate.getName().trim().isEmpty()) {
                    candidates.add(candidate);
                }
            }
        }
        SchemaContext schema;
        if (recall != null) {
            SchemaContext recalled = recall.recall(SchemaRecallInput.builder()
                    .question(input.getQuestion())
                    .candidates(new ArrayList<TableSchema>(candidates))
                    .indexMode(input.getRecallIndexMode())
                    .build());
            schema = restrictToAuthorizedCandidates(candidates, recalled);
        } else {
            schema = SchemaContext.builder()
                    .tables(candidates)
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
        QueryResult qr = execution.execute(SqlExecInput.builder()
                .sql(gen.getSql())
                .dialectHint(input.getDialectHint())
                .maxRows(input.getMaxRows())
                .queryTimeoutSeconds(input.getQueryTimeoutSeconds())
                .readOnly(input.isReadOnly()).build());
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

    private SchemaContext restrictToAuthorizedCandidates(List<TableSchema> candidates, SchemaContext recalled) {
        if (recalled == null || recalled.isEmpty()) {
            return SchemaContext.builder().tables(Collections.<TableSchema>emptyList()).build();
        }
        Map<String, TableSchema> authorized = new LinkedHashMap<String, TableSchema>();
        for (TableSchema candidate : candidates) {
            if (candidate != null && candidate.getName() != null) {
                authorized.put(normalize(candidate.getName()), candidate);
            }
        }
        java.util.ArrayList<TableSchema> tables = new java.util.ArrayList<TableSchema>();
        for (TableSchema table : recalled.getTables()) {
            if (table == null || table.getName() == null) {
                continue;
            }
            TableSchema trusted = authorized.get(normalize(table.getName()));
            if (trusted != null && !tables.contains(trusted)) {
                tables.add(trusted);
            }
        }
        return SchemaContext.builder().tables(tables).build();
    }

    private String normalize(String tableName) {
        return tableName.trim().toLowerCase(Locale.ROOT).replace("`", "").replace("\"", "");
    }
}
