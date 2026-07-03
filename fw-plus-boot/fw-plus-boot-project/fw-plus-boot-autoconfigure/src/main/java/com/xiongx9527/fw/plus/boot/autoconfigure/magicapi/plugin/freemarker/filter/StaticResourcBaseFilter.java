package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.filter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FreemarkerMagicDynamicRegistry;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

public abstract class StaticResourcBaseFilter {

    /**
     * 魔法API前缀配置，从配置文件中读取magic.api.prefix属性值，如果没有配置则默认为空字符串
     */
    @Value("${magic-api.prefix:}")
    private String magicApiPrefix;

    /**
     * 抽象方法，用于将脚本内容写入响应对象
     * @param response 响应对象，用于输出脚本内容
     * @param suffix 文件后缀名，标识脚本类型
     * @param script 脚本内容字符串
     */
    abstract void writeScript(Object response,String suffix,String script) ;

    /**
     * 写入脚本内容到响应中
     * @param url 请求的URL地址
     * @param response 响应对象
     * @return 如果成功写入脚本则返回true，否则返回false
     */
    protected boolean writeScript(String url,Object response) {
        // 验证URL有效性
        if (StringUtils.isNotBlank(url)) {
            // 处理静态文件请求
            if (url.contains(".")) {
                int lastDotIndex = url.lastIndexOf(".");
                String path = url.substring(0, lastDotIndex);
                String suffix = url.substring(lastDotIndex + 1);
                // 移除前缀
                if (path.startsWith(magicApiPrefix)) {
                    path = path.substring(magicApiPrefix.length());
                }
                FtlInfo ftlInfo = SpringUtils.getBean(FreemarkerMagicDynamicRegistry.class).getMapping(path);
                if (ftlInfo != null) {
                    writeScript(response, suffix,ftlInfo.getScript());
                    return true;
                }
            }
        }
        return false;
    }

}
