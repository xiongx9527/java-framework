package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.web;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;


/**
 * @author zzy
 */
@Slf4j
public class MagicWsController extends MagicController implements MagicExceptionHandler , BaseService {

    public MagicWsController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 执行Web Service操作
     * @param id 服务ID
     * @param request HTTP请求对象
     * @return 执行结果的JsonBean对象
     */
    @PostMapping("/ws/execute")
    @ResponseBody
    public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
        return new JsonBean<>("ok");
    }

    /**
     * 测试数据源连接
     * @param properties 数据源配置信息
     * @return 测试结果的JsonBean对象
     */
    @RequestMapping("/ws/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody WsDataSourceInfo properties) {
        return new JsonBean<>("ok");
    }

}
