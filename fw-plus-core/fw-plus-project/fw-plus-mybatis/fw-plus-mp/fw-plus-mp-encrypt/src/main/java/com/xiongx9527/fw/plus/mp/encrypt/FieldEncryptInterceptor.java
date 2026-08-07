package com.xiongx9527.fw.plus.mp.encrypt;

import java.util.Properties;

import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mp.encrypt.config.EncryptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 字段加密拦截器
 */
@Slf4j
@Intercepts({@Signature(
    type = Executor.class,
    method = "update",
    args = {MappedStatement.class, Object.class}
)})
public class FieldEncryptInterceptor implements Interceptor {
    /**
     * 加密器实例，用于执行具体的加密操作
     */
    private FieldEncrypt.IEncryptor encryptor;

    /**
     * 加密器配置属性，包含加密相关的配置信息
     */
    private EncryptorProperties encryptorProperties;


    /**
     * 构造函数，初始化字段加密拦截器
     *
     * @param encryptor 加密器实例，用于执行具体的加密解密操作
     * @param encryptorProperties 加密器配置属性，包含加密相关的配置信息
     */
    public FieldEncryptInterceptor(FieldEncrypt.IEncryptor encryptor, EncryptorProperties encryptorProperties) {
        this.encryptor = encryptor;
        this.encryptorProperties = encryptorProperties;
    }


    /**
     * 拦截方法调用并进行字段加密处理
     *
     * 本方法通过使用FieldEncryptorHandler来处理加密逻辑，目的是在方法调用过程中
     * 对特定字段进行加密，以增强数据安全性
     *
     * @param invocation 方法调用对象，包含被拦截方法的信息和参数
     * @return Object 加密处理后的结果
     * @throws Throwable 如果加密处理过程中发生异常，则向上抛出
     */
    public Object intercept(Invocation invocation) throws Throwable {
        return FieldEncryptorHandler.handleEncrypt(invocation, encryptorProperties , encryptor);
    }

    /**
     * 插装方法，用于将对象转换为带有插装功能的对象
     * 此方法主要用于增强执行器（Executor）对象的功能，通过插装方式实现
     * 如果传入的对象不是Executor类型，则不进行任何处理，直接返回原对象
     *
     * @param object 待插装的对象
     * @return 如果对象是Executor类型，则返回插装后的对象；否则返回原对象
     */
    public Object plugin(Object object) {
        // 判断对象是否为Executor类型，是则进行插装，否则直接返回原对象
        return object instanceof Executor ? Plugin.wrap(object, this) : object;
    }

    /**
     * 设置属性
     *
     * 该方法用于接收一个Properties对象，并将其内容设置给当前实例
     * 主要用于配置对象，使其根据提供的属性进行初始化或更新
     *
     * @param properties 包含待设置属性的Properties对象不能为空
     */
    public void setProperties(Properties properties) {
        // 方法体为空，具体实现未提供
    }
}