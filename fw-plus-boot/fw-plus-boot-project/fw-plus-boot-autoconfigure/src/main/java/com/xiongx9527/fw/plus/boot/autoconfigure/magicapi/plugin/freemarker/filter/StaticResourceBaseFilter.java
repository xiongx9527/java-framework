package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.filter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service.FreemarkerMagicDynamicRegistry;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

public abstract class StaticResourceBaseFilter {

    @Value("${magic-api.prefix:}")
    private String magicApiPrefix;

    abstract void writeScript(Object response, String suffix, String script);

    protected boolean writeScript(String url, Object response) {
        if (StringUtils.isBlank(url) || !url.contains(".")) {
            return false;
        }

        int lastDotIndex = url.lastIndexOf(".");
        String path = url.substring(0, lastDotIndex);
        String suffix = url.substring(lastDotIndex + 1);

        // 防止路径穿越
        if (path.contains("..") || path.contains("./") || path.contains("\0")) {
            return false;
        }

        // 移除魔法API前缀
        if (path.startsWith(magicApiPrefix)) {
            path = path.substring(magicApiPrefix.length());
        }

        FtlInfo ftlInfo = SpringUtils.getBean(FreemarkerMagicDynamicRegistry.class).getMapping(path);
        if (ftlInfo != null) {
            writeScript(response, suffix, ftlInfo.getScript());
            return true;
        }

        return false;
    }
}