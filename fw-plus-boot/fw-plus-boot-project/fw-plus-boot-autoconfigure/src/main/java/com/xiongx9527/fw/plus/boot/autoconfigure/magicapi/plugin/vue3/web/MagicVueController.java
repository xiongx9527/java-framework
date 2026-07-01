package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.web;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.model.VueInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules.VueModule;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

public class MagicVueController extends MagicController implements MagicExceptionHandler , BaseService {

    private final MagicAPIProperties properties;
    private final VueModule vueModule;

    public MagicVueController(MagicConfiguration configuration, MagicAPIProperties properties, VueModule vueModule) {
        super(configuration);
        this.properties = properties;
        this.vueModule = vueModule;
    }

    @PostMapping("/vue3/execute")
    @ResponseBody
    public ResponseEntity<String> execute(String id){
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("非法的文件ID");
        }
        VueInfo entity = MagicConfiguration.getMagicResourceService().file(id);
        if (StringUtils.isBlank(entity.getCompileJs())) {
            throw new IllegalArgumentException("源码还未编译");
        }
        String name = entity.getName();
        String jsDependences = entity.getJsDependences();
        if (StringUtils.isBlank(jsDependences)) {
            jsDependences = getJsDependences();
        }
        String cssDependences = entity.getCssDependences();
        if (StringUtils.isBlank(cssDependences)) {
            cssDependences = getCssDependences();
        }
        String html = vueModule.createHtml(name, entity.getCompileJs(), entity.getCompileCss(),
                jsDependences.split(","), cssDependences.split(","),JSON.parseObject(entity.getParams()));
        return ResponseEntity.ok()
                .header("Content-Type", "text/html;charset=utf-8")
                .body(html);
    }
}
