package com.xiongx9527.fw.plus.mp.encrypt;

import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mp.encrypt.config.EncryptorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.Properties;

/**
 * 字段解密拦截器
 */
@Slf4j
@Intercepts({@Signature(
        type = ResultSetHandler.class,
        method = "handleResultSets",
        args = {Statement.class}
)})
public class FieldDecryptInterceptor implements Interceptor  {
    /**
     * 加密器实例，用于执行具体的加密操作
     */
    private FieldEncrypt.IEncryptor encryptor;

    /**
     * 加密器配置属性，包含加密相关的配置信息
     */
    private EncryptorProperties encryptorProperties;


    /**
     * 构造函数，初始化字段解密拦截器
     *
     * @param encryptor 加密器实例，用于执行具体的加解密操作
     * @param encryptorProperties 加密器配置属性，包含加密相关的配置信息
     */
    public FieldDecryptInterceptor(FieldEncrypt.IEncryptor encryptor, EncryptorProperties encryptorProperties) {
        this.encryptor = encryptor;
        this.encryptorProperties = encryptorProperties;
    }


    /**
     * 拦截方法调用，主要用于字段解密
     * 此方法通过使用FieldEncryptorHandler来处理字段解密逻辑
     * 它利用了FieldDecryptor的processField方法来对特定字段进行解密处理
     *
     * @param invocation 被拦截的调用对象，包含被拦截方法的信息和参数
     * @return Object 返回经过解密处理后的结果
     * @throws Throwable 如果在拦截和解密过程中发生任何错误，抛出此异常
     */
    public Object intercept(Invocation invocation) throws Throwable {
        return FieldEncryptorHandler.handleDecrypt(invocation, (metaObject, fieldSetProperty)->{FieldDecryptor.processField(encryptor, encryptorProperties, metaObject, fieldSetProperty);});
    }

    /**
     * 插装方法，用于动态地给ResultSetHandler类型的对象添加插装功能
     * 此方法主要用于拦截和增强那些处理结果集的对象，通过插装，可以在不修改原有对象逻辑的情况下，添加额外的功能
     *
     * @param object 待插装的对象，该对象可能是ResultSetHandler类型，也可能不是
     * @return 如果对象是ResultSetHandler类型，则返回插装后的对象；否则，原样返回
     */
    public Object plugin(Object object) {
        // 判断传入的对象是否为ResultSetHandler类型，是则进行插装，否则直接返回
        return object instanceof ResultSetHandler ? Plugin.wrap(object, this) : object;
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
