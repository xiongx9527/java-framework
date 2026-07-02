package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.web;

import com.alibaba.fastjson.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.modules.FreemarkerModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FtlInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;


import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MagicFreemarkerController extends MagicController implements MagicBaseService {

    /**
     * 构造函数，用于创建MagicFreemarkerController实例
     *
     * @param configuration 魔法配置对象，用于初始化控制器的基础配置
     */
    public MagicFreemarkerController(MagicConfiguration configuration) {
        super(configuration);
    }


    /**
     * 执行FreeMarker模板渲染
     *
     * @param id 模板文件ID，用于定位具体的模板文件，不能为空
     * @return JsonBean<Object> 包含渲染后HTML内容的响应对象
     * @throws IllegalArgumentException 当参数id为空时抛出
     * @throws RuntimeException 当模板文件不存在、模板参数为空或模板处理失败时抛出
     */
    @ResponseBody
    @PostMapping("/freemarker/execute")
    public JsonBean<Object> execute(String id) {
        // 参数校验
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("参数 id 不能为空");
        }

        try {
            // 根据文件 ID 获取 FtlInfo 对象
            FtlInfo entity = MagicConfiguration.getMagicResourceService().file(id);
            if (entity == null) {
                throw new RuntimeException("未找到对应的模板文件");
            }

            // 参数校验
            String params = entity.getParams();
            if (params != null && !params.isEmpty()) {
                validateJSON(params, "参数");
            }

            // 构建模板路径
            String path = SpringUtils.getBean(FtlInfoMagicResourceStorage.class).buildMappingKey(entity);

            // 获取模板对象
            Template template = SpringUtils.getBean(FreemarkerModule.class).createTemplateByPath(path);

            // 解析参数为 Map
            Map<String, Object> paramsMap = parseParamsToMap(params);

            // 处理模板并生成 HTML
            StringWriter out = new StringWriter();
            try {
                template.process(paramsMap, out);
                String html = out.toString();
                return new JsonBean<>(html);
            } finally {
                // 明确释放 writer 引用
                out.close(); // StringWriter.close() 是空操作，但保留语义完整性
            }

        } catch (Exception e) {
            log.error("模板处理失败: {}", e.getMessage(), e);
            throw new RuntimeException("模板处理失败", e);
        }
    }

    /**
     * 将参数字符串解析为Map对象
     * @param params 参数字符串，预期为JSON格式
     * @return 解析后的Map对象，如果参数为空则返回空Map
     */
    private Map<String, Object> parseParamsToMap(String params) {
        // 如果参数为空，返回空的HashMap
        if (StringUtils.isEmpty(params)) {
            return new HashMap<>();
        }
        // 使用JSON解析器将参数字符串转换为Map对象
        return JSON.parseObject(params);
    }


}
