package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.common;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.aia.starter.MagicAPIAiaConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

import java.util.Map;

public interface BaseService extends MagicBaseService {

    /**
     * 获取AIA配置信息
     *
     * @return 返回包含AIA配置的Map对象，键为配置项名称，值为配置项对应的对象
     */
    default Map<String,Object> getAiaConfig() {
        return SpringUtils.getBean(MagicAPIAiaConfig.class);
    }


}
