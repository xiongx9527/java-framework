package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

@Slf4j
public class MagicRedisController extends MagicController implements MagicExceptionHandler , BaseService {

    public MagicRedisController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 测试Redis数据源连接是否可用。
     *
     * 该方法接收一个Redis配置信息对象，动态创建对应的Redis连接工厂并尝试建立连接，
     * 根据不同的模式（单机、集群、哨兵）进行连接测试，并返回测试结果。
     *
     * @param properties Redis数据源配置信息，包括地址、端口、密码等参数
     * @return JsonBean 返回测试结果，"ok" 表示连接成功，其他表示错误信息
     */
    @RequestMapping("/redis/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody RedisDataSourceInfo properties) {
        MagicDynamicRedisDataSource magicDynamicRedisDataSource = SpringUtils.getBean(MagicDynamicRedisDataSource.class) ;
        RedisConnectionFactory connectionFactory = null;
        try {
            // 验证传入的Redis配置字段是否合法
            validateField(properties);

            // 动态创建Redis连接工厂
            connectionFactory = magicDynamicRedisDataSource.createRedisConnectionFactory(properties);

            // 初始化连接工厂：根据具体实现类型调用其初始化方法
            if (connectionFactory instanceof LettuceConnectionFactory) {
                ((LettuceConnectionFactory) connectionFactory).afterPropertiesSet();
            } else if (connectionFactory instanceof JedisConnectionFactory) {
                ((JedisConnectionFactory) connectionFactory).afterPropertiesSet();
            }

            String ping = null;
            try {
                // 根据不同模式选择相应的连接方式进行测试
                if ("standalone".equals(properties.getMode())) {
                    // 单机模式配置
                    RedisConnection connection = connectionFactory.getConnection();
                    if (connection != null) {
                        ping = connection.ping();
                    }
                } else if ("cluster".equals(properties.getMode())) {
                    // 集群模式配置
                    RedisClusterConnection clusterConnection = connectionFactory.getClusterConnection();
                    if (clusterConnection != null) {
                        ping = clusterConnection.ping();
                    }
                } else if ("sentinel".equals(properties.getMode())) {
                    // 哨兵模式配置
                    RedisSentinelConnection sentinelConnection = connectionFactory.getSentinelConnection();
                    if (sentinelConnection != null) {
                        // 哨兵连接通常不支持ping命令，连接成功即表示正常
                        // 我们可以通过获取主节点信息来验证连接
                        sentinelConnection.masters();
                        ping = "PONG"; // 连接成功即表示正常
                    }
                }
            } catch (Exception e) {
                log.warn("无法获取Redis连接信息", e);
            }

            // 判断连接是否成功
            if ("PONG".equalsIgnoreCase(ping)) {
                return new JsonBean<>("ok");
            } else {
                return new JsonBean<>("无法建立Redis连接");
            }
        } catch (Exception e) {
            log.error("测试Redis数据源配置失败", e);
            return new JsonBean<>(e.getMessage());
        } finally {
            // 销毁临时创建的Redis连接工厂资源
            try {
                magicDynamicRedisDataSource.destroyRedisConnectionFactory(connectionFactory);
            } catch (Exception e) {
                log.error("销毁Redis数据源配置失败", e);
            }
        }
    }

}
