package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.web;

import com.alibaba.fastjson.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.modules.FreemarkerModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FtlInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.web.MagicController;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class MagicFreemarkerController extends MagicController implements MagicBaseService {

    public MagicFreemarkerController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 执行FreeMarker模板渲染
     *
     * @param id 模板文件ID
     * @return 包含渲染后HTML内容的响应对象
     */
    @ResponseBody
    @PostMapping("/freemarker/execute")
    public ResponseEntity<String> execute(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.freemarker.idRequired"));
        }

        FtlInfo entity = MagicConfiguration.getMagicResourceService().file(id);
        if (entity == null) {
            throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.freemarker.templateFileNotFound"));
        }

        String params = entity.getParams();
        if (StringUtils.isNotBlank(params)) {
            validateJSON(params, "参数");
        }

        String path = SpringUtils.getBean(FtlInfoMagicResourceStorage.class).buildMappingKey(entity);

        Template template = SpringUtils.getBean(FreemarkerModule.class).createTemplateByPath(path);

        Map<String, Object> paramsMap = parseParamsToMap(params);

        try (StringWriter out = new StringWriter()) {
            template.process(paramsMap, out);
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html;charset=utf-8")
                    .body(out.toString());
        } catch (Exception e) {
            log.error("模板处理失败: {}", e.getMessage(), e);
            throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.freemarker.templateProcessFailed"), e);
        }
    }

    /**
     * 将参数字符串解析为Map对象
     */
    private Map<String, Object> parseParamsToMap(String params) {
        if (StringUtils.isEmpty(params)) {
            return Collections.emptyMap();
        }
        return JSON.parseObject(params);
    }
}