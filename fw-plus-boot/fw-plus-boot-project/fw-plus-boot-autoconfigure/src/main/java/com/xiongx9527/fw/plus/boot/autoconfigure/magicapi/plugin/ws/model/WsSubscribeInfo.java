package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSubscribeScript;
import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

/**
 * 订阅信息
 *
 * <p>实现 WS 地基的 {@link WsSubscribeScript}，把订阅名称与业务标识暴露给地基的会话/消息流程；
 * 脚本内容仍由 magic 侧持有。</p>
 *
 * @author zzy
 */
@Data
public class WsSubscribeInfo extends PathMagicEntity implements WsSubscribeScript {

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
