package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.DataSourceBuilder;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicJdbcClassOptionProviderTest {

    @Test
    void shouldFilterJdbcDriversByClasspath() {
        MagicJdbcClassOptionProperties properties = new MagicJdbcClassOptionProperties();
        properties.setDriverCandidates(Arrays.asList(
                TestDriver.class.getName(),
                "com.example.MissingDriver"));

        MagicJdbcClassOptionProvider provider = new MagicJdbcClassOptionProvider(
                properties,
                Collections.<String, DataSourceBuilder>emptyMap(),
                MagicJdbcClassOptionProviderTest.class.getClassLoader());

        MagicJdbcClassOptions options = provider.getOptions();

        assertTrue(options.getJdbcDrivers().contains(TestDriver.class.getName()));
        assertFalse(options.getJdbcDrivers().contains("com.example.MissingDriver"));
    }

    @Test
    void shouldUseGraphAndMongoJdbcDriversAsFilteredCandidateInputs() {
        MagicJdbcClassOptionProvider provider = new MagicJdbcClassOptionProvider(
                new MagicJdbcClassOptionProperties(),
                Collections.<String, DataSourceBuilder>emptyMap(),
                new MissingOnlyClassLoader());

        MagicJdbcClassOptions options = provider.getOptions();

        assertFalse(options.getJdbcDrivers().contains("com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaDriver"));
        assertFalse(options.getJdbcDrivers().contains("com.xiongx9527.fw.plus.graph.neo4j.jdbc.Neo4jDriver"));
        assertFalse(options.getJdbcDrivers().contains("com.mongodb.jdbc.MongoDriver"));
    }

    @Test
    void shouldFilterDatasourceTypesByClasspathAndBuilderSupport() {
        MagicJdbcClassOptionProperties properties = new MagicJdbcClassOptionProperties();
        properties.setDatasourceTypeCandidates(Arrays.asList(
                TestDataSource.class.getName(),
                DefaultConstructibleDataSource.class.getName(),
                UnsupportedDataSource.class.getName(),
                "com.example.MissingDataSource"));

        Map<String, DataSourceBuilder> builders = new HashMap<String, DataSourceBuilder>();
        builders.put(TestDataSource.class.getName(), null);

        MagicJdbcClassOptionProvider provider = new MagicJdbcClassOptionProvider(
                properties,
                builders,
                MagicJdbcClassOptionProviderTest.class.getClassLoader());

        MagicJdbcClassOptions options = provider.getOptions();

        assertTrue(options.getDatasourceTypes().contains(TestDataSource.class.getName()));
        assertTrue(options.getDatasourceTypes().contains(DefaultConstructibleDataSource.class.getName()));
        assertFalse(options.getDatasourceTypes().contains(UnsupportedDataSource.class.getName()));
        assertFalse(options.getDatasourceTypes().contains("com.example.MissingDataSource"));
    }

    private static class TestDriver implements Driver {
        @Override
        public Connection connect(String url, java.util.Properties info) throws SQLException {
            return null;
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return false;
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, java.util.Properties info) throws SQLException {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return Logger.getGlobal();
        }
    }

    private static class MissingOnlyClassLoader extends ClassLoader {
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("com.xiongx9527.fw.plus.graph.") || name.startsWith("com.mongodb.jdbc.")) {
                throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
        }
    }

    private static class UnsupportedDataSource extends TestDataSource {
        private UnsupportedDataSource(String ignored) {
        }
    }

    public static class DefaultConstructibleDataSource extends TestDataSource {
        public DefaultConstructibleDataSource() {
        }
    }

    private static class TestDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            return null;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return null;
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return Logger.getGlobal();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }
}
