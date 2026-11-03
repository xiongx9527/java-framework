package com.xiongx9527.fw.plus.ai.base.model.moderation;

/**
 * 审核类别：与厂商分类解耦的通用枚举，adapter 负责映射。
 */
public enum ModerationCategory {

    HATE,

    HARASSMENT,

    SELF_HARM,

    SEXUAL,

    VIOLENCE,

    ILLEGAL,

    PERSONAL_DATA,

    OTHER
}
