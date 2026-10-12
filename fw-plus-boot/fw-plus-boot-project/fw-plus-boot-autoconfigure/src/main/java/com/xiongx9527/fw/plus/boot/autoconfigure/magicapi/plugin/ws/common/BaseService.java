package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;

/**
 * magic-api WS 插件的配置访问门面。
 *
 * <p>配置读取与 JWT 签发/校验能力已下沉到本模块
 * {@code com.xiongx9527.fw.plus.boot.autoconfigure.web.ws} 包中的
 * {@link WsBaseService}（按 {@code fw.plus.ws} 前缀绑定、点号扁平键读取）；本接口只做组合，
 * 使 magic 侧保留原有的 `BaseService` 语义与包名，30+ 处引用不变。</p>
 *
 * @author zzy
 */
public interface BaseService extends WsBaseService, MagicBaseService {
}
