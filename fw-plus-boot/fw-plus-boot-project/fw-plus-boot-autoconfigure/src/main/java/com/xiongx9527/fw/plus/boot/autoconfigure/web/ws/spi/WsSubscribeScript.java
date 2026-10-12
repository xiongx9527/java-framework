package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi;

/**
 * 订阅脚本的通用描述。
 *
 * <p>不同业务侧（如 magic-api 的订阅实体）实现该接口，向 WebSocket 地基暴露
 * 订阅的标识信息；脚本内容与执行方式由业务侧自行持有。</p>
 *
 * @author zzy
 */
public interface WsSubscribeScript {

    /**
     * 订阅名称，用于日志与脚本命名。
     *
     * @return 订阅名称
     */
    String getName();

    /**
     * 订阅所属业务类型。
     *
     * @return 业务类型
     */
    String getBusinessId();
}
