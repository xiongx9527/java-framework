package com.xiongx9527.fw.plus.auth.social;

import java.util.List;

/**
 * 身份提供方配置来源（解耦具体存储介质：数据库、yaml、配置中心均可实现）。
 */
public interface IdpConfigProvider {

    /**
     * 列出指定类型已启用的配置。
     */
    List<IdpConfig> listEnabled(IdpType type);

    /**
     * 按 id 取单个配置，不存在返回 {@code null}。
     */
    IdpConfig get(String id);
}
