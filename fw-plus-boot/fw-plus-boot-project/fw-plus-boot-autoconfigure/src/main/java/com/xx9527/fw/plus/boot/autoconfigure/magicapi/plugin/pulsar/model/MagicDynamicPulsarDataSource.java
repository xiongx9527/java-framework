package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer.PulsarConsumer;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer.PulsarConsumerService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.producer.PulsarProducer;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.shade.com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pulsar 动态数据源对象
 *
 * @author tc
 */
@Slf4j
public class MagicDynamicPulsarDataSource implements DisposableBean {

    /**
     * 用于解析JSON配置的ObjectMapper实例（Pulsar shaded版本）
     */
    private static final ObjectMapper shadedMapper = new org.apache.pulsar.shade.com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 存储所有Pulsar客户端实例的映射
     * key为数据源的唯一标识，value为对应的PulsarClient实例
     */
    private final Map<String, PulsarClient> pulsarClientMap = new ConcurrentHashMap<>();

    /**
     * 存储所有Pulsar生产者实例的映射
     * key为生产者数据源的唯一标识，value为对应的PulsarProducer实例
     */
    private final Map<String, PulsarProducer> producerMap = new ConcurrentHashMap<>();

    /**
     * 存储所有Pulsar消费者实例的映射
     * key为消费者数据源的唯一标识，value为对应的PulsarConsumer实例
     */
    private final Map<String, PulsarConsumer> consumerMap = new ConcurrentHashMap<>();

    /**
     * 存储所有Pulsar Admin实例的映射
     * key为数据源的唯一标识，value为对应的PulsarAdmin实例存储所有Pulsar管理员实例的映射
     */
    private final Map<String, PulsarAdmin> pulsarAdminMap = new ConcurrentHashMap<>();

    /**
     * Pulsar消费者服务实例
     * 用于从Pulsar主题接收消息
     */
    private final PulsarConsumerService pulsarConsumerService;

    /**
     * 默认的Pulsar管理员实例
     */
    private volatile PulsarAdmin defaultPulsarAdmin;

    /** 生产者操作锁，保护 producerMap 的并发读写 */
    private final Object producerLock = new Object();

    /** 客户端操作锁，保护 pulsarClientMap 的 DCL */
    private final Object clientLock = new Object();

    /**
     * 构造函数，初始化Pulsar动态数据源
     *
     * @param pulsarConsumerService Pulsar消费者服务
     */
    public MagicDynamicPulsarDataSource(PulsarConsumerService pulsarConsumerService) {
        this.pulsarConsumerService = pulsarConsumerService;
    }

    /**
     * 获取Pulsar消费者服务
     *
     * @return PulsarConsumerService Pulsar消费者服务实例
     */
    public PulsarConsumerService getPulsarConsumerService() {
        return this.pulsarConsumerService;
    }


    /**
     * 获取默认的PulsarAdmin实例
     *
     * @return PulsarAdmin实例，如果未初始化则返回null
     */
    public PulsarAdmin getDefaultPulsarAdmin() {
        return this.defaultPulsarAdmin;
    }

    /**
     * 创建并初始化默认的PulsarAdmin实例
     * 使用双重检查锁定（DCL）确保在并发环境下只初始化一次
     *
     * @param serviceHttpUrl Pulsar Admin服务的HTTP地址
     * @throws PulsarClientException 当客户端创建失败时抛出
     */
    public void createDefaultPulsarAdmin(String serviceHttpUrl) throws PulsarClientException {
        if (this.defaultPulsarAdmin == null) {
            synchronized (this) {
                if (this.defaultPulsarAdmin == null) {
                    this.defaultPulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(serviceHttpUrl).build();
                }
            }
        }
    }

