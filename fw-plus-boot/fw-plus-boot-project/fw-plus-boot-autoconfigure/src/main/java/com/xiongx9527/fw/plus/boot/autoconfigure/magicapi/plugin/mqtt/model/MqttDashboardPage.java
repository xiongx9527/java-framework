package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MqttDashboardPage<T> {

    private List<T> list = new ArrayList<T>();
    private int pageNumber;
    private int pageSize;
    private long totalRow;
}
