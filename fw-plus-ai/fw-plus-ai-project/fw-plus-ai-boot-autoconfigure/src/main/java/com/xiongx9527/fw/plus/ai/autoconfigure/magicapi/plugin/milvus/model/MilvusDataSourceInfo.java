package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class MilvusDataSourceInfo extends MagicEntity {
    /**
     * 数据源key
     */
    private String key;

    /**
     * 连接地址
     */
    private String uri;

    /**
     * token
     */
    private String token;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 数据库名称
     */
    private String dbName;

    /**
     * 其它配置
     */
    private String other;

    /**
     * 创建数据源信息的简单副本
     * <p>
     * 该方法通过创建新的MilvusDataSourceInfo对象，并将当前对象的基本信息
     * （包括key、uri、token、username、password、dbName、other等属性）
     * 复制到新对象中，实现数据源信息的简单复制功能。
     * </p>
     *
     * @return MagicEntity 返回包含当前配置信息的MilvusDataSourceInfo对象
     */
    @Override
    public MagicEntity simple() {
        MilvusDataSourceInfo dataSourceInfo = new MilvusDataSourceInfo();
        super.simple(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setUri(this.uri);
        dataSourceInfo.setToken(this.token);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setDbName(this.dbName);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

    /**
     * 创建数据源信息的深度副本
     * <p>
     * 该方法通过创建新的MilvusDataSourceInfo对象，并将当前对象的完整信息
     * （包括key、uri、token、username、password、dbName、other等属性）
     * 复制到新对象中，实现数据源信息的完整复制功能。
     * </p>
     *
     * @return MagicEntity 返回包含当前完整配置信息的MilvusDataSourceInfo对象
     */
    @Override
    public MagicEntity copy() {
        MilvusDataSourceInfo dataSourceInfo = new MilvusDataSourceInfo();
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setUri(this.uri);
        dataSourceInfo.setToken(this.token);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setDbName(this.dbName);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

}
