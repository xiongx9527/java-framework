package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

/** mem0 服务端鉴权方案：{@code API_KEY} 走 {@code X-API-Key} 头（兼容管理密钥 / 用户密钥），
 *  {@code BEARER} 走 {@code Authorization: Bearer}（JWT）。服务端关闭鉴权时不配置 apiKey 即可。 */
public enum MemoryAuthScheme {
    API_KEY,
    BEARER
}
