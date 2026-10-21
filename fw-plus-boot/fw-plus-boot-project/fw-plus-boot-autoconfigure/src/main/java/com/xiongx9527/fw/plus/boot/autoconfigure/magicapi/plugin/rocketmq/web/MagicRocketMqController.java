package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.common.message.MessageExt;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.config.WebSocketSessionManager;
import org.ssssssss.magicapi.core.logging.MagicLoggerContext;
import org.ssssssss.magicapi.core.model.DebugRequest;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.ServiceState;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.script.MagicScriptDebugContext;

import java.nio.charset.StandardCharsets;


/**
 * @author zzy
 */
@Slf4j
public class MagicRocketMqController extends MagicController implements MagicExceptionHandler {

    /**
     * MagicRocketMqController构造函数
     *
     * @param configuration 魔法配置对象，用于初始化控制器的基础配置
     */
    public MagicRocketMqController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 执行指定ID的脚本文件
     *
     * @param id 脚本文件的唯一标识符
     * @param request 包含执行请求信息的HTTP请求对象
     * @return 返回脚本执行结果的JsonBean对象
     */
    @PostMapping("/rocketmq/execute")
    @ResponseBody
    public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
        // 获取指定ID的脚本实体
        RocketMqSubscribeInfo entity = MagicConfiguration.getMagicResourceService().file(id);
        notNull(entity, FILE_NOT_FOUND);
        String script = entity.getScript();
        DebugRequest debugRequest = DebugRequest.create(request);

        // 设置日志上下文和会话信息
        MagicLoggerContext.SESSION.set(debugRequest.getRequestedClientId());
        String sessionAndScriptId = debugRequest.getRequestedClientId() + debugRequest.getRequestedScriptId();
        try {
            // 创建脚本执行上下文并添加到会话管理器
            MagicScriptDebugContext magicScriptContext = debugRequest.createMagicScriptContext(configuration.getDebugTimeout());
            WebSocketSessionManager.addMagicScriptContext(sessionAndScriptId, magicScriptContext);
            magicScriptContext.setScriptName(MagicConfiguration.getMagicResourceService().getScriptName(entity));
            MessageExt msg = new MessageExt();
            byte[] bytes = StringUtils.defaultIfEmpty(entity.getParams(), "").getBytes(StandardCharsets.UTF_8);
            msg.setBody(bytes);
            // 执行脚本并返回结果
            return new JsonBean<>(ScriptManager.executeScript(script, magicScriptContext));
        } finally {
            // 清理资源：移除脚本上下文和日志会话
            WebSocketSessionManager.removeMagicScriptContext(sessionAndScriptId);
            MagicLoggerContext.SESSION.remove();
        }
    }

    /**
     * 测试RocketMQ数据源连接
     *
     * @param properties 包含RocketMQ连接配置信息的数据源信息对象
     * @return 返回连接测试结果的JsonBean对象，成功返回"ok"，失败返回错误信息
     */
    @RequestMapping("/rocketmq/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody RocketMqDataSourceInfo properties) {
        DefaultMQProducer newProducer = null;
        try {
            if (StringUtils.isBlank(properties.getAccessKey()) || StringUtils.isBlank(properties.getSecretKey()) ) {
                newProducer = new DefaultMQProducer();
            } else {
                newProducer = new DefaultMQProducer(
                        new AclClientRPCHook(new SessionCredentials(properties.getAccessKey(),properties.getSecretKey())));
            }
            newProducer.setNamesrvAddr(properties.getNameServer());
            String group = properties.getGroup();
            if (StringUtils.isBlank(group)) {
                group = "magic-api";
            }
            newProducer.setProducerGroup(group);
            newProducer.start();
            // 检查生产者服务状态
            if (newProducer.getDefaultMQProducerImpl().getServiceState() != ServiceState.RUNNING) {
                throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.rocketmq.serviceNotStarted"));
            }
            return new JsonBean<>("ok");
        } catch (MQClientException e) {
            // 处理连接异常
            return new JsonBean<>(e.getMessage());
        } finally {
            // 确保生产者被正确关闭
            if (newProducer != null) {
                try {
                    newProducer.shutdown();
                } catch (Exception e){
                    log.error("关闭生产者时发生异常", e);
                }
            }
        }
    }

}
