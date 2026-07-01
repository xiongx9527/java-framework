package com.xx9527.fw.plus.mp.encrypt;


import cn.hutool.core.util.StrUtil;
import com.xx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xx9527.fw.plus.mp.encrypt.config.EncryptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * 字段加密处理器
 */
@Slf4j
public class FieldEncryptorHandler {
    /**
     * 加密器缓存映射表
     *
     * 用于存储不同加密器实现类与其实例的映射关系，避免重复创建相同类型的加密器实例。
     * key: 加密器实现类的Class对象
     * value: 对应的加密器实例
     *
     * 初始化容量为16，适用于大多数场景下的加密器数量
     */
    private static Map<Class<? extends FieldEncrypt.IEncryptor>, FieldEncrypt.IEncryptor> encryptorCache = new HashMap<>(16);


    /**
     * 私有构造函数，用于创建FieldEncryptorHandler实例
     * 此构造函数不接受任何参数，也不返回任何值
     * 由于是私有构造函数，只能在类内部调用
     */
    private FieldEncryptorHandler() {
    }


    /**
     * 处理加密操作，对传入的参数进行字段加密
     *
     * @param invocation 调用信息，包含方法和参数
     * @param properties 加密属性配置
     * @param encryptor 加密器接口实现
     * @return 执行调用的结果
     * @throws Throwable 可能抛出的任何异常
     */
    public static Object handleEncrypt(Invocation invocation, EncryptorProperties properties, FieldEncrypt.IEncryptor encryptor) throws Throwable {
        try {
            // 获取调用参数
            Object[] args = invocation.getArgs();
            // 获取映射语句对象
            MappedStatement mappedStatement = (MappedStatement) args[0];
            // 检查是否支持的SQL命令类型
            if (isSupportedSqlCommand(mappedStatement.getSqlCommandType())) {
                // 获取参数对象
                Object parameter = args[1];
                // 获取配置对象
                Configuration configuration = mappedStatement.getConfiguration();
                // 如果参数是一个Map，遍历Map的每个条目
                if (parameter instanceof Map) {
                    for (Map.Entry<String, Object> entry : ((Map<String, Object>) parameter).entrySet()) {
                        Object value = entry.getValue();
                        // 对非空值进行加密处理
                        if (value != null) {
                            // 如果值是一个ArrayList，对每个项进行加密处理
                            if (value instanceof ArrayList) {
                                // 只处理以"param"开头的键
                                if (!entry.getKey().startsWith("param")) {
                                    for (Object item : (ArrayList<?>) value) {
                                        encryptFields(configuration, encryptor, properties, item);
                                    }
                                }
                            } else if (encryptFields(configuration, encryptor, properties, value)) {
                                // 如果加密成功，终止循环
                                break;
                            }
                        }
                    }
                } else {
                    // 如果参数不是Map，直接进行加密处理
                    encryptFields(configuration, encryptor, properties, parameter);
                }
            }
        } catch (Throwable e) {
            // 记录加密器初始化失败的错误信息
            log.error("Failed to initialize field encryptor: {}", e.getMessage(), e);
        } finally {
            // 继续执行原始调用
            return invocation.proceed();
        }
    }

    /**
     * 加密指定对象中的敏感字段
     *
     * @param configuration 配置信息对象，用于获取全局配置
     * @param encryptor 外部传入的加密器，如果未指定，则使用默认加密器
     * @param properties 加密属性配置，包含算法、盐值等信息
     * @param target 需要进行字段加密的目标对象
     * @return 加密操作是否成功
     */
    public static boolean encryptFields(Configuration configuration, FieldEncrypt.IEncryptor encryptor, EncryptorProperties properties, Object target) {
        // 使用FieldProcessor处理对象中的所有字段，对符合加密条件的字段进行加密
        return FieldProcessor.processFields(configuration, target, (metaObject, fieldSetProperty) -> {
            // 获取字段的加密注解信息
            FieldEncrypt fieldEncrypt = fieldSetProperty.getFieldEncrypt();
            // 如果字段上有加密注解，则进行加密处理
            if (fieldEncrypt != null && fieldEncrypt.executeEncrypt()) {
                // 获取字段的当前值
                Object fieldValue = metaObject.getValue(fieldSetProperty.getFieldName());
                // 对非空且非空字符串的字段值进行加密
                if (fieldValue != null && !Objects.equals(StrUtil.EMPTY, fieldValue)) {
                    try {
                        // 获取并调用合适的加密器对字段值进行加密
                        String encryptedValue = getEncryptor(encryptor, fieldEncrypt.encryptor())
                                .encrypt(properties.algorithm(fieldEncrypt), properties.getPrivateKey(), properties.getIv(),
                                        properties.salt(fieldEncrypt), (String) fieldValue);
                        // 将加密后的值设置回原字段
                        metaObject.setValue(fieldSetProperty.getFieldName(), encryptedValue);
                    } catch (Throwable e) {
                        // 如果加密过程中发生异常，记录错误日志
                        log.error("Field encryption failed: {}", e.getMessage(), e);
                    }
                }
            }
        });
    }

