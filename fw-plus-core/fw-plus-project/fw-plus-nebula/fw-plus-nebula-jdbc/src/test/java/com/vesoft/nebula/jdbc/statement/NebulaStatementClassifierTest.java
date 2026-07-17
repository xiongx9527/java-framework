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
        NebulaStatementClassifier.checkQuery(" SELECT COUNT(*) FROM player ");
        NebulaStatementClassifier.checkQuery("YIELD 1");
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
    public void checkQueryShouldRejectLeadingUpdateKeyword() throws Exception {
        assertRejectedQuery("create tag person(name string)", "create");
        assertRejectedQuery("insert vertex person(name) values \"p1\":(\"tom\")", "insert");
        assertRejectedQuery("UPDATE VERTEX \"p1\" SET person.name = \"tom\"", "update");
    }

    @Test
    public void checkQueryShouldRejectUnknownLeadingKeywordEvenWhenQueryKeywordAppearsLater() throws Exception {
        assertRejectedQuery("foo MATCH (v) RETURN v", "query");
    }

    @Test
    public void checkUpdateShouldRejectQueryAndUnknownStatements() throws Exception {
        assertRejectedUpdate("  MATCH (v) RETURN v", "update");
        assertRejectedUpdate("select * from person", "update");
    }

    @Test
    public void classifierShouldRejectNullBlankAndUnknownNgql() throws Exception {
        assertRejectedQuery(null, "nGql");
        assertRejectedQuery("   ", "nGql");
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
