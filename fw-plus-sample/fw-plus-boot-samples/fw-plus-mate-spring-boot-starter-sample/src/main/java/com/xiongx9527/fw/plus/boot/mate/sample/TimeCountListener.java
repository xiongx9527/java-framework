package com.xiongx9527.fw.plus.boot.mate.sample;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.ServletRequestHandledEvent;

@Slf4j
@Component
public class TimeCountListener implements ApplicationListener<ServletRequestHandledEvent> {

    @Override
    public void onApplicationEvent(ServletRequestHandledEvent event) {
        Throwable failureCause = event.getFailureCause();
        if ( failureCause != null){
            log.error("错误原因: {}\n", failureCause.getMessage());
        }
        Object o = event.getSource();
        log.info("========================================");
        log.info("请求客户端地址：{} , 请求URL: {} , 请求Method: {} , 请求耗时: {}毫秒",
                event.getClientAddress(),
                event.getRequestUrl(),
                event.getMethod(),
                event.getProcessingTimeMillis());
        log.info("========================================");
    }
}