package com.xiongx9527.fw.plus.ai.base.skill;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SkillParserTest {

    @Test
    public void parseInlineListAndScalars() {
        String md = "---\n"
                + "id: monthly-report\n"
                + "version: 1.2.0\n"
                + "description: 经营分析报告\n"
                + "tools: [exec_sql, chart_render]\n"
                + "permissions: [datasource:read, export:write]\n"
                + "requiresConfirm: false\n"
                + "---\n"
                + "按月份生成报告。";
        SkillDescriptor d = SkillParser.parse(md);
        assertEquals("monthly-report", d.getId());
        assertEquals("1.2.0", d.getVersion());
        assertEquals("经营分析报告", d.getDescription());
        assertEquals("按月份生成报告。", d.getInstructions());
        assertTrue(d.allowsTool("exec_sql"));
        assertTrue(d.hasPermission("export:write"));
    }

    @Test
    public void parseBlockList() {
        String md = "---\n"
                + "id: b\n"
                + "tools:\n"
                + "  - a\n"
                + "  - c\n"
                + "---\n"
                + "body";
        SkillDescriptor d = SkillParser.parse(md);
        assertTrue(d.allowsTool("a"));
        assertTrue(d.allowsTool("c"));
        assertEquals("body", d.getInstructions());
    }
}
