package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class WeaviateDataSourceInfo extends MagicEntity {

    /**
     * 数据源key
     */
    private String key;

    /**
     *  数据源协议
     */
    private String scheme;

    /**
     * 数据源地址
     */
    private String host;

    /**
     * 连接超时
     */
    private String connectionTimeout = "60";

    /**
     * 请求超时
     */
    private String connectionRequestTimeout = "60";

    /**
     * 读取超时
     */
    private String socketTimeout = "60";

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 其它配置
     */
    private String other;

    /**
     * 创建并配置Weaviate数据源信息对象
     *
     * @return 配置完成的MagicEntity对象，实际类型为WeaviateDataSourceInfo
     */
    @Override
    public MagicEntity simple() {
        // 创建Weaviate数据源信息对象
        WeaviateDataSourceInfo dataSourceInfo = new WeaviateDataSourceInfo();

        // 调用父类的simple方法进行基础配置
        super.simple(dataSourceInfo);

        // 设置Weaviate连接相关配置信息
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHost(this.host);
        dataSourceInfo.setScheme(this.scheme);
        dataSourceInfo.setConnectionTimeout(this.connectionTimeout);
        dataSourceInfo.setConnectionRequestTimeout(this.connectionRequestTimeout);
        dataSourceInfo.setSocketTimeout(this.socketTimeout);
        dataSourceInfo.setApiKey(this.apiKey);
        dataSourceInfo.setOther(this.other);

        return dataSourceInfo;
    }

    /**
     * 创建当前数据源信息对象的深拷贝副本
     *
     * @return 返回一个新的WeaviateDataSourceInfo对象，包含与当前对象相同的所有属性值
     */
    @Override
    public MagicEntity copy() {
        // 创建新的数据源信息对象实例
        WeaviateDataSourceInfo dataSourceInfo = new WeaviateDataSourceInfo();

        // 拷贝父类中的通用属性
        super.copyTo(dataSourceInfo);

        // 设置Weaviate特有的连接配置属性
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setHost(this.host);
        dataSourceInfo.setScheme(this.scheme);
        dataSourceInfo.setConnectionTimeout(this.connectionTimeout);
        dataSourceInfo.setConnectionRequestTimeout(this.connectionRequestTimeout);
        dataSourceInfo.setSocketTimeout(this.socketTimeout);
        dataSourceInfo.setApiKey(this.apiKey);
        dataSourceInfo.setOther(this.other);

        return dataSourceInfo;
    }

}
