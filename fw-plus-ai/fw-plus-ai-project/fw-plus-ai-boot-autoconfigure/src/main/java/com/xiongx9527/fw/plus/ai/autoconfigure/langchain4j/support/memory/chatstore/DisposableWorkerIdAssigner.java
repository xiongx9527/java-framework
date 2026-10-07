package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import com.xiongx9527.fw.plus.common.uid.worker.WorkerIdAssigner;
import com.xiongx9527.fw.plus.common.uid.worker.WorkerNodeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.net.InetAddress;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.function.Supplier;

/**
 * 基于 {@code WORKER_NODE} 表分配 workerId 的分配器（对应百度 uidgenerator 的
 * DisposableWorkerIdAssigner 语义）：启动时插入一行节点记录取自增 ID，停用（{@link #dispose()}
 * ）时更新该记录的 MODIFIED 时间。
 *
 * <p>JdbcTemplate 通过 {@link Supplier} 每次调用时动态解析——即 workerId 随所选数据源分配
 * （表结构见仓库 {@code docs/sql/fw-plus-ai-chat-memory.sql} 的 WORKER_NODE 建表脚本）。
 * workerId 取节点记录 ID 对 1024 取模（与默认 workerBits=22 的位分配兼容）。</p>
 */
public class DisposableWorkerIdAssigner implements WorkerIdAssigner {

    private static final int WORKER_ID_MODULO = 1024;

    private final Supplier<JdbcTemplate> jdbcTemplateSupplier;

    private long workerNodeId = -1;

    public DisposableWorkerIdAssigner(Supplier<JdbcTemplate> jdbcTemplateSupplier) {
        if (jdbcTemplateSupplier == null) {
            throw new IllegalArgumentException("DisposableWorkerIdAssigner: jdbcTemplateSupplier 不能为空");
        }
        this.jdbcTemplateSupplier = jdbcTemplateSupplier;
    }

    @Override
    public long assignWorkerId() {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        String hostName = resolveHostName();
        String port = System.getProperty("server.port", "0");
        Date launchDate = new Date(System.currentTimeMillis());
        Timestamp now = new Timestamp(System.currentTimeMillis());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO WORKER_NODE (HOST_NAME, PORT, TYPE, LAUNCH_DATE, MODIFIED, CREATED)"
                            + " VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, hostName);
            ps.setString(2, port);
            ps.setInt(3, WorkerNodeType.ACTUAL.value());
            ps.setDate(4, launchDate);
            ps.setTimestamp(5, now);
            ps.setTimestamp(6, now);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("DisposableWorkerIdAssigner: WORKER_NODE 插入未返回自增 ID");
        }
        workerNodeId = key.longValue();
        return workerNodeId % WORKER_ID_MODULO;
    }

    /** 节点停用：更新 WORKER_NODE 记录的 MODIFIED 时间，便于运维清理长期未更新的节点。 */
    public void dispose() {
        if (workerNodeId < 0) {
            return;
        }
        jdbcTemplate().update("UPDATE WORKER_NODE SET MODIFIED = ? WHERE ID = ?",
                new Timestamp(System.currentTimeMillis()), workerNodeId);
    }

    private JdbcTemplate jdbcTemplate() {
        JdbcTemplate jdbcTemplate = jdbcTemplateSupplier.get();
        if (jdbcTemplate == null) {
            throw new IllegalStateException("DisposableWorkerIdAssigner: 无法解析 JdbcTemplate 客户端");
        }
        return jdbcTemplate;
    }

    private static String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
