package com.xiongx9527.fw.plus.common.databind;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xiongxiao
 */
public class RequestDataTransfer {
    /**
     * 线程本地变量，用于存储请求数据
     */
    private static final ThreadLocal<Map<String, Object>> REQUEST_DATA = new ThreadLocal();
    /**
     * 跳过敏感信息的字段名
     */
    protected static String SKIP_SENSITIVE = "skip_sensitive";

    /**
     * RequestDataTransfer构造函数
     *
     * 无参构造函数，用于创建RequestDataTransfer对象实例
     */
    public RequestDataTransfer() {
    }

    /**
     * 将请求数据存储到线程本地变量中
     *
     * @param requestData 请求数据的键值对集合，其中键是字符串类型，值可以是任意类型
     */
    public static void put(Map<String, Object> requestData) {
        REQUEST_DATA.set(requestData);
    }

    /**
     * 将键值对存储到数据映射中
     * 如果数据映射为空，则创建一个新的映射并存储键值对
     *
     * @param key 存储的键，用于后续检索值
     * @param value 存储的值，可以是任何对象
     */
    public static void put(final String key, final Object value) {
        // 获取当前的数据映射
        Map<String, Object> dataMap = getAll();
        // 如果数据映射非空，则直接在当前映射中存储键值对
        if (null != dataMap && !dataMap.isEmpty()) {
            dataMap.put(key, value);
        } else {
            // 如果数据映射为空，则创建一个新的映射，并在其中存储键值对
            put(new HashMap<String, Object>(16) {
                {
                    this.put(key, value);
                }
            });
        }
    }

    /**
     * 根据参数名从全局数据映射中获取对应的值
     * 此方法提供了一种类型安全的方式，从全局数据映射中根据参数名提取相应的值
     * 它首先尝试从全局数据映射中获取值，如果映射为空或不包含该参数，则返回null
     * 这种设计允许调用者根据需要获取任何类型的值，前提是该值在全局映射中存在且类型匹配
     *
     * @param param 参数名，用于在全局数据映射中查找对应的值
     * @param <T>   泛型参数，表示调用者期望获取的值的类型
     * @return      返回指定类型的值，如果找不到或映射为空，则返回null
     */
    public static <T> T get(String param) {
        // 获取全局数据映射
        Map<String, Object> dataMap = getAll();
        // 根据条件判断返回相应的值或null
        return (T)(null != dataMap && !dataMap.isEmpty() ? dataMap.get(param) : null);
    }

    /**
     * 获取当前请求的所有数据
     *
     * 此方法用于检索存储在当前请求范围内的所有数据它返回一个映射，其中包含所有已存储的数据键值对
     *
     * @return Map<String, Object> 包含所有请求数据的映射
     */
    public static Map<String, Object> getAll() {
        return (Map)REQUEST_DATA.get();
    }

    /**
     * 移除请求数据
     *
     * 此方法用于清除不再需要的请求数据，以释放资源或避免潜在的数据混淆
     * 它直接调用了REQUEST_DATA对象的remove方法，其中REQUEST_DATA被假定为一个全局变量或静态变量
     * 该方法没有输入参数，也不返回任何值
     */
    public static void remove() {
        REQUEST_DATA.remove();
    }

    /**
     * 跳过所有敏感信息的函数
     * 此函数通过调用重载函数skipSensitive，使用"_all_"作为参数，来实现跳过所有敏感信息的功能
     * 之所以这样做，是为了提供一个简单的方法来处理所有类型的敏感信息，而不需要指定具体的敏感信息类型
     */
    public static void skipSensitive() {
        skipSensitive("_all_");
    }

    /**
     * 设置跳过敏感信息的字段名
     *
     * 此方法用于指定在数据处理过程中哪些字段应被视为敏感信息，以便在输出或日志中跳过这些字段
     * 这对于保护用户隐私和敏感数据安全非常重要
     *
     * @param fieldNames 敏感字段名的字符串，多个字段名之间用逗号分隔
     */
    public static void skipSensitive(String fieldNames) {
        put(SKIP_SENSITIVE, fieldNames);
    }


    /**
     * 获取跳过敏感信息的配置值
     *
     * 该方法用于从配置系统中获取一个特定的值，该值指示是否应跳过敏感信息的处理
     * 敏感信息可能包括个人隐私数据、公司机密等需要特别处理的数据
     *
     * @return String 表示是否跳过敏感信息处理的配置值如果配置不存在，则返回null
     */
    public static String getSkipSensitive() {
        return get(SKIP_SENSITIVE);
    }

	/**
	 * 获取跳过敏感信息的配置项
	 *
	 * 此方法用于获取一个静态变量SKIP_SENSITIVE，该变量指示是否跳过敏感信息的处理
	 * 主要用于配置解析和数据处理过程中，根据实际需求决定是否忽略敏感信息
	 *
	 * @return 返回SKIP_SENSITIVE变量的值，true表示跳过敏感信息，false表示不跳过
	 */
	public static String getSkipSensitiveKey(){
		return SKIP_SENSITIVE;
	}
}