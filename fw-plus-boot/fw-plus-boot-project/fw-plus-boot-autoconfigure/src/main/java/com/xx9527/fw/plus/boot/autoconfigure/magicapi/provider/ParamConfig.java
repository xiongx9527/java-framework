package com.xx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import org.ssssssss.magicapi.core.model.ApiInfo;

public final class ParamConfig {

    private ParamConfig() {}

    /** 确保 path 以 / 开头 */
    public static String getPath(ApiInfo info) {
        String path = info.getPath();
        return path.startsWith("/") ? path : "/" + path;
    }

    /** true/false 开关的便捷方法 */
    public static boolean isEnabled(ApiInfo info, String key) {
        return "true".equalsIgnoreCase(get(info, key));
    }

    /** API options > magic.api./{path}.options.{key} */
    public static String get(ApiInfo info, String key) {
        String pathKey = getPath(info) + ".options." + key;
        return MagicAPIConfigUtils.getMagicAPIConfig(pathKey, info.getOptionValue(key));
    }
}
