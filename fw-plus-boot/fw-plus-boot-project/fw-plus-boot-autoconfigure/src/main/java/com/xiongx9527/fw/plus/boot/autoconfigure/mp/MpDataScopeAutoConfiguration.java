package com.xiongx9527.fw.plus.boot.autoconfigure.mp;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;

@Slf4j
//@ConditionalOnBean({SqlSessionFactory.class})
@AutoConfigureAfter(MybatisPlusAutoConfiguration.class)
@ConditionalOnProperty(prefix = "fw.plus.mp-datascope", name = "enabled", havingValue = "true",matchIfMissing = true)
@Lazy(false)
public class MpDataScopeAutoConfiguration {

}
