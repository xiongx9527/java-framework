package com.vesoft.nebula.jdbc.statement;

import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaStatementClassifierTest {

    @Test
    public void checkQueryShouldAcceptMixedCaseAndWhitespaceQueryNgql() throws Exception {
        NebulaStatementClassifier.checkQuery("  MATCH\n(v)\tRETURN v  ");
        NebulaStatementClassifier.checkQuery("\n  sHoW   TAGS");
    }

    @Test
    public void checkUpdateShouldAcceptMixedCaseAndWhitespaceUpdateNgql() throws Exception {
        NebulaStatementClassifier.checkUpdate("\n  INSERT   VERTEX person(name) VALUES \"p1\":(\"tom\")");
        NebulaStatementClassifier.checkUpdate("create tag person(name string)");
    }

    @Test
    public void checkUpdateShouldAcceptQueryKeywordAfterLeadingUpdateKeyword() throws Exception {
        try {
            NebulaStatementClassifier.checkUpdate("CREATE TAG person(desc string, show string, go string)");
        } catch (SQLException exception) {
            fail("Leading update keyword should classify the statement as update: " + exception.getMessage());
        }
    }

    @Test
    public void checkQueryShouldRejectUpdateKeyword() throws Exception {
        assertRejectedQuery("create tag person(name string)", "create");
        assertRejectedQuery("insert vertex person(name) values \"p1\":(\"tom\")", "insert");
    }

    @Test
    public void checkUpdateShouldRejectQueryAndUnknownStatements() throws Exception {
        assertRejectedUpdate("  MATCH (v) RETURN v", "match");
        assertRejectedUpdate("select * from person", "update");
    }

    @Test
    public void classifierShouldRejectNullBlankAndUnknownNgql() throws Exception {
        assertRejectedQuery(null, "nGql");
        assertRejectedQuery("   ", "nGql");
        assertRejectedQuery("select * from person", "query");
        assertRejectedUpdate(null, "nGql");
        assertRejectedUpdate("   ", "nGql");
    }

    private static void assertRejectedQuery(String ngql, String expectedMessagePart) throws Exception {
        try {
            NebulaStatementClassifier.checkQuery(ngql);
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().toLowerCase().contains(expectedMessagePart.toLowerCase()));
        }
    }

    private static void assertRejectedUpdate(String ngql, String expectedMessagePart) throws Exception {
        try {
            NebulaStatementClassifier.checkUpdate(ngql);
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().toLowerCase().contains(expectedMessagePart.toLowerCase()));
        }
    }
}
