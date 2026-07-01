package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jDataSourceInfo extends MagicEntity {

    /**
     * 数据源key
     */
    private String key;
    /**
     * 基础URL地址，用于构建API请求的完整路径
     */
    private String baseUrl;

    /**
     * API密钥，用于身份验证和访问权限控制
     */
    private String apiKey;

    /**
     * 模型名称，标识具体使用的AI模型
     */
    private String modelName;

    /**
     * 提供商信息，标识模型的服务提供商
     */
    private String provider;

    /**
     * 模型类型，区分不同类型的AI模型（如文本生成、图像识别等）
     */
    private String modelType;

    /**
     * 其它配置
     */
    private String other;

    /**
     * 创建一个简单的数据源信息对象副本
     * <p>
     * 该方法创建一个新的Langchain4jDataSourceInfo实例，并将当前对象的基本信息复制到新实例中。
     * 主要用于快速创建具有相同配置的数据源信息对象。
     * </p>
     *
     * @return MagicEntity 返回配置好的数据源信息对象
     */
    @Override
    public MagicEntity simple() {
        Langchain4jDataSourceInfo dataSourceInfo = new Langchain4jDataSourceInfo();
        super.simple(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setBaseUrl(this.baseUrl);
        dataSourceInfo.setModelName(this.modelName);
        dataSourceInfo.setApiKey(this.apiKey);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

    /**
     * 创建一个完整的数据源信息对象副本
     * <p>
     * 该方法创建一个新的Langchain4jDataSourceInfo实例，并将当前对象的所有信息深度复制到新实例中。
     * 相比于simple方法，此方法可能包含更完整的对象状态复制逻辑。
     * </p>
     *
     * @return MagicEntity 返回完整配置的数据源信息对象
     */
    @Override
    public MagicEntity copy() {
        Langchain4jDataSourceInfo dataSourceInfo = new Langchain4jDataSourceInfo();
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setBaseUrl(this.baseUrl);
        dataSourceInfo.setModelName(this.modelName);
        dataSourceInfo.setApiKey(this.apiKey);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

}
