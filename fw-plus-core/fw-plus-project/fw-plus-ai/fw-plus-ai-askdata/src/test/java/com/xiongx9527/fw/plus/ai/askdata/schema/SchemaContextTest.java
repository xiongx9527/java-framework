package com.xiongx9527.fw.plus.ai.askdata.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SchemaContextTest {

    @Test
    void returnsFirstPkColumn() {
        TableSchema t = TableSchema.builder().name("orders").primaryKey(Arrays.asList("id")).build();
        SchemaContext sc = SchemaContext.builder().tables(Arrays.asList(t)).build();
        assertEquals("id", sc.primaryKeyColumn("orders"));
        assertEquals("id", sc.primaryKeyColumn("ORDERS"));
        assertEquals("id", sc.primaryKeyColumn("db.orders"));
    }

    @Test
    void nullWhenNoPkOrMissingTable() {
        TableSchema t = TableSchema.builder().name("orders").build();
        SchemaContext sc = SchemaContext.builder().tables(Arrays.asList(t)).build();
        assertNull(sc.primaryKeyColumn("orders"));
        assertNull(sc.primaryKeyColumn("missing"));
    }
}
