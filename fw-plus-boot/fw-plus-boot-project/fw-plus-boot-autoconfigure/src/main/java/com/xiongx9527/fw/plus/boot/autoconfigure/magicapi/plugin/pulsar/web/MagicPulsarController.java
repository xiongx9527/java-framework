package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.config.PulsarCodeConstants;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.module.PulsarModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service.PulsarDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service.PulsarSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.admin.Topics;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.impl.MessageImpl;
import org.apache.pulsar.client.impl.schema.BytesSchema;
import org.apache.pulsar.common.api.proto.MessageMetadata;
import org.apache.pulsar.common.partition.PartitionedTopicMetadata;
import org.apache.pulsar.common.policies.data.TenantInfo;
import org.apache.pulsar.common.policies.data.TopicStats;
import org.springframework.web.bind.annotation.*;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @author zzy
 */
@Slf4j
public class MagicPulsarController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * Pulsar测试主题名称
     */
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
     * 获取所有可用的Pulsar数据源存储信息
     *
     * @return 包含所有Pulsar数据源信息的JSON响应，每个数据源的命名空间格式为"租户/命名空间"
     */
    @RequestMapping("/pulsar/availableStorages")
    @ResponseBody
    public JsonBean<Object> availableStorages() {
        PulsarDataSourceInfoMagicResourceStorage pulsarDataSourceInfoMagicResourceStorage = SpringUtils.getBean(PulsarDataSourceInfoMagicResourceStorage.class);
        List<PulsarDataSourceInfo> all = ListUtils.emptyIfNull(pulsarDataSourceInfoMagicResourceStorage.findAll())
                .stream().map(e -> {
                    PulsarDataSourceInfo copy = new PulsarDataSourceInfo();
                    copy.setKey(e.getKey());
                    copy.setName(e.getName());
                    copy.setServers(e.getServers());
                    copy.setTenant(e.getTenant());
                    copy.setNamespace(e.getTenant() + "/" + e.getNamespace());
                    copy.setProperties(e.getProperties());
                    copy.setId(e.getId());
                    return copy;
                }).collect(Collectors.toList());

        return new JsonBean<>(all);
    }

    /**
     * 获取指定数据源的所有租户列表
     *
     * @param key 数据源的唯一标识符
     * @return 包含所有租户名称的JSON响应，每个租户对象包含name字段
     * @throws PulsarAdminException 当调用Pulsar Admin API失败时抛出
     * @throws PulsarClientException 当Pulsar客户端操作失败时抛出
     */
    @PostMapping("/pulsar/{key}/tenants")
    @ResponseBody
    public JsonBean<List<Object>> tenants(@PathVariable String key) throws PulsarAdminException, PulsarClientException {
        PulsarModule pulsarModule = SpringUtils.getBean(PulsarModule.class).getDynamicAttribute(key);
        return new JsonBean<>(pulsarModule.getPulsarAdmin().tenants().getTenants().stream().map(e->{
            Map<String,Object> map = new HashMap<>();
            map.put("name",e);
            return map;
        }).collect(Collectors.toList()));
    }

    /**
     * 获取指定租户的详细信息
     *
     * @param key 数据源的唯一标识符
     * @param tenant 租户名称
     * @return 包含租户详细信息的JSON响应
     * @throws PulsarAdminException 当调用Pulsar Admin API失败时抛出
     * @throws PulsarClientException 当Pulsar客户端操作失败时抛出
     */
    @PostMapping("/pulsar/{key}/tenantInfo")
    @ResponseBody
    public JsonBean<TenantInfo> tenantInfo(@PathVariable String key, String tenant) throws PulsarAdminException, PulsarClientException {
        PulsarModule pulsarModule = SpringUtils.getBean(PulsarModule.class).getDynamicAttribute(key);
        return new JsonBean<>(pulsarModule.getPulsarAdmin().tenants().getTenantInfo(tenant));
    }

    /**
     * 获取指定租户下的所有命名空间列表
     *
     * @param key 数据源的唯一标识符
     * @param tenant 租户名称
     * @return 包含所有命名空间名称的JSON响应，每个命名空间对象包含name字段
     * @throws PulsarAdminException 当调用Pulsar Admin API失败时抛出
     * @throws PulsarClientException 当Pulsar客户端操作失败时抛出
     */
    @PostMapping("/pulsar/{key}/namespaces")
    @ResponseBody
    public JsonBean<List<Object>> namespaces(@PathVariable String key,String tenant) throws PulsarAdminException, PulsarClientException {
        PulsarModule pulsarModule = SpringUtils.getBean(PulsarModule.class).getDynamicAttribute(key);
        return new JsonBean<>(pulsarModule.getPulsarAdmin().namespaces().getNamespaces(tenant).stream().map(e->{
            Map<String,Object> map = new HashMap<>();
            map.put("name",e);
            return map;
        }).collect(Collectors.toList()));
    }

    /**
     * 获取指定命名空间下的所有主题列表
     *
     * @param key 数据源的唯一标识符
     * @param namespace 命名空间名称
     * @return 包含所有主题名称的JSON响应，每个主题对象包含name字段
     * @throws PulsarAdminException 当调用Pulsar Admin API失败时抛出
     * @throws PulsarClientException 当Pulsar客户端操作失败时抛出
     */
    @PostMapping("/pulsar/{key}/topics")
    @ResponseBody
    public JsonBean<List<Object>> topics(@PathVariable String key,String namespace) throws PulsarAdminException, PulsarClientException {
        PulsarModule pulsarModule = SpringUtils.getBean(PulsarModule.class).getDynamicAttribute(key);
        return new JsonBean<>(pulsarModule.getPulsarAdmin().topics().getList(namespace).stream().map(e->{
            Map<String,Object> map = new HashMap<>();
            map.put("name",e);
            return map;
        }).collect(Collectors.toList()));
    }

    /**
     * 获取指定主题的元数据和统计信息
     *
     * @param key 数据源的唯一标识符
     * @param topic 主题名称
     * @return 包含主题分区元数据和统计信息的JSON响应，包括partitionedTopicMetadata和stats两个部分
     * @throws PulsarClientException 当Pulsar客户端操作失败时抛出
     * @throws PulsarAdminException 当调用Pulsar Admin API失败时抛出
     */
    @PostMapping("/pulsar/{key}/topicInfo")
    @ResponseBody
    public JsonBean<Object> partitionedTopicMetadata(@PathVariable String key,String topic) throws PulsarClientException, PulsarAdminException {
        PulsarModule pulsarModule = SpringUtils.getBean(PulsarModule.class).getDynamicAttribute(key);
        Topics topics = pulsarModule.getPulsarAdmin().topics();
        PartitionedTopicMetadata partitionedTopicMetadata = topics.getPartitionedTopicMetadata(topic);
        Map<String,Object> topicInfo = new HashMap<>();
        topicInfo.put("partitionedTopicMetadata",partitionedTopicMetadata);
        TopicStats topicStats = topics.getStats(topic);
        topicInfo.put("stats", topicStats);
        return new JsonBean<>(topicInfo);
    }


    /**
     * 修改指定消费者组的消费位点
     *
     * @param key 数据源的唯一标识符，用于获取对应的Pulsar模块实例
     * @param topic 主题名称，需要修改消费位点的目标主题
     * @param group 消费者组名称，需要调整消费位点的消费者组
     * @return 操作结果封装在JsonBean对象中返回，成功时返回空的JsonBean
     * @throws PulsarClientException 当Pulsar客户端操作失败时抛出，如连接异常、超时等
     * @throws PulsarAdminException 当调用Pulsar Admin API失败时抛出，如权限不足、主题不存在等
     */
    @RequestMapping("/pulsar/{key}/alterConsumerGroupOffsets")
    @ResponseBody
    public JsonBean<Object> alterConsumerGroupOffsets(@PathVariable String key,
                                                      @RequestParam String topic,
                                                      @RequestParam String group) throws PulsarClientException, PulsarAdminException {
        PulsarModule pulsarModule = SpringUtils.getBean(PulsarModule.class).getDynamicAttribute(key);
        pulsarModule.alterConsumerGroupOffsets(topic, group);
        return new JsonBean<>();
    }

    /**
     * 删除指定的Pulsar主题
     *
     * @param key 数据源的唯一标识符，用于获取对应的Pulsar模块实例
     * @param topic 主题名称，需要删除的目标主题
     * @return 操作结果封装在JsonBean对象中返回，成功时返回空的JsonBean
     * @throws PulsarClientException 当Pulsar客户端操作失败时抛出，如连接异常、超时等
     * @throws PulsarAdminException 当调用Pulsar Admin API失败时抛出，如权限不足、主题不存在或主题非空等
     */
    @RequestMapping("/pulsar/{key}/deleteTopics")
    @ResponseBody
    public JsonBean<Object> deleteTopics(@PathVariable String key,
                                         @RequestParam String topic) throws PulsarClientException, PulsarAdminException {
        PulsarModule pulsarModule = SpringUtils.getBean(PulsarModule.class).getDynamicAttribute(key);
        Topics topics = pulsarModule.getPulsarAdmin().topics();
        topics.delete(topic);
        return new JsonBean<>();
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
     * 删除前校验：检查是否有订阅配置引用了该数据源
     *
     * @param properties Pulsar数据源配置信息
     * @return JsonBean<Object> 如果没有订阅引用返回"ok"，否则返回错误信息
     */
    @RequestMapping("/pulsar/datasource/doDelete")
    @ResponseBody
    public JsonBean<Object> doDelete(@RequestBody PulsarDataSourceInfo properties) {
        PulsarSubscribeInfoMagicResourceStorage subscribeStorage = SpringUtils.getBean(PulsarSubscribeInfoMagicResourceStorage.class);
        List<PulsarSubscribeInfo> subscribes = ListUtils.emptyIfNull(subscribeStorage.findAll());
        List<PulsarSubscribeInfo> referenced = subscribes.stream()
                .filter(it -> Objects.equals(it.getKey(), properties.getKey()))
                .collect(Collectors.toList());
        if (!referenced.isEmpty()) {
            String names = referenced.stream()
                    .map(it -> it.getName() + "(" + it.getTopic() + ")")
                    .collect(Collectors.joining("、"));
            return new JsonBean<>(PulsarCodeConstants.PULSAR_DATASOURCE_REFERENCED_BY_SUBSCRIBE.getMessage() + "：" + names);
        }
        return new JsonBean<>("ok");
    }

    /**
     * 删除前校验：检查订阅是否处于启用状态，启用状态不允许删除
     *
     * @param properties Pulsar订阅配置信息
     * @return JsonBean<Object> 如果未启用返回"ok"，否则返回错误信息
     */
    @RequestMapping("/pulsar/subscribe/doDelete")
    @ResponseBody
    public JsonBean<Object> doDeleteSubscribe(@RequestBody PulsarSubscribeInfo properties) {
        if (properties.isEnabled()) {
            return new JsonBean<>(PulsarCodeConstants.PULSAR_SUBSCRIBE_ENABLED_CANNOT_DELETE.getMessage() + "：「" + properties.getName() + "」");
        }
        return new JsonBean<>("ok");
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
        servers = MagicAPIConfigUtils.getPulsarConfig(properties.getKey(), "servers", servers);
        String tenant = properties.getTenant();
        tenant = MagicAPIConfigUtils.getPulsarConfig(properties.getKey(), "tenant", tenant);
        String namespace = properties.getNamespace();
        namespace = MagicAPIConfigUtils.getPulsarConfig(properties.getKey(), "namespace", namespace);
        // 构造Pulsar服务URL
        String serviceUrl = "pulsar://" + servers;
        PulsarClient client = null;
        try {
            // 创建Pulsar客户端
            client = PulsarClient.builder()
                    .serviceUrl(serviceUrl)
                    .operationTimeout(10, TimeUnit.SECONDS)
                    .connectionTimeout(10, TimeUnit.SECONDS)
                    .build();
            String newTopicName = createTopicName(tenant , namespace , PULSAR_TEST_TOPIC) ;
            // 测试与Pulsar服务器的连接
            client.getPartitionsForTopic(newTopicName).get();
            // 如果连接成功，返回成功信息
            return new JsonBean<>("ok");
        } catch (Exception e) {
            // 如果连接失败，返回连接失败信息
            return new JsonBean<>(PulsarCodeConstants.PULSAR_DATASOURCE_CONNECT_FAILED.getMessage() + ": " + e.getMessage());
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
