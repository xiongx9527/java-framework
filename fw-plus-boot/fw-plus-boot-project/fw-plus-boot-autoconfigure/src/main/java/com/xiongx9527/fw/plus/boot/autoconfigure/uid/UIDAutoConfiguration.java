package com.xiongx9527.fw.plus.boot.autoconfigure.uid;

import cn.hutool.core.date.DateUtil;
import com.xiongx9527.fw.plus.boot.autoconfigure.uid.jdbc.UIDJdbcAutoConfiguration;
import com.xiongx9527.fw.plus.common.uid.UidGenerator;
import com.xiongx9527.fw.plus.common.uid.impl.CachedUidGenerator;
import com.xiongx9527.fw.plus.common.uid.worker.WorkerIdAssigner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Date;

@ConditionalOnProperty(prefix = "fw.plus.uid", name = "enabled", havingValue = "true", matchIfMissing = false)
@Configuration
@Import({UIDJdbcAutoConfiguration.class})
public class UIDAutoConfiguration {

    /**
     * 配置属性注入：雪花算法UID生成器的时间戳起始值
     *
     * 该字段用于配置雪花算法中时间戳计算的起始时间点，
     * 通过读取配置文件中的"fw.plus.uid.epochStr"属性值进行注入。
     * 如果配置文件中未指定该属性，则使用默认空字符串值。
     */
    @Value("${fw.plus.uid.epochStr:}")
    private String epochStr;

    /**
     * 创建并配置UidGenerator bean
     *
     * 该方法首先会检查容器中是否已有UidGenerator类型的bean，如果有，则不会创建新的bean
     * 然后，它会检查容器中是否存在WorkerIdAssigner类型的bean，如果不存在，则不会创建新的bean
     * 这两个条件通过Spring的@ConditionalOnMissingBean和@ConditionalOnBean注解实现
     *
     * @param workerIdAssigner WorkerIdAssigner类型的bean，用于分配worker id
     * @return 返回配置好的UidGenerator对象
     */
    @Bean
    @ConditionalOnMissingBean(value = {UidGenerator.class})
    @ConditionalOnBean(value = {WorkerIdAssigner.class})
    public UidGenerator uidGenerator(WorkerIdAssigner workerIdAssigner) {
        if (workerIdAssigner == null) {
            return null;
        }
        // 创建CachedUidGenerator实例，它是UidGenerator的一个实现
        CachedUidGenerator uidGenerator = new CachedUidGenerator();

        // 设置uid生成器的纪元时间如果未指定纪元时间，则使用当前日期
        if  (StringUtils.isNotBlank(epochStr)) {
            uidGenerator.setEpochStr(epochStr);
        } else {
            // 默认纪元时间设置为当前日期，格式为"yyyy-MM-dd"
            uidGenerator.setEpochStr(DateUtil.format(new Date(), "yyyy-MM-dd"));
        }

        // 设置WorkerIdAssigner，用于分配worker id
        uidGenerator.setWorkerIdAssigner(workerIdAssigner);

        // 返回配置好的uid生成器实例
        return uidGenerator;
    }
}
