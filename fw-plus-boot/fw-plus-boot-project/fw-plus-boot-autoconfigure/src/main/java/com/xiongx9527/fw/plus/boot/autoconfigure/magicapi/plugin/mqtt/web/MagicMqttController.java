package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.BaseMagicDynamicMqttDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MagicAPIMqttServer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

@Slf4j
public class MagicMqttController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数
     * 
     * @param configuration Magic配置实例
     */
    public MagicMqttController(MagicConfiguration configuration) {
        super(configuration);
    }


    /**
     * 测试MQTT数据源连接功能
     *
     * @param properties MQTT数据源配置信息对象，包含连接所需的各种参数
     * @return JsonBean<Object> 返回操作结果，成功时返回"ok"，失败时返回错误信息
     */
    @RequestMapping("/mqtt/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody MqttDataSourceInfo properties) {
        MagicAPIMqttServer server = null;
        try {
            // 验证输入参数的合法性
            validateField(properties);

            // 构建MQTT客户端实例
            server = SpringUtils.getBean(BaseMagicDynamicMqttDataSource.class).buildServer(properties);

            // 验证连接
            server.start();
            return new JsonBean<>("ok");
        } catch (Exception e) {
            return new JsonBean<>(e.getMessage());
        } finally {
            // 确保在所有情况下都销毁客户端，释放资源
            if (server != null) {
                try {
                    SpringUtils.getBean(BaseMagicDynamicMqttDataSource.class).destroyServer(server);
                } catch (Exception e) {
                }
            }
        }
    }

}
