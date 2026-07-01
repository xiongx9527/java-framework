package com.xiongx9527.fw.plus.common.validator;

import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 线程局部变量持有者
 */
public class CommonsValidatorHolder {

    /**
     * 上下文持有者，用于存储当前线程的上下文信息
     * 使用ThreadLocal确保每个线程都有独立的上下文副本
     * 类型为Map<String,Object>，可以存储键值对形式的上下文数据
     */
    private static final ThreadLocal<Map<String,Object>> CONTEXT_HOLDER;

    /**
     * 静态初始化块，用于初始化线程本地变量持有器
     *
     * 该初始化块会尝试创建TransmittableThreadLocal实例以支持线程池中的上下文传递，
     * 如果失败则回退到使用InheritableThreadLocal
     *
     * 注意：此代码块不接受参数且无返回值
     */
    static {
        ThreadLocal<Map<String,Object>> holder;
        try {
            // 尝试使用阿里巴巴的TransmittableThreadLocal来支持线程池上下文传递
            holder = (ThreadLocal)Class.forName("com.alibaba.ttl.TransmittableThreadLocal").newInstance();
        } catch (Throwable var2) {
            // 如果TransmittableThreadLocal不可用，则使用标准的InheritableThreadLocal
            holder = new InheritableThreadLocal();
        }

        CONTEXT_HOLDER = holder;
    }

    /**
     * 私有构造函数，用于创建CommonsValidatorHolder实例
     * 此构造函数被声明为私有，防止外部直接实例化该类
     */
    private CommonsValidatorHolder(){}


    /**
     * 获取当前线程关联的上下文对象
     * 此方法用于从ThreadLocal变量CONTEXT_HOLDER中获取存储的上下文对象
     * 主要用于跨方法调用或跨类访问时保持上下文信息的完整性
     *
     * @return Object 返回存储的上下文对象，需要调用者自行转换类型
     */
    public static Map<String,Object> get() {
        Map<String,Object> map = CONTEXT_HOLDER.get();
        if (ObjectUtils.isEmpty(map)) {
            map = new HashMap<>();
            set(map);
        }
        return map;
    }


    /**
     * 根据键获取值
     * 该方法旨在从一个映射中根据给定的键获取对应的值如果映射为空，则返回一个空映射以避免空指针异常
     *
     * @param key 要获取值的键，不能为空
     * @return 对应键的值如果键不存在或映射为空，则返回null
     */
    public static Object get(String key) {
        Map<String,Object> map = get();
        // 根据键从映射中获取值
        return map.get(key);
    }

    /**
     * 设置当前线程关联的上下文对象
     * @param map
     */
    public static void set(Map<String,Object> map) {
        // 检查提供的表单对象是否为空
        if (ObjectUtils.isEmpty(map)) {
            // 如果表单对象为空，从上下文持有者中移除当前表单对象
            CONTEXT_HOLDER.remove();
        } else {
            // 如果表单对象不为空，将其设置到上下文持有者中
            CONTEXT_HOLDER.set(map);
        }
    }

    /**
     * 设置当前线程关联的上下文对象
     * @param key
     * @param value
     */
    public static void set(String key, Object value) {
        Map<String,Object> map = get();
        map.put(key, value);
        set(map);
    }

    /**
     * 清除当前线程上下文中的对象
     * 此方法用于移除线程局部变量CONTEXT_HOLDER中的对象，以避免内存泄漏或数据污染
     * 主要用于在请求结束或必要时清理资源
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }


}
