package com.xx9527.fw.plus.boot.mp.sample.handler;

import com.alibaba.fastjson.JSON;
import com.xx9527.fw.plus.mp.datachange.DataChangeHandler;
import com.xx9527.fw.plus.mp.datachange.properties.ChangeDataRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MpSampleDataChangeHandler extends DataChangeHandler {
    @Override
    public UserInfo getUserInfo() {
        return new UserInfo() {
            @Override
            public String getId() {
                return "";
            }

            @Override
            public String getName() {
                return "";
            }
        };
    }

    @Override
    public Map<Object, Object> enumToMap(Class<?> clazz) {
        return Collections.emptyMap();
    }

    @Override
    public void handler(List<ChangeDataRecord> operationRecords) {
        log.info("operationRecords:{}", JSON.toJSONString(operationRecords));
    }
}
