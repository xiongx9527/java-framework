package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.web;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;


/**
 * @author zzy
 */
@Slf4j
public class MagicSseController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数，用于创建MagicSseController实例
     *
     * @param configuration 魔法配置对象，用于初始化控制器的基础配置
     */
    public MagicSseController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 执行SSE请求处理
     * @param id 请求标识符
     * @param request MagicHttpServletRequest对象，包含请求信息
     * @return JsonBean<Object> 返回处理结果，状态为"ok"
     */
    @PostMapping("/sse/execute")
    @ResponseBody
    public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
        return new JsonBean<>("ok");
    }

    /**
     * 测试SSE数据源连接
     * @param properties SseDataSourceInfo对象，包含数据源连接属性信息
     * @return JsonBean<Object> 返回测试结果，状态为"ok"
     */
    @RequestMapping("/sse/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody SseDataSourceInfo properties) {
        return new JsonBean<>("ok");
    }
}
