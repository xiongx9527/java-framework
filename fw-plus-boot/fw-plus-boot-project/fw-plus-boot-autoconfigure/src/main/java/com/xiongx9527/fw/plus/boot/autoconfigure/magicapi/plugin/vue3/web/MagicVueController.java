package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.web;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.model.VueInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.modules.VueModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.service.VueInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MagicVueController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 魔法API配置属性对象
     * 用于存储和管理API相关的配置信息
     */
    private final MagicAPIProperties properties;

    /**
     * Vue模块对象
     * 用于处理Vue相关的功能模块
     */
    private final VueModule vueModule;

    /**
     * MagicVueController构造函数
     *
     * @param configuration 魔法配置对象，用于初始化父类
     * @param properties 魔法API属性配置对象
     * @param vueModule Vue模块对象
     */
    public MagicVueController(MagicConfiguration configuration, MagicAPIProperties properties, VueModule vueModule) {
        super(configuration);
        this.properties = properties;
        this.vueModule = vueModule;
    }

    /**
     * 执行Vue3页面渲染
     */
    @PostMapping("/vue3/execute")
    @ResponseBody
    public JsonBean<Object> execute(String id,String sourceCode,String  compiledJs,String compiledCss){
        if (StringUtils.isBlank(id)) {
            return new JsonBean<>(new IllegalArgumentException("非法的文件ID"));
        }
        VueInfoMagicResourceStorage.Vue3ComponentInfo data = new VueInfoMagicResourceStorage.Vue3ComponentInfo();
        data.setId(id);
        data.setSourceCode(sourceCode);
        data.setCompileJs(compiledJs);
        data.setCompileCss(compiledCss);
        SpringUtils.getBean(VueInfoMagicResourceStorage.class).saveCode(data);
        VueInfo entity = MagicConfiguration.getMagicResourceService().file(id);
        try {
            if (StringUtils.isBlank(entity.getCompileJs())) {
                throw new IllegalArgumentException("源码还未编译");
            }
            String name = entity.getName();
            String jsDependences = entity.getJsDependences();
            // 如果实体中没有设置JS依赖，则使用默认的JS依赖配置
            if (StringUtils.isBlank(jsDependences)) {
                jsDependences = getJsDependences();
            }

            String cssDependences = entity.getCssDependences();
            // 如果实体中没有设置CSS依赖，则使用默认的CSS依赖配置
            if (StringUtils.isBlank(cssDependences)) {
                cssDependences = getCssDependences();
            }
            String html = vueModule.createHtml(name, compiledJs, compiledCss,
                    jsDependences.split(","), cssDependences.split(","),JSON.parseObject(entity.getParams()));
            return new JsonBean<>(html);
        } catch (Exception e) {
            log.error("执行Vue3页面时发生错误，ID: {}", id, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取Vue3组件树数据
     */
    @RequestMapping("/vue3/component/tree")
    @ResponseBody
    public JsonBean<Object> getComponentTree() {
        Map<String, Object> data = new HashMap<>();
        data.put("list", SpringUtils.getBean(VueInfoMagicResourceStorage.class).findVue3ComponentTree());
        data.put("total", 1);
        JsonBean<Object> jsonBean = new JsonBean<>();
        jsonBean.setData(data);
        jsonBean.setCode(200);
        return jsonBean;
    }

    /**
     * 获取Vue3组件的源码
     */
    @RequestMapping("/vue3/component/getSourceCode")
    @ResponseBody
    public JsonBean<Object> getSourceCode(String id) {
        JsonBean<Object> jsonBean = new JsonBean<>();
        jsonBean.setData(SpringUtils.getBean(VueInfoMagicResourceStorage.class).getSourceCode(id).getSourceCode());
        jsonBean.setCode(200);
        return jsonBean;
    }

    /**
     * 获取Vue3组件的历史记录信息
     */
    @RequestMapping("/vue3/component/history")
    @ResponseBody
    public JsonBean<Object> history(String componentId) {
        Map<String, Object> data = new HashMap<>();
        List<VueInfo.Vue3ComponentHisInfo> history = ListUtils.emptyIfNull(SpringUtils.getBean(VueInfoMagicResourceStorage.class).history(componentId));
        data.put("list", history);
        data.put("total", history.size());
        JsonBean<Object> jsonBean = new JsonBean<>();
        jsonBean.setData(data);
        jsonBean.setCode(200);
        return jsonBean;
    }

    /**
     * 获取指定组件的最后代码
     */
    @RequestMapping("/vue3/component/getLastCode")
    @ResponseBody
    public JsonBean<Object> getLastCode(String componentId) {
        JsonBean<Object> jsonBean = new JsonBean<>();
        jsonBean.setData(SpringUtils.getBean(VueInfoMagicResourceStorage.class).getLastCode(componentId));
        jsonBean.setCode(200);
        return jsonBean;
    }

    /**
     * 保存Vue3组件代码
     */
    @PostMapping(value = "/vue3/component/saveCode")
    @ResponseBody
    public JsonBean<Object> save(@ModelAttribute VueInfoMagicResourceStorage.Vue3ComponentInfo data) {
        SpringUtils.getBean(VueInfoMagicResourceStorage.class).saveCode(data);
        JsonBean<Object> jsonBean = new JsonBean<>();
        jsonBean.setCode(200);
        return jsonBean;
    }
}
