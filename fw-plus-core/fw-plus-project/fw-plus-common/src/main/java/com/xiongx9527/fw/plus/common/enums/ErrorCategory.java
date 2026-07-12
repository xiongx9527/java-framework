package com.xiongx9527.fw.plus.common.enums;

import lombok.Getter;

/** 错误分类 — 状态码第3-5位 */
@Getter
public enum ErrorCategory {

    REQUEST         (1, "请求/参数"),
    AUTH            (2, "认证"),
    AUTHORIZATION   (3, "授权"),
    RESOURCE        (4, "资源"),
    BUSINESS        (5, "业务"),
    RATE_LIMIT      (6, "限流/熔断"),
    FILE_STORAGE    (7, "文件/存储"),
    THIRD_PARTY     (8, "第三方服务"),
    DATABASE        (9, "数据库"),
    INFRASTRUCTURE  (10, "系统/基础设施"),
    DATA            (11, "数据"),
    COMMUNICATION   (12, "通信"),
    PAYMENT         (13, "支付/交易"),
    USER_ACCOUNT    (14, "用户/账户"),
    NOTIFICATION    (15, "通知/消息"),
    CONFIG          (16, "配置"),
    SCHEDULED_TASK  (17, "定时任务"),
    CACHE           (18, "缓存"),
    CDC_STREAM      (19, "流/CDC"),
    SECURITY        (20, "安全/风控"),
    REPORT          (21, "报表/导出"),
    SEARCH_ENGINE   (22, "搜索引擎"),
    WORKFLOW        (23, "工作流"),
    SERIALIZATION   (24, "序列化/转换"),
    IDEMPOTENCY     (25, "幂等/去重"),
    CORS            (26, "跨域/CORS"),
    MULTI_TENANCY   (27, "多租户"),
    TEMPLATE        (28, "模板/渲染"),
    SCRIPT          (29, "脚本/表达式"),
    DEPENDENCY      (30, "依赖/License"),

    UNKNOWN         (999, "通用/未知");

    private final int code;
    private final String desc;

    ErrorCategory(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
