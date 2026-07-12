package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class DtpDataSourceInfo extends MagicEntity {

    /**
     * 数据源key
     */
    private String key;
    /** 核心线程池大小 */
    private int corePoolSize = 1;
    /** 最大线程池大小 */
    private int maximumPoolSize = 10;
    /** 线程空闲存活时间 */
    private long keepAliveTime ;
    /** 队列类型 */
    private String queueType;
    /** 队列容量 */
    private int queueCapacity = 1000;
    /** 线程工厂 */
    private String threadFactory;
    /** 拒绝策略 */
    private String rejectedPolicy;

    /**
     * 其它配置
     */
    private String other;


    @Override
    public MagicEntity simple() {
        DtpDataSourceInfo dataSourceInfo = new DtpDataSourceInfo();

        // 调用父类的simple方法进行基础配置
        super.simple(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setCorePoolSize(this.corePoolSize);
        dataSourceInfo.setMaximumPoolSize(this.maximumPoolSize);
        dataSourceInfo.setKeepAliveTime(this.keepAliveTime);
        dataSourceInfo.setQueueType(this.queueType);
        dataSourceInfo.setQueueCapacity(this.queueCapacity);
        dataSourceInfo.setThreadFactory(this.threadFactory);
        dataSourceInfo.setRejectedPolicy(this.rejectedPolicy);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

    @Override
    public MagicEntity copy() {
        // 创建新的数据源信息对象实例
        DtpDataSourceInfo dataSourceInfo = new DtpDataSourceInfo();
        // 拷贝父类中的通用属性
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setCorePoolSize(this.corePoolSize);
        dataSourceInfo.setMaximumPoolSize(this.maximumPoolSize);
        dataSourceInfo.setKeepAliveTime(this.keepAliveTime);
        dataSourceInfo.setQueueType(this.queueType);
        dataSourceInfo.setQueueCapacity(this.queueCapacity);
        dataSourceInfo.setThreadFactory(this.threadFactory);
        dataSourceInfo.setRejectedPolicy(this.rejectedPolicy);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

}
