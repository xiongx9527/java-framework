package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class InfluxDataSourceInfo extends MagicEntity {

    /**
     * 私有成员变量key，用于存储密钥信息
     */
    private String key;

    /** InfluxDB服务器地址 */
    private String url;

    /** 日志级别，默认为"NONE" */
    private String logLevel = "NONE";

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 访问令牌 */
    private String token;

    /** 组织名称 */
    private String org;

    /** 存储桶名称 */
    private String bucket;

    /**
     * 私有成员变量other，用于存储其他相关信息
     */
    private Map<String, Object> properties = new LinkedHashMap<String, Object>();

    @Override
    public MagicEntity simple() {
        // 创建Nebula数据源信息对象
        InfluxDataSourceInfo dataSourceInfo = new InfluxDataSourceInfo();
        super.simple(dataSourceInfo);
        // 设置数据源基本信息
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setUrl(url);
        dataSourceInfo.setLogLevel(logLevel);
        dataSourceInfo.setUsername(username);
        dataSourceInfo.setPassword(password);
        dataSourceInfo.setToken(token);
        dataSourceInfo.setOrg(org);
        dataSourceInfo.setBucket(bucket);
        dataSourceInfo.setProperties(copyProperties());
        return dataSourceInfo;
    }

    @Override
    public MagicEntity copy() {
        // 创建新的数据源信息对象实例
        InfluxDataSourceInfo dataSourceInfo = new InfluxDataSourceInfo();
        // 复制父类中的通用属性
        super.copyTo(dataSourceInfo);
        // 设置当前类特有的属性值
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setUrl(url);
        dataSourceInfo.setLogLevel(logLevel);
        dataSourceInfo.setUsername(username);
        dataSourceInfo.setPassword(password);
        dataSourceInfo.setToken(token);
        dataSourceInfo.setOrg(org);
        dataSourceInfo.setBucket(bucket);
        dataSourceInfo.setProperties(copyProperties());
        return dataSourceInfo;
    }

    private Map<String, Object> copyProperties() {
        return this.properties == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(this.properties);
    }

}
