package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataDialectRegistryCapabilityTest {

    @Test
    public void shouldDescribeAllSqlBotAndWrenAiConnectorCapabilities() {
        AskDataDialectRegistry registry = new AskDataDialectRegistry();

        assertTrue(registry.getCapability("mysql").isDirectExecutionSupported());
        assertTrue(registry.getCapability("oracle").isDirectExecutionSupported());
        assertTrue(registry.getCapability("postgresql").isDirectExecutionSupported());
        assertTrue(registry.getCapability("sqlserver").isDirectExecutionSupported());
        assertTrue(registry.getCapability("clickhouse").isDirectExecutionSupported());
        assertTrue(registry.getCapability("dameng").isDirectExecutionSupported());
        assertTrue(registry.getCapability("doris").isDirectExecutionSupported());
        assertTrue(registry.getCapability("redshift").isDirectExecutionSupported());
        assertTrue(registry.getCapability("elasticsearch").isReadOnlyDescriptor());
        assertTrue(registry.getCapability("kingbase").isDirectExecutionSupported());
        assertTrue(registry.getCapability("starrocks").isDirectExecutionSupported());
        assertTrue(registry.getCapability("hive").isDirectExecutionSupported());
        assertTrue(registry.getCapability("excel-csv").isReadOnlyDescriptor());
        assertTrue(registry.getCapability("bigquery").isDirectExecutionSupported());
        assertTrue(registry.getCapability("duckdb").isDirectExecutionSupported());
        assertTrue(registry.getCapability("snowflake").isDirectExecutionSupported());
        assertFalse(registry.getCapability("elasticsearch").isDirectExecutionSupported());
    }
}
