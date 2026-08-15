package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.BaseMagicDynamicMqttDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MagicAPIMqttServer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardClientInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardPage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardServerInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardStats;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardSubscriptionRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.service.MqttDashboardService;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.core.server.model.Subscribe;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.util.List;

@Slf4j
public class MagicMqttController extends MagicController implements MagicExceptionHandler , BaseService {

    private final MqttDashboardService dashboardService;

    /**
     * 构造函数
     * 
     * @param configuration Magic配置实例
     */
    public MagicMqttController(MagicConfiguration configuration) {
        this(configuration, SpringUtils.getBean(MqttDashboardService.class));
    }

    public MagicMqttController(MagicConfiguration configuration, MqttDashboardService dashboardService) {
        super(configuration);
        this.dashboardService = dashboardService;
    }

    @GetMapping("/mqtt/dashboard/servers")
    @ResponseBody
    public JsonBean<List<MqttDashboardServerInfo>> servers() {
        return new JsonBean<List<MqttDashboardServerInfo>>(dashboardService.listServers());
    }

    @GetMapping("/mqtt/dashboard/{serverId}/stats")
    @ResponseBody
    public JsonBean<MqttDashboardStats> stats(@PathVariable String serverId) {
        return new JsonBean<MqttDashboardStats>(dashboardService.stats(serverId));
    }

    @GetMapping("/mqtt/dashboard/{serverId}/clients")
    @ResponseBody
    public JsonBean<MqttDashboardPage<MqttDashboardClientInfo>> clients(@PathVariable String serverId,
                                                                         @RequestParam(value = "_page", required = false) Integer page,
                                                                         @RequestParam(value = "_limit", required = false) Integer limit) {
        return new JsonBean<MqttDashboardPage<MqttDashboardClientInfo>>(dashboardService.clients(serverId, page, limit));
    }

    @GetMapping("/mqtt/dashboard/{serverId}/clients/info")
    @ResponseBody
    public JsonBean<MqttDashboardClientInfo> clientInfo(@PathVariable String serverId,
                                                         @RequestParam String clientId) {
        return new JsonBean<MqttDashboardClientInfo>(dashboardService.clientInfo(serverId, clientId));
    }

    @GetMapping("/mqtt/dashboard/{serverId}/client/subscriptions")
    @ResponseBody
    public JsonBean<List<Subscribe>> subscriptions(@PathVariable String serverId,
                                                    @RequestParam String clientId) {
        return new JsonBean<List<Subscribe>>(dashboardService.subscriptions(serverId, clientId));
    }

    @PostMapping("/mqtt/dashboard/{serverId}/clients/delete")
    @ResponseBody
    public JsonBean<Object> deleteClient(@PathVariable String serverId, @RequestParam String clientId) {
        dashboardService.deleteClient(serverId, clientId);
        return new JsonBean<Object>();
    }

    @PostMapping("/mqtt/dashboard/{serverId}/mqtt/subscribe")
    @ResponseBody
    public JsonBean<Object> subscribe(@PathVariable String serverId,
                                      @RequestBody MqttDashboardSubscriptionRequest request) {
        dashboardService.subscribe(serverId, request);
        return new JsonBean<Object>();
    }

    @PostMapping("/mqtt/dashboard/{serverId}/mqtt/unsubscribe")
    @ResponseBody
    public JsonBean<Object> unsubscribe(@PathVariable String serverId,
                                        @RequestBody MqttDashboardSubscriptionRequest request) {
        dashboardService.unsubscribe(serverId, request);
        return new JsonBean<Object>();
    }

    @Override
    public Object exceptionHandler(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return new JsonBean<Object>(400, exception.getMessage());
        }
        return MagicExceptionHandler.super.exceptionHandler(exception);
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
