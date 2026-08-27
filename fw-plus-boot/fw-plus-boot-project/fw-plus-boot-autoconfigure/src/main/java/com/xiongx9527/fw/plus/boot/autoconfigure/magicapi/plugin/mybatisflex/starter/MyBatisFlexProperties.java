package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisflex.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(
        prefix = "magic.api.mybatis-flex"
)
public class MyBatisFlexProperties {
    private boolean mapUnderscoreToCamelCase;
}
