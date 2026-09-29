package dev.langchain4j.store.graph.nebula;

import com.vesoft.nebula.client.graph.SessionPool;
import com.vesoft.nebula.client.graph.SessionPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * NebulaGraph 连接、nGQL 执行和返回值转换的公共工具。
 *
 * <p>本工程没有直接复用 LangChain4j 上游实现，LangChain4j 没有 nebula 模块，
 * 这里只沿用 {@code dev.langchain4j.*} 包名，与 {@code dev.langchain4j.store.graph.neo4j} 保持对称。
 */
public class NebulaUtils {

    private NebulaUtils() {
    }

    /**
     * 创建 {@link SessionPool}。
     *
     * @param addresses      graphd 地址，多个用逗号分隔，例如 {@code 127.0.0.1:9669,127.0.0.1:9670}
     * @param username       用户名
     * @param password       密码
     * @param spaceName      图空间名，NebulaGraph 的 schema 与数据都归属于某个图空间，不能为空
     * @param maxSessionSize 最大会话数，小于等于 0 时使用驱动默认值
     * @param timeout        超时时间（毫秒），小于 0 时使用驱动默认值
     */
    public static SessionPool createSessionPool(String addresses,
                                                String username,
                                                String password,
                                                String spaceName,
                                                int maxSessionSize,
                                                int timeout) {

        List<HostAddress> hostAddresses = parseAddresses(addresses);
        SessionPoolConfig config = new SessionPoolConfig(hostAddresses, spaceName, username, password);
        if (maxSessionSize > 0) {
            config.setMaxSessionSize(maxSessionSize);
        }
        if (timeout >= 0) {
            config.setTimeout(timeout);
        }
        // SessionPool 的构造会按 minSessionSize 建会话（默认 1 个），连不上或鉴权失败时直接抛异常
        try {
            return new SessionPool(config);
        } catch (RuntimeException e) {
            throw new NebulaException(String.format(
                    "It's not possible to init the NebulaGraph session pool for space `%s` with addresses `%s`",
                    spaceName, addresses), e);
        }
    }

    public static List<HostAddress> parseAddresses(String addresses) {

        List<HostAddress> hostAddresses = new ArrayList<>();
        if (addresses == null || addresses.trim().isEmpty()) {
            throw new NebulaException("NebulaGraph addresses cannot be empty, for example `127.0.0.1:9669`");
        }
        for (String address : addresses.split(",")) {
            String trimmed = address.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int separator = trimmed.lastIndexOf(':');
            if (separator <= 0 || separator == trimmed.length() - 1) {
                throw new NebulaException(String.format(
                        "The address `%s` is not a valid `host:port` NebulaGraph address", trimmed));
            }
            String host = trimmed.substring(0, separator);
            int port;
            try {
                port = Integer.parseInt(trimmed.substring(separator + 1));
            } catch (NumberFormatException e) {
                throw new NebulaException(String.format(
                        "The address `%s` is not a valid `host:port` NebulaGraph address", trimmed), e);
            }
            hostAddresses.add(new HostAddress(host, port));
        }
        if (hostAddresses.isEmpty()) {
            throw new NebulaException("NebulaGraph addresses cannot be empty, for example `127.0.0.1:9669`");
        }
        return hostAddresses;
    }

    /**
     * 执行 nGQL，失败时抛出 {@link NebulaException}。
     */
    public static ResultSet execute(SessionPool sessionPool, String nGql) {

        ResultSet resultSet;
        try {
            resultSet = sessionPool.execute(nGql);
        } catch (NebulaException e) {
            throw e;
        } catch (Exception e) {
            throw new NebulaException("Error executing query: " + nGql, e);
        }
        if (resultSet == null || !resultSet.isSucceeded()) {
            String errorMessage = resultSet == null ? "unknown error" : resultSet.getErrorMessage();
            throw new NebulaException(String.format("Error executing query: %s, error message: %s",
                    nGql, errorMessage));
        }
        return resultSet;
    }

    public static List<ResultSet.Record> rows(SessionPool sessionPool, String nGql) {

        return rows(execute(sessionPool, nGql));
    }

    public static List<ResultSet.Record> rows(ResultSet resultSet) {

        List<ResultSet.Record> records = new ArrayList<>();
        if (resultSet == null) {
            return records;
        }
        for (int i = 0; i < resultSet.rowsSize(); i++) {
            records.add(resultSet.rowValues(i));
        }
        return records;
    }

    /**
     * 把 nGQL 返回值转成可读文本，供 {@link dev.langchain4j.rag.content.Content} 使用。
     */
    public static String toText(ValueWrapper value) {

        if (value == null || value.isEmpty() || value.isNull()) {
            return "";
        }
        try {
            if (value.isString()) {
                return value.asString();
            }
            return value.toString();
        } catch (Exception e) {
            throw new NebulaException("Error converting the NebulaGraph value: " + value, e);
        }
    }

    /**
     * 转义并包裹成 nGQL 字符串字面量。
     */
    public static String quote(String value) {

        if (value == null) {
            return null;
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                + "\"";
    }

    /**
     * 校验标识符（tag、edge、属性名），防止 nGQL 注入。
     */
    public static String sanitizeOrThrows(String value, String config) {

        if (value != null && value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            return value;
        }
        throw new NebulaException(String.format("The value %s, to assign to configuration %s, cannot be safely quoted",
                value, config));
    }
}
