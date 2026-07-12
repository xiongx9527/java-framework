package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.common.BaseService;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MagicDynamicLangchain4jDataSource implements BaseService, DisposableBean {

    /**
     * 模型映射表，用于存储不同类型的模型实例
     * key为模型类型标识，value为该类型下具体的模型实例映射
     * 使用ConcurrentHashMap保证线程安全
     */
    private Map<String, Map<String, LangChain4jModel>> modelMap = new ConcurrentHashMap<>();

    /**
     * 数据源信息映射表，用于存储数据源的配置信息
     * key为数据源标识，value为对应的数据源信息对象
     * 使用ConcurrentHashMap保证线程安全
     */
    private Map<String, Langchain4jDataSourceInfo> infoMap = new ConcurrentHashMap<>();

    /**
     * 根据数据源信息构建LangChain4j模型实例
     *
     * @param info 包含模型配置信息的数据源对象，包括基础URL、API密钥、模型名称、提供者、模型类型等信息
     * @return 构建完成的LangChain4j模型实例
     */
    public LangChain4jModel buildModel(Langchain4jDataSourceInfo info) {
        // 创建属性映射表，用于存储模型配置参数
        Map<String, Object> properties = new HashMap<>();
        String baseUrl = info.getBaseUrl();
        baseUrl = MagicAPIConfigUtils.getLangchain4jConfig(info.getKey(), "baseUrl", baseUrl);
        String apiKey = info.getApiKey();
        apiKey = MagicAPIConfigUtils.getLangchain4jConfig(info.getKey(), "apiKey", apiKey);
        String modelName = info.getModelName();
        modelName = MagicAPIConfigUtils.getLangchain4jConfig(info.getKey(), "modelName", modelName);

        // 如果存在其他配置参数，将其解析并添加到属性映射表中
        String others = info.getOther();
        others = MagicAPIConfigUtils.getLangchain4jConfig(info.getKey(), "other", others);
        if (StringUtils.isNotBlank(others)) {
            properties.putAll(JSON.parseObject(others, new TypeReference<Map<String, Object>>() {}));
        }

        // 将基础配置参数添加到属性映射表中
        properties.put("baseUrl", baseUrl);
        properties.put("apiKey", apiKey);
        properties.put("modelName", modelName);

        // 使用工厂方法创建模型实例
        LangChain4jModel model = LangChain4jModelFactory.createModel(LangChain4jModel.Provider.valueOf(info.getProvider()),
                LangChain4jModel.ModelType.valueOf(info.getModelType()), properties);
        return model;
    }


    /**
     * 创建客户端并初始化模型配置
     * @param info 数据源信息对象，包含客户端配置参数
     */
    public void createClient(Langchain4jDataSourceInfo info) {
        // 构建语言模型实例
        LangChain4jModel model = buildModel(info);
        // 将模型按key和模型类型存储到模型映射表中
        this.modelMap.put(info.getKey(), new HashMap<String, LangChain4jModel>(){{
            put(info.getModelType(), model);
        }});
        // 存储数据源信息到信息映射表中
        this.infoMap.put(info.getKey(), info);
    }


    /**
     * 根据指定的键获取对应的模型对象
     *
     * @param key 模型的唯一标识符
     * @return 返回与指定键关联的LangChain4jModel对象，如果未找到则返回null
     */
    public Map<String, LangChain4jModel> getModel(String key) {
        return this.modelMap.get(key);
    }

    /**
     * 销毁LangChain4j模型客户端资源
     *
     * @param client LangChain4j模型客户端实例，可能为null
     */
    public void destroyModel(LangChain4jModel client) {
        if (client != null) {
            try {
                //client.close();
            } catch (Exception e) {
                log.warn("关闭LangChain4jModel客户端时发生异常", e);
            }
        }
    }

    /**
     * 获取所有数据源节点信息
     *
     * @return 数据源信息列表，包含动态创建的数据源和默认数据源
     */
    public List<Langchain4jDataSourceInfo> datasourceNodes() {
        // 获取所有动态创建的数据源信息
        List<Langchain4jDataSourceInfo>  dataSourceInfoList = new ArrayList<>(infoMap.values());
        try {
            // 获取Spring容器中所有相关的模型Bean
            Map<String, ImageModel> imageModelMap = SpringUtils.getBeansOfType(ImageModel.class);
            Map<String, EmbeddingModel> embeddingModelMap = SpringUtils.getBeansOfType(EmbeddingModel.class);
            Map<String, LanguageModel> languageModelMap = SpringUtils.getBeansOfType(LanguageModel.class);
            Map<String, StreamingLanguageModel> streamingLanguageModelMap = SpringUtils.getBeansOfType(StreamingLanguageModel.class);
            Map<String, ChatLanguageModel> chatLanguageModelMap = SpringUtils.getBeansOfType(ChatLanguageModel.class);
            Map<String, StreamingChatLanguageModel> streamingChatLanguageModelMap = SpringUtils.getBeansOfType(StreamingChatLanguageModel.class);

            // 如果存在任意一种模型Bean，则添加默认数据源
            if (MapUtils.isNotEmpty(imageModelMap) ||
                    MapUtils.isNotEmpty(embeddingModelMap) ||
                    MapUtils.isNotEmpty(languageModelMap) ||
                    MapUtils.isNotEmpty(streamingLanguageModelMap) ||
                    MapUtils.isNotEmpty(chatLanguageModelMap) ||
                    MapUtils.isNotEmpty(streamingChatLanguageModelMap)) {
                Langchain4jDataSourceInfo dataSourceInfo = new Langchain4jDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

    /**
     * 根据指定的key销毁对应的模型客户端
     *
     * @param key 模型的唯一标识符，用于定位需要销毁的模型客户端
     */
    public void destroyModelByKey(String  key) {
        if (key == null) {
            return;
        }
        try {
            // 获取并销毁指定key对应的所有模型客户端
            Map<String, LangChain4jModel> map = this.modelMap.get(key);
            if (map != null) {
                for (LangChain4jModel client : map.values()) {
                    try {
                        destroyModel(client);
                    } catch (Exception e) {
                        log.warn("销毁LangChain4jModel客户端时发生异常，key: {}", key, e);
                    }
                }
                this.modelMap.remove(key);
            }
            // 同时从infoMap中移除对应的数据源信息
            this.infoMap.remove(key);
        } catch (Exception e) {
            log.warn("销毁LangChain4jModel客户端时发生异常，key: {}", key, e);
        }
    }


        /**
     * 销毁方法，用于清理和释放LangChain4jModel客户端资源
     * <p>
     * 该方法会遍历所有已注册的模型，逐一销毁它们，然后清空内部的映射集合。
     * 在销毁过程中如果发生异常，会记录警告日志但不会中断整个销毁流程。
     * </p>
     *
     * @throws Exception 如果在销毁过程中发生无法处理的异常
     */
    @Override
    public void destroy() throws Exception {
        // 遍历并销毁所有模型客户端
        for (String key : new ArrayList<>(this.modelMap.keySet())) {
            try {
                destroyModelByKey(key);
            } catch (Exception e) {
                log.warn("关闭LangChain4jModel客户端时发生异常，key: {}", key, e);
            }
        }

        // 清空clientMap和infoMap
        try {
            this.modelMap.clear();
            this.infoMap.clear();
        } catch (Exception e) {
            log.warn("清理LangChain4jModel客户端映射时发生异常", e);
        }
    }


}
