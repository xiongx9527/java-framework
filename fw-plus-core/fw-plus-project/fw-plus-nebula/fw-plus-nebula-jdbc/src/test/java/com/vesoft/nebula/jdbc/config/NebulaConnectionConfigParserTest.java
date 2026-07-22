package com.vesoft.nebula.jdbc.config;

import com.vesoft.nebula.jdbc.client.NebulaHostAddress;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaConnectionConfigParserTest {

    @Test
    public void parseShouldMergeUrlQueryAndInputPropertiesIntoTypedConfig() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", "root");
        properties.setProperty("password", "nebula");
        properties.setProperty("minConnsSize", "1");

        NebulaConnectionConfig config = NebulaConnectionConfigParser.parse(
                "jdbc:nebula://127.0.0.1:9669/test_space?maxConnsSize=10", properties);

        assertEquals("test_space", config.getGraphspace());
        assertEquals("root", config.getUsername());
        assertEquals("nebula", config.getPassword());
        List<NebulaHostAddress> addresses = config.getGraphAddresses();
        assertEquals(1, addresses.size());
        assertEquals("127.0.0.1", addresses.get(0).getHost());
        assertEquals(9669, addresses.get(0).getPort());
        assertEquals(1, config.getMinConnsSize());
        assertEquals(10, config.getMaxConnsSize());
        assertEquals("root", config.getMergedProperties().getProperty("user"));
        assertEquals("nebula", config.getMergedProperties().getProperty("password"));
        assertEquals("1", config.getMergedProperties().getProperty("minConnsSize"));
        assertEquals("10", config.getMergedProperties().getProperty("maxConnsSize"));
    }

    @Test
    public void parseShouldSupportStringNumberAndTrimSpacesForPoolOptions() throws Exception {
        Properties properties = basicProperties();
        properties.setProperty("minConnsSize", " 2 ");
        properties.setProperty("maxConnsSize", " 11 ");
        properties.setProperty("timeout", " 1000 ");
        properties.setProperty("idleTime", " 2000 ");
        properties.setProperty("intervalIdle", " 3000 ");
        properties.setProperty("waitTime", " 4000 ");

        NebulaConnectionConfig config = NebulaConnectionConfigParser.parse(
                "jdbc:nebula://127.0.0.1:9669/test_space", properties);

        assertEquals(2, config.getMinConnsSize());
        assertEquals(11, config.getMaxConnsSize());
        assertEquals(1000, config.getTimeout());
        assertEquals(2000, config.getIdleTime());
        assertEquals(3000, config.getIntervalIdle());
        assertEquals(4000, config.getWaitTime());
    }

    @Test
    public void parseShouldSupportNumberInstancesForPoolOptions() throws Exception {
        Properties properties = basicProperties();
        properties.put("minConnsSize", 3);
        properties.put("maxConnsSize", 12L);
        properties.put("timeout", Short.valueOf((short) 5));

        NebulaConnectionConfig config = NebulaConnectionConfigParser.parse(
                "jdbc:nebula://127.0.0.1:9669/test_space", properties);

        assertEquals(3, config.getMinConnsSize());
        assertEquals(12, config.getMaxConnsSize());
        assertEquals(5, config.getTimeout());
    }

    @Test
    public void parseShouldKeepNonStringPoolOptionsWhenUrlContainsQuery() throws Exception {
        Properties properties = basicProperties();
        properties.put("minConnsSize", Integer.valueOf(2));
        properties.put("timeout", Long.valueOf(1000L));

        NebulaConnectionConfig config = NebulaConnectionConfigParser.parse(
                "jdbc:nebula://127.0.0.1:9669/test_space?user=url_user&maxConnsSize=12", properties);

        assertEquals("url_user", config.getUsername());
        assertEquals(2, config.getMinConnsSize());
        assertEquals(12, config.getMaxConnsSize());
        assertEquals(1000, config.getTimeout());
        assertEquals(Integer.valueOf(2), config.getMergedProperties().get("minConnsSize"));
        assertEquals(Long.valueOf(1000L), config.getMergedProperties().get("timeout"));
    }

    @Test
    public void parseShouldMergeStringPropertiesFromDefaultsChainWhenUrlContainsQuery() throws Exception {
        Properties parentDefaults = new Properties();
        parentDefaults.setProperty("minConnsSize", "2");
        Properties defaults = new Properties(parentDefaults);
        defaults.setProperty("timeout", "1000");
        Properties properties = new Properties(defaults);
        properties.setProperty("user", "root");
        properties.setProperty("password", "nebula");

        NebulaConnectionConfig config = NebulaConnectionConfigParser.parse(
                "jdbc:nebula://127.0.0.1:9669/test_space?user=url_user", properties);

        assertEquals("url_user", config.getUsername());
        assertEquals(2, config.getMinConnsSize());
        assertEquals(1000, config.getTimeout());
        assertEquals("2", config.getMergedProperties().getProperty("minConnsSize"));
        assertEquals("1000", config.getMergedProperties().getProperty("timeout"));
    }

    @Test
    public void parserSourceShouldNotReflectIntoPropertiesDefaults() throws Exception {
        File sourceFile = new File("src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParser.java");
        String source = new String(Files.readAllBytes(sourceFile.toPath()), StandardCharsets.UTF_8);

        assertFalse(source.contains("getDeclaredField(\"defaults\")"));
        assertFalse(source.contains("setAccessible"));
    }

    @Test
    public void parseShouldUseDefaultsWhenPoolOptionsMissing() throws Exception {
        NebulaConnectionConfig config = NebulaConnectionConfigParser.parse(
                "jdbc:nebula://127.0.0.1:9669/test_space", basicProperties());

        assertEquals(0, config.getMinConnsSize());
        assertEquals(10, config.getMaxConnsSize());
        assertEquals(0, config.getTimeout());
        assertEquals(0, config.getIdleTime());
        assertEquals(-1, config.getIntervalIdle());
        assertEquals(0, config.getWaitTime());
    }

    @Test
    public void parseShouldUseMergedPropertiesAsSingleSourceAndReturnDefensiveCopy() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", "property_user");
        properties.setProperty("password", "property_password");
        properties.setProperty("maxConnsSize", "2");

        NebulaConnectionConfig config = NebulaConnectionConfigParser.parse(
                "jdbc:nebula://127.0.0.1:9669/url_space?user=url_user&password=url_password&maxConnsSize=15", properties);

        assertEquals("url_space", config.getGraphspace());
        assertEquals("url_user", config.getUsername());
        assertEquals("url_password", config.getPassword());
        assertEquals(15, config.getMaxConnsSize());
        Properties mergedProperties = config.getMergedProperties();
        assertEquals("url_space", mergedProperties.getProperty("graphspace"));
        assertEquals("url_user", mergedProperties.getProperty("user"));
        assertEquals("url_password", mergedProperties.getProperty("password"));
        assertEquals("15", mergedProperties.getProperty("maxConnsSize"));

        mergedProperties.setProperty("graphspace", "mutated");

        assertFalse("mutated".equals(config.getMergedProperties().getProperty("graphspace")));
        assertEquals("url_space", config.getGraphspace());
    }

    @Test
    public void parseShouldRejectBlankAndNonNumericPoolOptionsWithFieldName() throws Exception {
        Properties blankProperties = basicProperties();
        blankProperties.setProperty("minConnsSize", "   ");

        assertInvalidPoolOption(blankProperties, "minConnsSize");

        Properties textProperties = basicProperties();
        textProperties.setProperty("maxConnsSize", "ten");

        assertInvalidPoolOption(textProperties, "maxConnsSize");
    }

    private static Properties basicProperties() {
        Properties properties = new Properties();
        properties.setProperty("user", "root");
        properties.setProperty("password", "nebula");
        return properties;
    }

    private static void assertInvalidPoolOption(Properties properties, String fieldName) throws Exception {
        Object invalidValue = properties.get(fieldName);
        try {
            NebulaConnectionConfigParser.parse("jdbc:nebula://127.0.0.1:9669/test_space", properties);
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains(fieldName));
            assertTrue(exception.getMessage().contains(String.valueOf(invalidValue)));
        }
    }
}
