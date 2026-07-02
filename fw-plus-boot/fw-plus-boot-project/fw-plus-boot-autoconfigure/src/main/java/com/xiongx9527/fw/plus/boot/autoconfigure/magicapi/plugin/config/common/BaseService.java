package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.model.ConfigInfo;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

/**
 * 基础服务接口，提供通用的字符串处理方法
 */
public interface BaseService extends MagicBaseService {

    /**
     * 验证配置信息的必填字段
     * 
     * @param entity 配置信息对象
     */
    default void validateField(ConfigInfo entity) {
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getAppId(), "appId");
        validateRequiredField(entity.getEnv(), "env");
        validateRequiredField(entity.getNamespace(), "namespace");
    }


    /**
     * 验证YAML格式字符串的有效性
     * 
     * @param yamlStr 需要验证的YAML格式字符串
     * @throws IllegalArgumentException 当YAML格式不正确时抛出
     */
    default void validateFormat(String yamlStr) {
        if (StringUtils.isNotBlank(yamlStr)  && yamlStr.trim().length() > 0) {
            try {
                // 创建YAML解析器对象
                Yaml yaml = new Yaml();
                // 使用YAML解析器解析脚本内容，并返回一个Map对象
                this.flattenYamlMap(yaml.load(yamlStr));
            } catch (Exception e) {
                // 如果解析失败，则抛出异常
                throw new IllegalArgumentException("配置文件格式错误",e);
            }
        }
    }



    /**
     * 移除字符串中的所有下划线字符
     *
     * @param input 待处理的输入字符串，可能包含下划线
     * @return 处理后的字符串，如果输入为null则返回空字符串
     */
    default String removeUnderscores(String input) {
        return input != null ? input.replace("_", "") : "";
    }

    /**
     * 创建属性名称路径，将组路径和路径组合成标准格式
     *
     * @param groupPath 组路径，表示属性所属的分组
     * @param path 属性路径，表示具体的属性名称
     * @return 组合后的完整路径，格式为"groupPath/path"
     */
    default String createPropName(String groupPath, String path) {
        return String.format("%s/%s", groupPath, path);
    }

    /**
     * 扁平化处理YAML格式的Map结构，将嵌套的Map转换为点分隔的键值对
     *
     * <p>该方法通过递归遍历嵌套的Map结构，将所有层级的键用点号连接起来，
     * 生成一个单层结构的Map。例如将{"a": {"b": 1}}转换为{"a.b": 1}。</p>
     *
     * @param yamlMap 输入的YAML格式Map，可能包含多层嵌套结构
     *                要求Map的键为String类型，值可以是任意Object或嵌套Map
     * @return 扁平化后的Map，所有嵌套键都会被转换为点分隔的格式
     *         返回的Map中不会包含任何嵌套结构，所有值都是基本类型或非Map对象
     *
     * @throws IllegalArgumentException 如果输入参数为null
     * @throws ClassCastException 如果嵌套结构中包含非String类型的Map键
     *
     * 示例：
     * 输入: {"server": {"port": 8080, "ssl": {"enabled": true}}}
     * 输出: {"server.port": 8080, "server.ssl.enabled": true}
     */
    default Map<String, Object> flattenYamlMap(Map<String, Object> yamlMap) {
        // 创建结果Map并预先估算大小
        Map<String, Object> propMap = new HashMap<>(estimateSize(MapUtils.emptyIfNull(yamlMap)));

        // 处理扁平化
        flattenMap("", MapUtils.emptyIfNull(yamlMap), propMap);
        return propMap;
    }

    /**
     * 递归处理Map的扁平化
     */
    default void flattenMap(String prefix, Map<String, Object> sourceMap, Map<String, Object> resultMap) {
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String newKey = prefix.isEmpty() ? key : prefix + "." + key;

            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> childMap = (Map<String, Object>) value;
                flattenMap(newKey, childMap, resultMap);
            } else {
                resultMap.put(newKey, value);
            }
        }
    }

    /**
     * 估算结果Map的大致容量，减少扩容次数
     */
    default int estimateSize(Map<String, Object> map) {
        int size = map.size();
        for (Object value : map.values()) {
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> childMap = (Map<String, Object>) value;
                size += estimateSize(childMap);
            }
        }
        return size;
    }

}
