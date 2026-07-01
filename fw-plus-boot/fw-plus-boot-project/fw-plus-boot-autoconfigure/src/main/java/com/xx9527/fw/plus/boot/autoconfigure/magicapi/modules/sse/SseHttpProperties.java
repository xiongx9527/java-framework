package com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.sse;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(
        prefix = "fw.plus.magicapi.ssehttp"
)
public class SseHttpProperties {

    /**
     * 连接超时时间（秒），默认10秒
     */
    private int connectTimeoutSec = 10;
    
    /**
     * 读取超时时间（秒），默认0秒表示不超时
     */
    private int readTimeoutSec = 0;
    
    /**
     * 写入超时时间（秒），默认10秒
     */
    private int writeTimeoutSec = 10;
}
