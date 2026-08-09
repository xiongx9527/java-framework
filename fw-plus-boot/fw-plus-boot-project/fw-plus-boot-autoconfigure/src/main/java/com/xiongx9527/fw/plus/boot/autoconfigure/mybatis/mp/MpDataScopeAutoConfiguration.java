package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.mp;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ConditionalOnProperty(prefix = "fw.plus.mp-datascope", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MpDataScopeAutoConfiguration {
}
