package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 校验（L1 护栏）：禁止写入型 DML、表名必须在可见 schema 内、括号平衡。
 * 不在生成里再判权限（权限由上游注入可见 schema 体现，见 §6.38.7）。
 */
public class SqlValidator {

    private static final Pattern WRITE_DML = Pattern.compile("(?i)\\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|REPLACE|GRANT|REVOKE|MERGE)\\b");
    private static final Pattern TABLE_REF = Pattern.compile("(?i)\\b(?:FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_.$#]*)");

    public ValidationResult validate(String sql, SchemaContext schema) {
        List<String> issues = new ArrayList<>();
        List<String> referenced = new ArrayList<>();
        if (sql == null || sql.trim().isEmpty()) {
            return ValidationResult.fail(Collections.singletonList(I18nMessageResolver.resolveKey("askdata.validate.empty_sql")), referenced);
        }
        String upper = sql.toUpperCase();
        Matcher dml = WRITE_DML.matcher(upper);
        while (dml.find()) {
            issues.add(I18nMessageResolver.resolveKey("askdata.validate.write_dml", dml.group(1)));
        }
        Matcher tbl = TABLE_REF.matcher(sql);
        while (tbl.find()) {
            String name = stripQuotes(tbl.group(1).trim());
            if (!referenced.contains(name)) {
                referenced.add(name);
            }
            if (schema != null && !schema.containsTable(name)) {
                issues.add(I18nMessageResolver.resolveKey("askdata.validate.table_not_in_schema", name));
            }
        }
        int open = count(sql, '(');
        int close = count(sql, ')');
        if (open != close) {
            issues.add(I18nMessageResolver.resolveKey("askdata.validate.unbalanced_paren", open, close));
        }
        return issues.isEmpty() ? ValidationResult.ok(referenced) : ValidationResult.fail(issues, referenced);
    }

    /** 仅判断 SQL 是否包含写入型 DML（供执行期只读守卫复用，§6.38）。 */
    public boolean containsWriteDml(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        return WRITE_DML.matcher(sql.toUpperCase()).find();
    }

    private static String stripQuotes(String s) {
        return s.replace(String.valueOf('`'), "").replace(String.valueOf('"'), "").replace("[", "").replace("]", "").trim();
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }
}