    /**
     * 获取加密器实例
     * 如果指定的加密器类不是默认加密器类，则尝试从缓存中获取对应的加密器实例
     * 如果缓存中不存在该实例，则尝试创建一个新的实例并存入缓存中
     *
     * @param defaultEncryptor 默认的加密器实例，当指定的加密器类为FieldEncrypt.IEncryptor时返回
     * @param encryptorClass 指定的加密器类，用于创建具体的加密器实例
     * @return 返回指定的或默认的加密器实例
     */
    protected static FieldEncrypt.IEncryptor getEncryptor(FieldEncrypt.IEncryptor defaultEncryptor, Class<? extends FieldEncrypt.IEncryptor> encryptorClass) {
        // 检查是否使用默认加密器
        if (FieldEncrypt.IEncryptor.class != encryptorClass) {
            // 尝试从缓存中获取加密器实例
            FieldEncrypt.IEncryptor cachedEncryptor = encryptorCache.get(encryptorClass);
            if (cachedEncryptor == null) {
                try {
                    // 如果缓存中不存在，创建一个新的实例并存入缓存
                    cachedEncryptor = encryptorClass.getDeclaredConstructor().newInstance();
                    encryptorCache.put(encryptorClass, cachedEncryptor);
                } catch (Throwable e) {
                    // 记录创建实例时的错误信息
                    log.error("Failed to instantiate encryptor: {}", e.getMessage(), e);
                }
            }
            return cachedEncryptor;
        }
        // 如果指定的加密器类为FieldEncrypt.IEncryptor，则返回默认加密器实例
        return defaultEncryptor;
    }

    /**
     * 处理解密逻辑的通用方法
     * 该方法拦截查询结果进行解密处理，主要针对查询结果中的加密字段
     * 使用invocation进行链路调用，确保在获取结果后进行解密处理
     *
     * @param invocation 调用对象，包含调用的目标方法和参数
     * @param fieldProcessor 字段处理器，用于处理字段解密逻辑
     * @return 解密后的查询结果
     * @throws Throwable 转发异常，如果调用链路或解密过程中出现异常
     */
    public static Object handleDecrypt(Invocation invocation, BiConsumer<MetaObject, FieldSetProperty> fieldProcessor) throws Throwable {
        // 执行原始方法并获取结果，确保解密处理是在获取查询结果之后
        List<?> result = (List<?>) invocation.proceed();
        try {
            if (CollectionUtils.isNotEmpty(result)) {
                // 获取调用的目标对象，判断是否为DefaultResultSetHandler实例
                Object target = invocation.getTarget();
                if (target instanceof DefaultResultSetHandler) {
                    Configuration configuration = getConfiguration((DefaultResultSetHandler) target);
                    // 遍历查询结果，对每个对象进行解密处理
                    for (Object item : result) {
                        if (item != null && !FieldProcessor.processFields(configuration, item, fieldProcessor)) {
                            break;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            log.error("Field decryption failed: {}", e.getMessage(), e);
        } finally {
            return result;
        }
    }

    private static Configuration getConfiguration(DefaultResultSetHandler target) throws NoSuchFieldException, IllegalAccessException {
        DefaultResultSetHandler resultSetHandler = target;
        // 反射获取mappedStatement字段，用于获取配置信息
        Field mappedStatementField = resultSetHandler.getClass().getDeclaredField("mappedStatement");
        mappedStatementField.setAccessible(true);
        MappedStatement mappedStatement = (MappedStatement) mappedStatementField.get(resultSetHandler);
        Configuration configuration = mappedStatement.getConfiguration();
        return configuration;
    }

    /**
     * 判断是否是支持的SQL命令类型
     *
     * @param commandType SQL命令类型，用于判断是否被支持
     * @return 如果命令类型为INSERT或UPDATE，则返回true，表示被支持；否则返回false
     */
    private static boolean isSupportedSqlCommand(SqlCommandType commandType) {
        return commandType == SqlCommandType.INSERT || commandType == SqlCommandType.UPDATE;
    }
}