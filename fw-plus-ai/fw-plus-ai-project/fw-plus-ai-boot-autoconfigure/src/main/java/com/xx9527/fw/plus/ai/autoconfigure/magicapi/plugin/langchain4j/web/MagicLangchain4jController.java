package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web;

import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.common.BaseService;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

@Slf4j
public class MagicLangchain4jController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数，用于创建MagicLangchain4jController实例
     * @param configuration 魔法配置对象，用于初始化控制器
     */
    public MagicLangchain4jController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 测试数据源连接状态的接口
     * 该方法接收数据源配置信息，验证数据源连接是否正常
     * @param properties 数据源配置信息对象，包含连接数据源所需的各种参数
     * @return JsonBean<Object> 返回测试结果，成功时返回"ok"
     */
    @RequestMapping("/langchain4j/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody Langchain4jDataSourceInfo properties) {
        return new JsonBean<>("ok");
    }

}
