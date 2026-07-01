package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.common;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.starter.MagicVue3Config;
import com.xx9527.fw.plus.common.util.SpringUtils;

import java.util.Map;

public interface BaseService extends MagicBaseService {

    /**
     * 获取 Vue3 组件资源路径前缀，默认 /vue3/component/
     */
    default String getVue3ComponentPath() {
        return getConfigValueAsString(getConfig(), "vue.componentPath", "/vue3/component/");
    }

    /**
     * 获取MagicVue3的配置信息
     *
     * @return 返回MagicVue3Config配置对象的Bean实例
     */
    default Map<String, Object> getConfig() {
        return SpringUtils.getBean(MagicVue3Config.class);
    }


    /**
     * 获取Vue.js的依赖库URL地址
     *
     * @return 返回Vue.js全局库的URL地址，如果配置中未指定则返回默认的unpkg CDN地址
     */
    //<script src="https://unpkg.com/vue@3/dist/vue.global.prod.js"></script>
    //<script src="https://unpkg.com/vue@3/dist/vue.global.js"></script>
    default String getJsDependences() {
        return getConfigValueAsString(getConfig(),"vue.jsDependences", "/vue3/js/vue.global.3.5.13.prod.js,/vue3/js/vue.axios.1.7.0.min.js,/vue3/js/base.1.0.0.js");
    }


    /**
     * 获取CSS依赖配置值
     *
     * @return 返回CSS依赖配置字符串，如果未配置则返回空字符串
     */
    default String getCssDependences() {
        return getConfigValueAsString(getConfig(),"vue.cssDependences", "");
    }






}
