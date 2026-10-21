package com.xiongx9527.fw.plus.mybatis.datascope.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 行级数据权限异常的国际化文案测试。
 *
 * <p>验证迁移后的异常以「具体错误码 + 参数」构造时，能按指定语言解析出中文 / 英文文案。</p>
 */
@DisplayName("行权限异常国际化文案")
class RowPermissionExceptionI18nTest {

    private static final Locale ZH = Locale.SIMPLIFIED_CHINESE;
    private static final Locale EN = Locale.US;

    @Test
    @DisplayName("配置异常：中文（默认）")
    void configException_chinese() {
        RowPermissionConfigException exception = RowPermissionConfigException.of(
                RowPermissionConfigException.DB_TYPE_REQUIRED, "row.xml");
        assertEquals(50_003_102, exception.getCode());
        assertEquals("必须配置 dbType。file=row.xml", exception.getMessage(ZH));
    }

    @Test
    @DisplayName("配置异常：英文")
    void configException_english() {
        RowPermissionConfigException exception = RowPermissionConfigException.of(
                RowPermissionConfigException.DB_TYPE_REQUIRED, "row.xml");
        assertEquals("dbType is required. file=row.xml", exception.getMessage(EN));
    }

    @Test
    @DisplayName("配置异常：scope 重复定义带两个参数")
    void configException_scopeDuplicated() {
        RowPermissionConfigException exception = RowPermissionConfigException.of(
                RowPermissionConfigException.SCOPE_DUPLICATED, "all", "row.xml");
        assertEquals("scope=all 重复定义。file=row.xml", exception.getMessage(ZH));
        assertEquals("scope=all is defined more than once. file=row.xml", exception.getMessage(EN));
    }

    @Test
    @DisplayName("处理异常：中文（默认）")
    void handleException_chinese() {
        RowPermissionHandleException exception = RowPermissionHandleException.of(
                RowPermissionHandleException.TABLE_PERMISSION_UNDEFINED, "t_order");
        assertEquals(50_003_210, exception.getCode());
        assertEquals("表 t_order 未定义行权限", exception.getMessage(ZH));
    }

    @Test
    @DisplayName("处理异常：英文")
    void handleException_english() {
        RowPermissionHandleException exception = RowPermissionHandleException.of(
                RowPermissionHandleException.TABLE_PERMISSION_UNDEFINED, "t_order");
        assertEquals("No row permission defined for table t_order", exception.getMessage(EN));
    }

    @Test
    @DisplayName("处理异常：无参数文案")
    void handleException_withoutArgs() {
        RowPermissionHandleException exception = RowPermissionHandleException.of(
                RowPermissionHandleException.UNSUPPORTED_JOIN_USING);
        assertEquals("不支持 Join Using", exception.getMessage(ZH));
        assertEquals("Join Using is not supported", exception.getMessage(EN));
    }

    @Test
    @DisplayName("scope 未定义文案带两个占位参数")
    void handleException_scopeUndefined() {
        RowPermissionHandleException exception = RowPermissionHandleException.of(
                RowPermissionHandleException.SCOPE_PERMISSION_UNDEFINED, "t_order", 7);
        assertEquals("表 t_order 未定义 scope=7 的行权限", exception.getMessage(ZH));
        assertEquals("No row permission defined for scope=7 of table t_order", exception.getMessage(EN));
    }

    @Test
    @DisplayName("未定义资源的状态码解析为 null，不抛异常")
    void undefinedResourceResolvesToNull() {
        assertNull(com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver.resolve(50_003_001, ZH));
    }
}
