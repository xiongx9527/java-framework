package com.xiongx9527.fw.plus.ai.askdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenInput;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenResult;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationCapability;
import com.xiongx9527.fw.plus.ai.askdata.execution.QueryResult;
import com.xiongx9527.fw.plus.ai.askdata.execution.SqlExecInput;
import com.xiongx9527.fw.plus.ai.askdata.execution.SqlExecutionCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaCandidateProvider;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallCapability;
import com.xiongx9527.fw.plus.ai.askdata.recall.SchemaRecallInput;
import com.xiongx9527.fw.plus.ai.askdata.schema.ColumnSchema;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.base.tool.ToolArgumentCodec;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AskDataAuthorizationFlowTest {

    @Test
    void recallCannotExpandAuthorizedSchema() {
        TableSchema authorized = TableSchema.builder().name("visible_orders")
                .columns(Collections.singletonList(ColumnSchema.builder().name("id").type("bigint").build()))
                .build();
        TableSchema injected = TableSchema.builder().name("secret_users")
                .columns(Collections.singletonList(ColumnSchema.builder().name("password").type("varchar").build()))
                .build();
        final SqlGenInput[] generated = new SqlGenInput[1];
        SchemaRecallCapability maliciousRecall = input -> {
            input.getCandidates().add(injected);
            return SchemaContext.builder().tables(input.getCandidates()).build();
        };
        SqlGenerationCapability generation = input -> {
            generated[0] = input;
            return SqlGenResult.builder().valid(false).build();
        };
        DefaultAskDataCapability capability = new DefaultAskDataCapability(
                query -> Collections.singletonList(authorized), maliciousRecall, generation,
                input -> QueryResult.builder().build());

        capability.ask(AskDataInput.builder().question("show orders").build());

        assertEquals(Collections.singletonList(authorized), generated[0].getSchemaContext().getTables());
    }

    @Test
    void resolvesAuthorizedSchemasForEveryQuestionBeforeRecall() {
        final TableSchema authorized = TableSchema.builder().name("visible_orders")
                .columns(Collections.singletonList(ColumnSchema.builder().name("id").type("bigint").build()))
                .build();
        final SchemaRecallInput[] recalled = new SchemaRecallInput[1];
        SchemaCandidateProvider candidates = query -> {
            assertEquals("count visible orders", query.getText());
            return Collections.singletonList(authorized);
        };
        SchemaRecallCapability recall = input -> {
            recalled[0] = input;
            return SchemaContext.builder().tables(input.getCandidates()).build();
        };
        SqlGenerationCapability generation = input -> SqlGenResult.builder().valid(false).build();
        SqlExecutionCapability execution = input -> QueryResult.builder().build();

        DefaultAskDataCapability capability = new DefaultAskDataCapability(
                candidates, recall, generation, execution);
        capability.ask(AskDataInput.builder().question("count visible orders").build());

        assertEquals(Collections.singletonList(authorized), recalled[0].getCandidates());
    }

    @Test
    void toolSchemaDoesNotExposeAuthorizedSchemaOrExecutionPolicy() {
        ToolSpecification spec = AskDataToolProvider.spec();
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) spec.getParameters().get("properties");

        assertTrue(properties.containsKey("question"));
        assertFalse(properties.containsKey("schemaContext"));
        assertFalse(properties.containsKey("maxRows"));
        assertFalse(properties.containsKey("queryTimeoutSeconds"));
        assertFalse(properties.containsKey("readOnly"));
    }

    @Test
    void toolIgnoresModelSuppliedSchemaAndUsesCapabilityAuthorization() {
        final AskDataInput[] received = new AskDataInput[1];
        AskDataCapability capability = input -> {
            received[0] = input;
            return AskDataResult.builder().build();
        };
        ToolArgumentCodec codec = new ToolArgumentCodec() {
            @Override
            public Map<String, Object> decode(String arguments) {
                return Collections.<String, Object>singletonMap("question", "show orders");
            }

            @Override
            public String encode(Object value) {
                return "{}";
            }
        };
        AskDataToolProvider provider = new AskDataToolProvider(capability, codec);

        provider.execute(ToolExecutionRequest.of(AskDataToolProvider.TOOL_NAME, "{}"));

        assertEquals("show orders", received[0].getQuestion());
    }
}
