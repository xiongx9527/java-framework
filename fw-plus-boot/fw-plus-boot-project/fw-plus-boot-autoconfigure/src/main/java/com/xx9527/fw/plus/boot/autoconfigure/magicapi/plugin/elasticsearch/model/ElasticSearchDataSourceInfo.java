package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;
@Data
@EqualsAndHashCode(callSuper = true)
public class ElasticSearchDataSourceInfo extends MagicEntity {

    /**
     * 数据源key
     */
    private String key;

    /**
     * 数据源协议
     */
    private String scheme = "http";

    /**
     * 数据源名称
     */
    private String hostname;

    /**
     * 数据源端口
     */
    private String port;

     /**
      * 数据源用户名
      */
    private String username;

    /**
     * 数据源密码
     */
    private String password;


    /**
     * 数据源路径前缀
     */
    private String pathPrefix;

    /**
     * 其他参数
     */
    private String other;

    @Override
    public MagicEntity simple() {
        ElasticSearchDataSourceInfo dataSourceInfo = new ElasticSearchDataSourceInfo();
        super.simple(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHostname(this.hostname);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setScheme(this.scheme);
        dataSourceInfo.setPathPrefix(this.pathPrefix);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

    @Override
    public MagicEntity copy() {
        ElasticSearchDataSourceInfo dataSourceInfo = new ElasticSearchDataSourceInfo();
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHostname(this.hostname);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setScheme(this.scheme);
        dataSourceInfo.setPathPrefix(this.pathPrefix);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

}