    /**
     * 向pulsarClientMap中添加Pulsar客户端对象
     * 使用双重检查锁定模式优化并发性能，避免重复创建客户端
     *
     * @param info Pulsar数据源信息，包含连接到Pulsar服务所需的信息
     * @throws PulsarClientException 当创建Pulsar客户端对象时发生错误，将抛出此异常
     */
    private void putPulsarClient(PulsarDataSourceInfo info) throws PulsarClientException {
        if (this.pulsarClientMap.containsKey(info.getKey())) {
            return;
        }
        synchronized (this.clientLock) {
            if (this.pulsarClientMap.containsKey(info.getKey())) {
                return;
            }
            // 使用明确的配置Map替代JSON解析
            Map<String, Object> pulsarConfig = new HashMap<>();
            String servers = MagicAPIConfigUtils.getPulsarConfig(info.getKey(), "servers", info.getServers());
            if (StringUtils.isBlank(servers)) {
                throw new PulsarClientException("Pulsar servers 不能为空，key: " + info.getKey());
            }
            pulsarConfig.put("serviceUrl", "pulsar://" + servers);
            String others = MagicAPIConfigUtils.getPulsarConfig(info.getKey(), "other", info.getOther());
            // 添加其他必要配置
            if (StringUtils.isNotBlank(others)) {
                try {
                    Map<String, Object> otherConfig = shadedMapper.readValue(others,
                            new org.apache.pulsar.shade.com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
                    pulsarConfig.putAll(otherConfig);
                } catch (Exception e) {
                    log.error("解析Pulsar配置失败", e);
                }
            }

            // 构建客户端
            this.pulsarClientMap.put(info.getKey(), PulsarClient.builder()
                    .loadConf(pulsarConfig)
                    .build());
        }
    }

    /**
     * 创建并缓存PulsarAdmin实例
     * 使用双重检查锁定确保并发安全，如果已存在则直接返回。在替换旧实例时会先关闭旧连接以防止资源泄漏。
     *
     * @param info Pulsar数据源配置信息
     * @return 创建或获取成功返回true，失败返回false
     */
    public boolean putPulsarAdmin(PulsarDataSourceInfo info) {
        if (this.pulsarAdminMap.containsKey(info.getKey())) {
            return true;
        }
        synchronized (this.pulsarAdminMap) {
            if (this.pulsarAdminMap.containsKey(info.getKey())) {
                return true;
            }
            try {
                String adminUrl = info.getServiceUrls();
                adminUrl = MagicAPIConfigUtils.getPulsarConfig(info.getKey(), "serviceHttpUrl", adminUrl);
                if (StringUtils.isBlank(adminUrl)) {
                    throw new PulsarClientException("Pulsar Admin serviceHttpUrl 不能为空，key: " + info.getKey());
                }
                PulsarAdmin pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(adminUrl).build();
                PulsarAdmin oldPulsarAdmin = this.pulsarAdminMap.put(info.getKey(), pulsarAdmin);
                if (oldPulsarAdmin != null) {
                    try {
                        oldPulsarAdmin.close();
                    } catch (Exception e) {
                        log.error("Failed to close old Pulsar admin", e);
                    }
                }
                return true;
            } catch (Exception e) {
                log.error("Failed to create Pulsar admin", e);
                return false;
            }
        }
    }

    /**
     * 注册生产者数据源（可以运行时注册）
     *
     * @param info 数据源信息
     */
    public boolean putProducer(PulsarDataSourceInfo info) {
        synchronized (this.producerLock) {
            try {
                putPulsarClient(info);
                PulsarProducer oldProducer = this.producerMap.put(info.getKey(), new PulsarProducer(this, info));
                if (null != oldProducer) {
                    oldProducer.close();
                }
                log.info("注册 Pulsar 生产者:[{}]完成执行", info.getKey());
            } catch (Exception e) {
                log.error("Pulsar 生产者:[{}]注册失败", info.getKey(), e);
                return false;
            }
        }
        return true;
    }

    /**
     * 注册消费者数据源（可以运行时注册）
     *
     * @param info 数据源信息
     */
    public boolean putConsumer(PulsarDataSourceInfo info, List<PulsarSubscribeScriptInfo> subscribeScripteInfoList) {
        synchronized (this.consumerMap) {
            try {
                putPulsarClient(info);
                PulsarConsumer pulsarConsumer = this.consumerMap.get(info.getKey());
                if (pulsarConsumer != null) {
                    pulsarConsumer.createPulsarConsumer(subscribeScripteInfoList);
                } else {
                    pulsarConsumer = new PulsarConsumer(this, info, subscribeScripteInfoList);
                    this.consumerMap.put(info.getKey(), pulsarConsumer);
                }
                log.info("注册 Pulsar 消费者:[{}]完成执行", info.getKey());
            } catch (Exception e) {
                log.error("Pulsar 消费者:[{}]注册失败", info.getKey(),e);
                return false;
            }
        }
        return true;
    }

    /**
     * 删除生产者数据源
     *
     * @param datasourceKey 数据源Key
     */
    public void deleteProducer(String datasourceKey) {
        if (StringUtils.isEmpty(datasourceKey)) {
            return;
        }
        // 分别在各自的锁保护下清理资源，避免与 putPulsarClient(clientLock) / putPulsarAdmin(pulsarAdminMap) 产生竞态
        synchronized (this.producerLock) {
            PulsarProducer producer = this.producerMap.remove(datasourceKey);
            if (producer != null) {
                producer.close();
            }
        }
        synchronized (this.clientLock) {
            PulsarClient pulsarClient = this.pulsarClientMap.remove(datasourceKey);
            if (pulsarClient != null) {
                try {
                    pulsarClient.close();
                } catch (PulsarClientException e) {
                    log.error("Failed to close Pulsar client", e);
                }
            }
        }
        synchronized (this.pulsarAdminMap) {
            PulsarAdmin pulsarAdmin = this.pulsarAdminMap.remove(datasourceKey);
            if (pulsarAdmin != null) {
                try {
                    pulsarAdmin.close();
                } catch (Exception e) {
                    log.error("Failed to close Pulsar admin", e);
                }
            }
        }
    }

    /**
     * 获取全部生产者数据源
     */
    public Collection<PulsarProducer> producerNodes() {
        List<PulsarProducer> producers = new ArrayList<>(this.producerMap.values());
        try {
            Producer producer = SpringUtils.getBean(Producer.class);
            if (producer != null) {
                producers.add(new PulsarProducer(this, producer));
            }
        } catch (Exception e) {
            log.debug("未找到默认 Producer bean", e);
        }
        return producers;
    }

    /**
     * 获取生产者数据源
     *
     * @param datasourceKey 数据源Key
     */
    public PulsarProducer getProducer(String datasourceKey) {
        return this.producerMap.get(datasourceKey);
    }

    /**
     * 获取消费者数据源
     *
     * @param datasourceKey 数据源Key
     */
    public PulsarConsumer getConsumer(String datasourceKey) {
        return this.consumerMap.get(datasourceKey);
    }

    /**
     * 根据数据源键获取对应的Pulsar客户端实例
     *
     * @param datasourceKey 数据源的唯一键，用于区分不同的数据源
     * @return PulsarClient 实例，如果找不到对应的实例，则返回null
     */
    public PulsarClient getPulsarClient(String datasourceKey) {
        return this.pulsarClientMap.get(datasourceKey);
    }

    /**
     * 获取全部Pulsar Admin实例
     */
    public PulsarAdmin getPulsarAdmin(String datasourceKey) {
        return this.pulsarAdminMap.get(datasourceKey);
    }

    /**
     * 销毁方法，用于清理资源
     * <p>
     * 该方法会依次关闭所有Pulsar生产者、消费者和客户端连接，并清空对应的映射表。
     * 主要执行以下操作：
     * 1. 关闭所有Pulsar生产者并清空生产者映射表
     * 2. 关闭所有Pulsar消费者并清空消费者映射表
     * 3. 关闭所有Pulsar客户端并清空客户端映射表
     * </p>
     *
     * @throws Exception 如果在关闭过程中发生异常
     */
    @Override
    public void destroy() throws Exception {
        // 1. 先关闭所有消费者（停止接收新消息），保证 DLT 发送时生产者仍可用
        synchronized (this.consumerMap) {
            for (PulsarConsumer consumer : this.consumerMap.values()) {
                consumer.close();
            }
            this.consumerMap.clear();
        }

        // 2. 再关闭所有生产者（停止发送消息）
        synchronized (this.producerLock) {
            for (PulsarProducer producer : this.producerMap.values()) {
                producer.close();
            }
            this.producerMap.clear();
        }

        // 关闭所有Pulsar客户端并清空映射表
        synchronized (this.clientLock) {
            for (PulsarClient client : this.pulsarClientMap.values()) {
                if (null != client && !client.isClosed()) {
                    try {
                        // 尝试关闭client对象
                        client.close();
                    } catch (Exception e) {
                        // 记录关闭客户端时出现的异常
                        log.error("Failed to close Pulsar client", e);
                    }
                }
            }
            this.pulsarClientMap.clear();
        }

        synchronized (this.pulsarAdminMap) {
            for (PulsarAdmin admin : this.pulsarAdminMap.values()) {
                if (null != admin) {
                    try {
                        // 尝试关闭admin对象
                        admin.close();
                    } catch (Exception e) {
                        // 记录关闭admin时出现的异常
                        log.error("Failed to close Pulsar admin", e);
                    }
                }
            }
            this.pulsarAdminMap.clear();
        }

        if (this.defaultPulsarAdmin != null) {
            try {
                this.defaultPulsarAdmin.close();
            } catch (Exception e) {
                log.error("Failed to close default Pulsar admin", e);
            }
            this.defaultPulsarAdmin = null;
        }
    }

}
