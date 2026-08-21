package com.xiongx9527.fw.plus.boot.mp.sample.handler;

import com.alibaba.fastjson.JSON;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class MpSampleDataChangeHandler implements DataChangeHandler {
	@Override
	public boolean supports(DataChangeEvent event) {
	    return true;
	}

    @Override
	public void handle(DataChangeEvent event) {
	    log.info("data change event: {}", JSON.toJSONString(event));
	}

	@Override
	public int getOrder() {
	    return 0;
	}
}
