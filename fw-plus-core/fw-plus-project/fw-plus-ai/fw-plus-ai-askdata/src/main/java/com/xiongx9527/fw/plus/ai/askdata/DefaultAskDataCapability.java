package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.ai.askdata.answer.AnswerCapability;
import com.xiongx9527.fw.plus.ai.askdata.answer.AnswerInput;
import com.xiongx9527.fw.plus.ai.askdata.answer.AnswerResult;
import com.xiongx9527.fw.plus.ai.askdata.answer.DefaultAnswerCapability;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenInput;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenResult;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationCapability;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import com.xiongx9527.fw.plus.ai.askdata.chart.ChartGenInput;
import com.xiongx9527.fw.plus.ai.askdata.chart.ChartGenResult;
import com.xiongx9527.fw.plus.ai.askdata.chart.ChartGenerationCapability;
import com.xiongx9527.fw.plus.ai.askdata.chart.ChartType;
import com.xiongx9527.fw.plus.ai.askdata.clarify.ClarificationCapability;
import com.xiongx9527.fw.plus.ai.askdata.clarify.ClarificationInput;
import com.xiongx9527.fw.plus.ai.askdata.clarify.ClarificationResult;
import com.xiongx9527.fw.plus.ai.askdata.clarify.DefaultClarificationCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaCandidateProvider;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallInput;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.base.memory.ChatMemory;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.jdbc.execution.QueryResult;
import com.xiongx9527.fw.plus.jdbc.execution.SqlExecInput;
import com.xiongx9527.fw.plus.jdbc.execution.SqlExecutionCapability;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Default AskDataCapability: resolve authorized schema candidates, recall schema, generate SQL with the NL2SQL capability,
 * and execute it with the execution capability. Returns the executed result plus the SQL and
 * confidence so callers can present or re-ask.
 *
 * <p>新增（§6.38 产品化能力，均为可选、向后兼容）：
 * <ul>
 *   <li>图表联动：提供 {@link ChartGenerationCapability} 时，对非空结果自动推荐图表；</li>
 *   <li>答案合成：提供 {@link LlmPort} 时，用 {@link DefaultAnswerCapability} 把结果转为自然语言回答；</li>
 *   <li>低置信反问：生成无效时（若提供 LlmPort）用 {@link DefaultClarificationCapability} 生成澄清问题，
 *       避免把错误 SQL 直接执行。</li>
 * </ul>
 */
public class DefaultAskDataCapability implements AskDataCapability {

    private final SchemaCandidateProvider candidateProvider;
    private final SchemaRecallCapability recall;
    private final SqlGenerationCapability generation;
    private final SqlExecutionCapability execution;
    private final AnswerCapability answerCapability;
    private final ClarificationCapability clarificationCapability;
    private final ChartGenerationCapability chart;
    private final ChatMemory memory;

    public DefaultAskDataCapability(SchemaCandidateProvider candidateProvider, SchemaRecallCapability recall,
                                   SqlGenerationCapability generation, SqlExecutionCapability execution) {
        this(candidateProvider, recall, generation, execution, null, null, null);
    }

    public DefaultAskDataCapability(SchemaCandidateProvider candidateProvider, SchemaRecallCapability recall,
                                   SqlGenerationCapability generation, SqlExecutionCapability execution,
                                   LlmPort llm) {
        this(candidateProvider, recall, generation, execution, llm, null, null);
    }

    public DefaultAskDataCapability(SchemaCandidateProvider candidateProvider, SchemaRecallCapability recall,
                                   SqlGenerationCapability generation, SqlExecutionCapability execution,
                                   LlmPort llm, ChartGenerationCapability chart) {
        this(candidateProvider, recall, generation, execution, llm, chart, null);
    }

    public DefaultAskDataCapability(SchemaCandidateProvider candidateProvider, SchemaRecallCapability recall,
                                   SqlGenerationCapability generation, SqlExecutionCapability execution,
                                   LlmPort llm, ChartGenerationCapability chart, ChatMemory memory) {
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
        this.answerCapability = llm == null ? null : new DefaultAnswerCapability(llm);
        this.clarificationCapability = llm == null ? null : new DefaultClarificationCapability(llm);
        this.chart = chart;
        this.memory = memory;
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
                .extraParams(input.getExtraParams())
                .history(resolveHistory(input));
        SqlGenResult gen = generation.generate(gb.build());
        if (!gen.isValid()) {
            // 生成无效：不执行。若具备澄清能力则给出澄清问题（§6.32）。
            String clarification = null;
            if (clarificationCapability != null) {
                ClarificationResult cr = clarificationCapability.clarify(ClarificationInput.builder()
                        .question(input.getQuestion())
                        .issues(Collections.singletonList(gen.getExplanation()))
                        .candidateTables(tableNames(candidates))
                        .build());
                if (cr.isValid()) {
                    clarification = cr.getText();
                }
            }
            return AskDataResult.builder()
                    .sql(gen.getSql())
                    .explanation(gen.getExplanation())
                    .confidence(gen.getConfidence())
                    .valid(false)
                    .referencedTables(gen.getReferencedTables())
                    .alternatives(gen.getAlternatives())
                    .executed(false)
                    .clarification(clarification)
                    .build();
        }
        QueryResult qr = execution.execute(SqlExecInput.builder()
                .sql(gen.getSql())
                .dialectHint(input.getDialectHint())
                .maxRows(input.getMaxRows())
                .queryTimeoutSeconds(input.getQueryTimeoutSeconds())
                .readOnly(input.isReadOnly()).build());
        ChartType chartType = null;
        Map<String, Object> chartConfig = null;
        if (chart != null && qr != null && qr.rowCount() > 0) {
            ChartGenResult cgr = chart.generate(ChartGenInput.builder()
                    .question(input.getQuestion())
                    .data(ChartDataset.from(qr))
                    .build());
            chartType = cgr.getChartType();
            chartConfig = cgr.getConfig();
        }
        String answer = null;
        if (answerCapability != null && qr != null) {
            AnswerResult ar = answerCapability.answer(AnswerInput.builder()
                    .question(input.getQuestion())
                    .sql(gen.getSql())
                    .queryResult(qr)
                    .dialectHint(input.getDialectHint())
                    .build());
            if (ar.isValid()) {
                answer = ar.getText();
            }
        }
        if (memory != null) {
            memory.add(ChatMessage.user(input.getQuestion()));
            memory.add(ChatMessage.assistant(answer != null ? answer : gen.getSql()));
        }
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
                .answer(answer)
                .chartType(chartType)
                .chartConfig(chartConfig)
                .build();
    }

    private List<ChatMessage> resolveHistory(AskDataInput input) {
        if (memory != null) {
            List<ChatMessage> fromMemory = memory.messages();
            return fromMemory == null ? Collections.<ChatMessage>emptyList() : fromMemory;
        }
        return input.getHistory() == null ? Collections.<ChatMessage>emptyList() : input.getHistory();
    }

    private List<String> tableNames(List<TableSchema> candidates) {
        List<String> names = new ArrayList<String>();
        for (TableSchema t : candidates) {
            if (t != null && t.getName() != null) {
                names.add(t.getName());
            }
        }
        return names;
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
