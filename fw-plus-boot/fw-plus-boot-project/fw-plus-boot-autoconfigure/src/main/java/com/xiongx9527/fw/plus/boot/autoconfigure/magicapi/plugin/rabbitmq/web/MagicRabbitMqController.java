package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.config.WebSocketSessionManager;
import org.ssssssss.magicapi.core.logging.MagicLoggerContext;
import org.ssssssss.magicapi.core.model.DebugRequest;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptDebugContext;

import java.nio.charset.StandardCharsets;


@Slf4j
public class MagicRabbitMqController extends MagicController implements MagicExceptionHandler, BaseService {

    /**
     * 构造函数，用于创建MagicRabbitMqController实例
     *
     * @param configuration 魔法配置对象，用于初始化控制器的基础配置
     */
    public MagicRabbitMqController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 执行RabbitMQ脚本请求
     *
     * @param id 请求的脚本ID
     * @param request HTTP请求对象，包含执行脚本所需的参数和上下文信息
     * @return JsonBean<Object> 包含脚本执行结果的JSON响应对象
     */
    @PostMapping({"/rabbitmq/subscribe/execute"})
    @ResponseBody
    public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
        // 获取指定ID的魔法实体资源
        RabbitMqSubscribeInfo entity = MagicConfiguration.getMagicResourceService().file(id);
        this.notNull(entity, FILE_NOT_FOUND);
        String script = entity.getScript();
        // 创建调试请求对象并设置日志上下文
        DebugRequest debugRequest = DebugRequest.create(request);
        MagicLoggerContext.SESSION.set(debugRequest.getRequestedClientId());
        String sessionAndScriptId = debugRequest.getRequestedClientId() + debugRequest.getRequestedScriptId();
        JsonBean jsonBean;
        try {
            // 创建脚本执行上下文并添加到会话管理器中
            MagicScriptDebugContext magicScriptContext = debugRequest.createMagicScriptContext(this.configuration.getDebugTimeout());
            WebSocketSessionManager.addMagicScriptContext(sessionAndScriptId, magicScriptContext);
            byte[] bytes = StringUtils.defaultIfEmpty(entity.getParams(), "").getBytes(StandardCharsets.UTF_8);
            Message msg = new Message(bytes);
            magicScriptContext.set("msg", msg);
            magicScriptContext.setScriptName(MagicConfiguration.getMagicResourceService().getScriptName(entity));
            // 执行脚本并封装执行结果
            jsonBean = new JsonBean(ScriptManager.executeScript(script, magicScriptContext));
        } finally {
            // 清理资源：移除脚本执行上下文和日志上下文
            WebSocketSessionManager.removeMagicScriptContext(sessionAndScriptId);
            MagicLoggerContext.SESSION.remove();
        }
        return jsonBean;
    }


    /**
     * 测试RabbitMQ数据源连接
     *
     * @param properties RabbitMQ数据源配置信息
     * @return 连接测试结果，成功返回"ok"，失败返回错误信息
     */
    @RequestMapping({"/rabbitmq/datasource/test"})
    @ResponseBody
    public JsonBean<Object> test(@RequestBody RabbitMqDataSourceInfo properties) {
        CachingConnectionFactory connectionFactory = null;
        Connection connection = null;
        try {
            validateField(properties);
            connectionFactory = new CachingConnectionFactory();
            String host = properties.getHost();
            host = MagicAPIConfigUtils.getRabbitMQConfig(properties.getKey(), "host", host);
            connectionFactory.setHost(host);
            String portStr = properties.getPort();
            portStr = MagicAPIConfigUtils.getRabbitMQConfig(properties.getKey(), "port", portStr);
            int port = Integer.parseInt(portStr);
            connectionFactory.setPort(port);
            if (StringUtils.isNotBlank(properties.getUsername()) && StringUtils.isNotBlank(properties.getPassword())) {
                String username = properties.getUsername();
                username = MagicAPIConfigUtils.getRabbitMQConfig(properties.getKey(), "username", username);
                String password = properties.getPassword() ;
                password = MagicAPIConfigUtils.getRabbitMQConfig(properties.getKey(), "password", password);
                connectionFactory.setUsername(username);
                connectionFactory.setPassword(password);
            }
            if (StringUtils.isNotBlank(properties.getVirtualHost())) {
                String virtualHost = properties.getVirtualHost();
                virtualHost = MagicAPIConfigUtils.getRabbitMQConfig(properties.getKey(), "virtualHost", virtualHost);
                connectionFactory.setVirtualHost(virtualHost);
            }
            connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
            connectionFactory.setPublisherReturns(true);
            connection = connectionFactory.createConnection();
            return new JsonBean<>("ok");
        } catch (Exception e) {
            log.error("RabbitMq连接测试失败", e);
            return new JsonBean(e.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception closeException) {
                    log.warn("关闭RabbitMq连接时发生异常", closeException);
                }
            }
            // 确保资源正确释放
            if (connectionFactory != null) {
                try {
                    connectionFactory.destroy();
                } catch (Exception closeException) {
                    log.warn("关闭RabbitMq连接时发生异常", closeException);
                }
            }
        }
    }

}
