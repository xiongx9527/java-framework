package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.web;

import com.influxdb.client.InfluxDBClient;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.MagicDynamicInfluxDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.InfluxDataSourceInfo;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

@Slf4j
public class MagicInfluxController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数，用于创建MagicInfluxController实例
     *
     * @param configuration 魔法配置对象，用于初始化控制器的基础配置
     */
    public MagicInfluxController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 测试InfluxDB数据源连接是否可用
     *
     * @param properties InfluxDB数据源配置信息
     * @return JsonBean<Object> 连接测试结果，成功返回"ok"，失败返回错误信息
     */
    @RequestMapping("/influx/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody InfluxDataSourceInfo properties) {
        MagicDynamicInfluxDataSource magicDynamicInfluxDataSource = SpringUtils.getBean(MagicDynamicInfluxDataSource.class) ;
        InfluxDBClient influxDBClient = null;
        try {
            // 验证数据源配置字段的有效性
            validateField(properties);

            // 创建InfluxDB客户端连接
            influxDBClient = magicDynamicInfluxDataSource.createInfluxDBClient(properties);

            // 测试连接是否可用
            boolean isAvailable = influxDBClient.ping();
            if (!isAvailable) {
                return new JsonBean<>("连接失败");
            }
            return new JsonBean<>("ok");
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return new JsonBean<>(e.getMessage());
        } finally {
            // 销毁InfluxDB客户端连接资源
            try {
                magicDynamicInfluxDataSource.destroyInfluxDBClient(influxDBClient);
            } catch (Exception e) {
                log.error("销毁连接失败", e);
            }
        }
    }


}
