package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.impl.MessageImpl;
import org.apache.pulsar.client.impl.schema.BytesSchema;
import org.apache.pulsar.common.api.proto.MessageMetadata;
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;



/**
 * @author zzy
 */
@Slf4j
public class MagicPulsarController extends MagicController implements MagicExceptionHandler , BaseService {

    // 提取为类级别的常量
    private static final String PULSAR_TEST_TOPIC = "magic-api-test-topic";

    /**
     * 构造函数，用于创建MagicPulsarController实例
     *
     * @param configuration 魔法配置对象，用于初始化控制器的配置参数
     */
    public MagicPulsarController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 处理Pulsar脚本执行请求
     *
     * @param id 脚本的唯一标识符，用于定位需要执行的具体脚本
     * @param request HTTP请求对象，用于获取调试请求信息，包括客户端ID和脚本ID等上下文数据
     * @return 执行结果封装在JsonBean对象中返回给调用方
     */
    @PostMapping("/pulsar/execute")
    @ResponseBody
    public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
        // 根据脚本ID获取脚本实体
        PulsarSubscribeInfo entity = MagicConfiguration.getMagicResourceService().file(id);
        // 检查脚本实体是否存在，如果不存在则抛出异常
        notNull(entity, FILE_NOT_FOUND);

        // 获取脚本内容
        String script = entity.getScript();
        // 创建调试请求对象
        DebugRequest debugRequest = DebugRequest.create(request);
        // 设置日志上下文的会话ID
        MagicLoggerContext.SESSION.set(debugRequest.getRequestedClientId());
        // 构造会话和脚本ID的组合键
        String sessionAndScriptId = debugRequest.getRequestedClientId() + debugRequest.getRequestedScriptId();
        try {
            // 创建魔法脚本调试上下文，并设置调试超时时间
            MagicScriptDebugContext magicScriptContext = debugRequest.createMagicScriptContext(configuration.getDebugTimeout());
            // 将魔法脚本上下文添加到WebSocket会话管理器中
            WebSocketSessionManager.addMagicScriptContext(sessionAndScriptId, magicScriptContext);
            // 设置脚本名称
            magicScriptContext.setScriptName(MagicConfiguration.getMagicResourceService().getScriptName(entity));
            // 构造测试消息并设置到脚本上下文中
            byte[] data = StringUtils.defaultIfEmpty(entity.getParams(),"").getBytes(StandardCharsets.UTF_8);
            Message message = MessageImpl.create(new MessageMetadata(), ByteBuffer.wrap(data), new BytesSchema(), PULSAR_TEST_TOPIC);
            magicScriptContext.set("msg", message);
            // 执行脚本并返回结果
            return new JsonBean<>(ScriptManager.executeScript(script, magicScriptContext));
        } finally {
            // 清理资源：移除WebSocket会话管理器中的魔法脚本上下文
            WebSocketSessionManager.removeMagicScriptContext(sessionAndScriptId);
            // 清理资源：移除日志上下文中的会话ID
            MagicLoggerContext.SESSION.remove();
        }
    }

    /**
     * 处理Pulsar数据源测试请求
     * 该方法用于测试Pulsar数据源连接是否正常
     *
     * @param properties Pulsar数据源配置信息，包含服务器地址、租户、命名空间等信息
     * @return JsonBean<Object> 返回测试结果，成功返回"ok"，失败返回错误信息
     */
    @RequestMapping("/pulsar/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody PulsarDataSourceInfo properties) {
        String servers = properties.getServers();
        servers = SpringUtils.getAppConfig(String.format("magic.api.pulsar.%s.servers",properties.getKey()),servers);
        String tenant = properties.getTenant();
        tenant = SpringUtils.getAppConfig(String.format("magic.api.pulsar.%s.tenant",properties.getKey()),tenant);
        String namespace = properties.getNamespace();
        namespace = SpringUtils.getAppConfig(String.format("magic.api.pulsar.%s.namespace",properties.getKey()),namespace);
        // 构造Pulsar服务URL
        String serviceUrl = "pulsar://" + servers;
        PulsarClient client = null;
        try {
            // 创建Pulsar客户端
            client = PulsarClient.builder()
                    .serviceUrl(serviceUrl)
                    .build();
            String newTopicName = createTopicName(tenant , namespace , PULSAR_TEST_TOPIC) ;
            // 测试与Pulsar服务器的连接
            client.getPartitionsForTopic(newTopicName).get();
            // 如果连接成功，返回成功信息
            return new JsonBean<>("ok");
        } catch (Exception e) {
            // 如果连接失败，返回连接失败信息
            return new JsonBean<>("连接失败: " + e.getMessage());
        } finally {
            if (client != null) {
                try {
                    // 关闭Pulsar客户端连接
                    client.close();
                } catch (Exception e) {
                    // 忽略关闭连接时发生的异常
                    log.warn("关闭Pulsar客户端时发生异常", e);
                }
            }
        }
    }


}
