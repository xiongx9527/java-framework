package com.xiongx9527.fw.plus.ai.askdata.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Convert a tool-argument Map (decoded JSON) into a SchemaContext without a JSON library,
 * keeping the askdata module neutral. Accepts the nested structure produced by the
 * askdata.generate_sql / askdata.ask_data tool schemas.
 */
public final class SchemaContexts {

    private SchemaContexts() {
    }

    @SuppressWarnings("unchecked")
    public static SchemaContext fromMap(Object obj) {
        if (!(obj instanceof Map)) {
            return SchemaContext.builder().build();
        }
        Map<String, Object> map = (Map<String, Object>) obj;
        List<TableSchema> tables = new ArrayList<TableSchema>();
        Object tbls = map.get("tables");
        if (tbls instanceof List) {
            for (Object t : (List<Object>) tbls) {
                TableSchema ts = tableFromMap(t);
                if (ts != null) {
                    tables.add(ts);
                }
            }
        }
        return SchemaContext.builder().tables(tables).build();
    }

    @SuppressWarnings("unchecked")
    private static TableSchema tableFromMap(Object obj) {
        if (!(obj instanceof Map)) {
            return null;
        }
        Map<String, Object> m = (Map<String, Object>) obj;
        String name = str(m.get("name"));
        if (name == null) {
            return null;
        }
        List<ColumnSchema> cols = new ArrayList<ColumnSchema>();
        Object cs = m.get("columns");
        if (cs instanceof List) {
            for (Object c : (List<Object>) cs) {
                ColumnSchema cc = columnFromMap(c);
                if (cc != null) {
                    cols.add(cc);
                }
            }
        }
        List<String> pk = new ArrayList<String>();
        Object pko = m.get("primaryKey");
        if (pko instanceof List) {
            for (Object p : (List<Object>) pko) {
                String s = str(p);
                if (s != null) {
                    pk.add(s);
                }
            }
        } else if (pko instanceof String && !((String) pko).isEmpty()) {
            pk.add((String) pko);
        }
        List<RelationSchema> rels = new ArrayList<RelationSchema>();
        Object rs = m.get("relations");
        if (rs instanceof List) {
            for (Object r : (List<Object>) rs) {
                RelationSchema rr = relationFromMap(r);
                if (rr != null) {
                    rels.add(rr);
                }
            }
        }
        return TableSchema.builder()
                .name(name)
                .comment(str(m.get("comment")))
                .columns(cols)
                .primaryKey(pk)
                .relations(rels)
                .build();
    }

    private static ColumnSchema columnFromMap(Object obj) {
        if (!(obj instanceof Map)) {
            return null;
        }
        Map<String, Object> m = (Map<String, Object>) obj;
        String name = str(m.get("name"));
        if (name == null) {
            return null;
        }
        boolean pk = Boolean.parseBoolean(String.valueOf(m.get("primaryKey")));
        Object nv = m.get("nullable");
        boolean nullable = nv == null ? true : Boolean.parseBoolean(String.valueOf(nv));
        List<String> samples = new ArrayList<String>();
        Object sv = m.get("sampleValues");
        if (sv instanceof List) {
            for (Object s : (List<Object>) sv) {
                String x = str(s);
                if (x != null) {
                    samples.add(x);
                }
            }
        }
        return ColumnSchema.builder()
                .name(name)
                .type(str(m.get("type")))
                .comment(str(m.get("comment")))
                .primaryKey(pk)
                .nullable(nullable)
                .sampleValues(samples)
                .build();
    }

    private static RelationSchema relationFromMap(Object obj) {
        if (!(obj instanceof Map)) {
            return null;
        }
        Map<String, Object> m = (Map<String, Object>) obj;
        String ft = str(m.get("fromTable"));
        String fc = str(m.get("fromColumn"));
        String tt = str(m.get("toTable"));
        String tc = str(m.get("toColumn"));
        if (ft == null || fc == null || tt == null || tc == null) {
            return null;
        }
        return RelationSchema.builder()
                .fromTable(ft)
                .fromColumn(fc)
                .toTable(tt)
                .toColumn(tc)
                .build();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
