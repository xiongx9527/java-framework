package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.web;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoIterable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.ssssssss.magicapi.utils.JdbcUtils;

import java.net.URLEncoder;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Slf4j
public class MagicDebeziumController extends MagicController implements MagicExceptionHandler , BaseService  {

    /**
     * 构造函数，用于创建MagicDebeziumController实例
     *
     * @param configuration 魔法配置对象，包含Debezium相关的配置信息
     */
    public MagicDebeziumController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 根据不同数据库类型获取表名列表
     *
     * @param connection 数据库连接
     * @param connectorClass 连接器类型
     * @param database 数据库名
     * @return 表名列表
     */
    private List<String> fetchSqlTableNames(Connection connection, String connectorClass, String database) {
        List<String> tableList = new ArrayList<>();
        try {
            if (connectorClass.contains("mysql")) {
                // MySQL 查询表名
                String sql = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, database);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        tableList.add(rs.getString("TABLE_NAME"));
                    }
                }
            } else if (connectorClass.contains("postgresql")) {
                // PostgreSQL 查询表名
                String sql = "SELECT tablename FROM pg_tables WHERE schemaname = 'public'";
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery(sql);
                    while (rs.next()) {
                        tableList.add(rs.getString("tablename"));
                    }
                }
            } else if (connectorClass.contains("sqlserver")) {
                // SQL Server 查询表名
                String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'";
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery(sql);
                    while (rs.next()) {
                        tableList.add(rs.getString("TABLE_NAME"));
                    }
                }
            } else if (connectorClass.contains("oracle")) {
                // Oracle 查询表名
                String sql = "SELECT TABLE_NAME FROM USER_TABLES";
                try (Statement stmt = connection.createStatement()) {
                    ResultSet rs = stmt.executeQuery(sql);
                    while (rs.next()) {
                        tableList.add(rs.getString("TABLE_NAME"));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("查询表名失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return tableList;
    }


    /**
     * 获取需要处理的数据库表列表
     * 
     * 该方法从配置中读取包含表和排除表的配置，将它们合并到一个列表中返回。
     * 注意：返回的列表同时包含includeTables和excludeTables中的表名。
     *
     * @param properties Debezium数据源配置信息，包含数据源的键以及包含表和排除表的配置
     * @return 表名列表，包含配置的包含表和排除表中的所有表名
     */
    private List<String> getTables(DebeziumDataSourceInfo properties) {
        // 从配置中获取包含表和排除表的字符串配置
        String includeTablesStr = MagicAPIConfigUtils.getDebeziumConfig(properties.getKey(), "includeTables", properties.getIncludeTables());
        String excludeTablesStr = MagicAPIConfigUtils.getDebeziumConfig(properties.getKey(), "excludeTables", properties.getExcludeTables());

        List<String> tableList = new ArrayList<>();
        
        // 解析包含表配置，将逗号分隔的表名添加到列表中
        if (StringUtils.isNotEmpty(includeTablesStr)) {
            String[] includeTables = StringUtils.split(includeTablesStr,",");
            for (String table : includeTables) {
                tableList.add(table);
            }
        }
        
        // 解析排除表配置，将逗号分隔的表名添加到列表中
        if (StringUtils.isNotEmpty(excludeTablesStr)) {
            String[] excludeTables = StringUtils.split(excludeTablesStr,",");
            for (String table : excludeTables) {
                tableList.add(table);
            }
        }
        
        return tableList;
    }

    /**
     * 测试数据库连接是否正常。
     * 根据传入的 Debezium 数据源配置信息，判断数据库类型并尝试建立连接以验证其可用性。
     *
     * @param properties 包含数据库连接所需信息的 Debezium 数据源配置对象，
     *                   包括 hostname、port、database、username、password 和 connectorClass 等属性。
     */
    private void testDb(DebeziumDataSourceInfo properties) {
        String connectorClass = MagicAPIConfigUtils.getDebeziumConfig(properties.getKey(), "connectorClass", properties.getConnectorClass());
        String driver = "";
        String url = "";
        // 数据库连接配置
        String hostname = MagicAPIConfigUtils.getDebeziumConfig(properties.getKey(), "hostname", properties.getHostname());
        String port = MagicAPIConfigUtils.getDebeziumConfig(properties.getKey(), "port", properties.getPort());
        String username = MagicAPIConfigUtils.getDebeziumConfig(properties.getKey(), "username", properties.getUsername());
        String password = MagicAPIConfigUtils.getDebeziumConfig(properties.getKey(), "password", properties.getPassword());
        String database =  MagicAPIConfigUtils.getDebeziumConfig(properties.getKey(), "database", properties.getDatabase());

        // 根据不同的数据库类型设置对应的 JDBC 驱动和连接 URL
        if (connectorClass.contains("mysql")) {
            driver = "com.mysql.cj.jdbc.Driver";
            url = "jdbc:mysql://" + hostname + ":" + port + "/" + database;
        } else if (connectorClass.contains("postgresql")) {
            driver = "org.postgresql.Driver";
            url = "jdbc:postgresql://" + hostname + ":" + port + "/" + database;
        } else if (connectorClass.contains("sqlserver")) {
            driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            url = "jdbc:sqlserver://" + hostname + ":" + port + ";databaseName=" + database;
        } else if (connectorClass.contains("oracle")) {
            driver = "oracle.jdbc.driver.OracleDriver";
            url = "jdbc:oracle:thin:@" + hostname + ":" + port + "/" + database;
        }

        // 如果是 MongoDB，则使用 Mongo Client 进行连接测试
        if (connectorClass.contains("mongodb")) {
            MongoClient mongoClient = null;
            try {
                // 构建 MongoDB 连接字符串，对密码进行 URL 编码防止特殊字符问题
                String mongoUri = "mongodb://" + username + ":" + URLEncoder.encode(password, "UTF-8") +
                        "@" + hostname + ":" + port +
                        "/" + database +
                        "?connectTimeoutMS=5000&serverSelectionTimeoutMS=5000";
                mongoClient = com.mongodb.client.MongoClients.create(mongoUri);
                MongoIterable<String> collectionNames =  mongoClient.getDatabase(database).listCollectionNames();
                List<String> allTables = new ArrayList<>();
                for (String collectionName : collectionNames) {
                    allTables.add(collectionName);
                }
                List<String> tableList = getTables(properties);
                List<String> difference = new ArrayList<>(tableList);
                difference.removeAll(allTables);
                if (!difference.isEmpty()) {
                    throw new RuntimeException("未找到表：" + String.join(", ", difference));
                }
            } catch (Exception e) {
                log.error("MongoDB 连接失败: " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                if (mongoClient != null) {
                    try {
                        mongoClient.close();
                    } catch (Exception e) {
                    }
                }
            }
        } else {
            // 其他关系型数据库通过 JDBC 方式进行连接测试
            Connection connection = null;
            try {
                connection = JdbcUtils.getConnection(driver,url, username, password);
                List<String> allTables = fetchSqlTableNames(connection, connectorClass, database);
                List<String> tableList = getTables(properties);
                List<String> difference = new ArrayList<>(tableList);
                difference.removeAll(allTables);
                if (!difference.isEmpty()) {
                    throw new RuntimeException("未找到表：" + String.join(", ", difference));
                }
            } catch (Exception e) {
                log.error("数据库连接失败: " + e.getMessage());
                throw new RuntimeException(e);
            } finally {
                if (connection != null) {
                    JdbcUtils.close(connection);
                }
            }
        }
    }

    /**
     * 测试Kafka连接是否正常
     *
     * @param properties Debezium数据源配置信息，用于获取Kafka服务器地址
     */
    private void testKafka(DebeziumDataSourceInfo properties) {
        // 配置Kafka生产者属性
        Properties kafkaProps = new Properties();
        String kafkaServers = MagicAPIConfigUtils.getDebeziumConfig(properties.getKey(), "kafkaServers", properties.getKafkaServers());
        kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProps.put(ProducerConfig.METADATA_MAX_IDLE_CONFIG, 6000);
        kafkaProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);

        // 创建Kafka生产者并测试连接
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps)) {
            producer.partitionsFor("magic-api-test-topic");
            producer.flush();
        } catch (Exception e) {
            log.error("连接失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 测试Debezium数据源配置是否正确
     * 通过创建一个临时的Debezium引擎并尝试连接数据库来验证配置
     *
     * @param properties Debezium数据源配置信息
     * @return JsonBean 包含测试结果的对象，成功时返回"OK"，失败时返回错误信息
     */
    @RequestMapping("/debezium/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody DebeziumDataSourceInfo properties) {
        try {
            validateField(properties);
            testDb(properties);
            testKafka(properties);
            return new JsonBean<>("ok");
        } catch (Exception e) {
            log.error("测试数据源失败", e);
            String message = e.getMessage() != null ? e.getMessage() : "未知错误";
            return new JsonBean<>("数据源连接失败: " + message);
        }
    }
}
