package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Option;

import java.util.List;
import java.util.Objects;

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

    /** magic.api./{path}.options.{key} > API options */
    public static String get(ApiInfo info, String key) {
        String pathKey = getPath(info) + ".options." + key;
        return MagicAPIConfigUtils.getMagicAPIConfig(pathKey, getOptionValue(info, key));
    }

    private static String getOptionValue(ApiInfo info, String key) {
        try {
            return info.getOptionValue(key);
        } catch (NullPointerException e) {
            List<Option> options = info.getOptions();
            if (options == null) {
                return null;
            }
            for (Option option : options) {
                if (key.equals(option.getName())) {
                    return Objects.toString(option.getValue(), null);
                }
            }
            return null;
        }
    }
}
