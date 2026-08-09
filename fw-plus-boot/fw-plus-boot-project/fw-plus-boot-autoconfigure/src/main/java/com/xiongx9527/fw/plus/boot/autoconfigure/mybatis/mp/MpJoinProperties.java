package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp;

import com.github.yulichang.autoconfigure.MybatisPlusJoinProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "fw.plus.mp-join"
)
public class MpJoinProperties extends MybatisPlusJoinProperties {
}
