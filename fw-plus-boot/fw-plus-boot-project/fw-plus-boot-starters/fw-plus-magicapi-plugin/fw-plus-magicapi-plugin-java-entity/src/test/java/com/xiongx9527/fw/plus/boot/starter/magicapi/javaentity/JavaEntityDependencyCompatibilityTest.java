package com.xiongx9527.fw.plus.boot.starter.magicapi.javaentity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JavaEntityDependencyCompatibilityTest {

    @Test
    void pluginClasspathContainsSupportedEntityAnnotations() {
        assertDoesNotThrow(() -> Class.forName("lombok.Data"));
        assertDoesNotThrow(() -> Class.forName("com.baomidou.mybatisplus.annotation.TableName"));
        assertDoesNotThrow(() -> Class.forName("com.mybatisflex.annotation.Table"));
        assertDoesNotThrow(() -> Class.forName("com.alibaba.fastjson.annotation.JSONField"));
        assertDoesNotThrow(() -> Class.forName("com.alibaba.fastjson2.annotation.JSONField"));
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.annotation.JsonProperty"));
        assertDoesNotThrow(() -> Class.forName("com.fasterxml.jackson.databind.annotation.JsonSerialize"));
    }
}
