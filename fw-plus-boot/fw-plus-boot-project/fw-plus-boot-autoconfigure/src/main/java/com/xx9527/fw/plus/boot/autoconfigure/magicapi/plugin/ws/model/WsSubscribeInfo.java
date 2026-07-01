package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

/**
 * 订阅信息
 *
 * @author zzy
 */
@Data
public class WsSubscribeInfo extends PathMagicEntity {

    /**
     * 数据源Key
     */
    private String key;

    /**
     * 业务标识
     */
    private String businessId;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 描述
     */
    private String description;


    public WsSubscribeInfo copyTo(WsSubscribeInfo info) {
        super.copyTo(info);
        info.setKey(this.key);
        info.setEnabled(this.enabled);
        info.setDescription(this.description);
        info.setBusinessId(this.businessId);
        info.setPath(null);
        return info;
    }

    @Override
    public WsSubscribeInfo copy() {
        WsSubscribeInfo info = new WsSubscribeInfo();
        return copyTo(info);
    }

    @Override
    public MagicEntity simple() {
        WsSubscribeInfo info = new WsSubscribeInfo();
        return copyTo(info);
    }
}
