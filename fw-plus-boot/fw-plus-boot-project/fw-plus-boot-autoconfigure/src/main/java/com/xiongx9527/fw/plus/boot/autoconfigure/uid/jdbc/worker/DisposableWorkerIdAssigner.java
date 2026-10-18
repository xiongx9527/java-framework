package com.xiongx9527.fw.plus.boot.autoconfigure.uid.jdbc.worker;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.xiongx9527.fw.plus.boot.autoconfigure.uid.jdbc.entity.WorkerNodeEntity;
import com.xiongx9527.fw.plus.common.uid.worker.WorkerIdAssigner;
import com.xiongx9527.fw.plus.common.uid.worker.WorkerNodeType;
import com.xiongx9527.fw.plus.common.util.DockerUtils;
import com.xiongx9527.fw.plus.common.util.NetUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Date;

/**
 * Represents an implementation of {@link WorkerIdAssigner}, 
 * the worker id will be discarded after assigned to the UidGenerator
 * 
 * @author yutianbao
 */
@Slf4j
public class DisposableWorkerIdAssigner implements WorkerIdAssigner {

    /**
     * 工作节点表名配置项
     * 从配置文件中读取表名，默认为"WORKER_NODE"
     */
    @Value("${fw.plus.uid.jdbc.worker_node_table_name:WORKER_NODE}")
    private String workerNodeTableName = "WORKER_NODE";

    /**
     * JDBC模板对象
     * 用于执行数据库操作
     */
    private final JdbcTemplate jdbcTemplate;


    /**
     * 构造函数，初始化DisposableWorkerIdAssigner实例
     *
     * @param jdbcTemplate 用于执行数据库操作的JdbcTemplate对象
     */
    public DisposableWorkerIdAssigner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 添加工作节点信息到数据库
     *
     * 此方法负责将给定的工作节点实体信息插入到数据库中对应的工作节点表中
     * 它首先准备插入语句和参数，然后执行插入操作，并从数据库获取生成的主键，
     * 最后将这个主键设置回工作节点实体对象
     *
     * @param workerNodeEntity 工作节点实体对象，包含要插入的主机名、端口、类型等信息
     */
    private void addWorkerNode(WorkerNodeEntity workerNodeEntity) {
        // 获取当前日期时间，用于记录工作节点的创建和修改时间
        Date now = new Date();

        // 创建一个KeyHolder对象，用于存储数据库插入操作后生成的主键值
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 准备插入语句的参数，按照SQL语句中的顺序排列
        Object[] args = new Object[]{workerNodeEntity.getHostName(), workerNodeEntity.getPort(), workerNodeEntity.getType() , workerNodeEntity.getLaunchDate(), now, now};

        // 构造插入SQL语句，用于向工作节点表中插入一条新记录
        String  insertSql = String.format("insert into %s (host_name, port, type, launch_date, created, modified) values (?, ?, ?, ?, ?, ?) ", workerNodeTableName);

        // 执行插入操作，并将生成的主键值存储到keyHolder中
        jdbcTemplate.update(connection -> {
            // 准备插入语句，指定返回生成的主键值
            PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);

            // 设置插入语句的参数值
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            // 返回准备好的PreparedStatement对象
            return ps;
        }, keyHolder);

        // 从keyHolder中获取生成的主键值，并设置到workerNodeEntity对象中
        workerNodeEntity.setId(keyHolder.getKey().longValue());
    }

    /**
     * Assign worker id base on database.<p>
     * If there is host name & port in the environment, we considered that the node runs in Docker container<br>
     * Otherwise, the node runs on an actual machine.
     * 
     * @return assigned worker id
     */
    @DS("uid")
    @Override
    @Transactional
    public long assignWorkerId() {
        // build worker node entity
        WorkerNodeEntity workerNodeEntity = buildWorkerNode();
        // add worker node for new (ignore the same IP + PORT)
        addWorkerNode(workerNodeEntity);
        log.info("Add worker node:" + workerNodeEntity);
        return workerNodeEntity.getId();
    }

    /**
     * Build worker node entity by IP and PORT
     */
    private WorkerNodeEntity buildWorkerNode() {
        WorkerNodeEntity workerNodeEntity = new WorkerNodeEntity();
        if (DockerUtils.isDocker()) {
            workerNodeEntity.setType(WorkerNodeType.CONTAINER.value());
            workerNodeEntity.setHostName(DockerUtils.getDockerHost());
            workerNodeEntity.setPort(DockerUtils.getDockerPort());

        } else {
            workerNodeEntity.setType(WorkerNodeType.ACTUAL.value());
            workerNodeEntity.setHostName(NetUtils.getLocalAddress());
            workerNodeEntity.setPort(System.currentTimeMillis() + "-" + RandomUtils.nextInt(100000));
        }

        return workerNodeEntity;
    }

}
