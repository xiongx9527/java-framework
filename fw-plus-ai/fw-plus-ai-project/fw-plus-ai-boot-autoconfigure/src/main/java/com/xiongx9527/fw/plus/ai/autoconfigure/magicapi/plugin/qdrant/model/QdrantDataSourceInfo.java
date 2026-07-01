package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class QdrantDataSourceInfo extends MagicEntity {
    /**
     * 数据源key
     */
    private String key;

    /**
     * 数据源host
     */
    private String host;

    /**
     * 数据源端口
     */
    private String port;

    /**
     * 数据源是否使用安全传输层
     */
    private  boolean utls;

    /**
     * 数据源apiKey
     */
    private String apiKey;

    /**
     * 其它配置
     */
    private String other;

    @Override
    public MagicEntity simple() {
        QdrantDataSourceInfo dataSourceInfo = new QdrantDataSourceInfo();
        super.simple(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHost(this.host);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setUtls(this.utls);
        dataSourceInfo.setApiKey(this.apiKey);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

    @Override
    public MagicEntity copy() {
        QdrantDataSourceInfo dataSourceInfo = new QdrantDataSourceInfo();
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHost(this.host);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setUtls(this.utls);
        dataSourceInfo.setApiKey(this.apiKey);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }
}
