package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptionProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import org.ssssssss.magicapi.core.annotation.Valid;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.context.MagicUser;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MagicAPIControllerJdbcOptionsTest {

    @Test
    void datasourceOptionsEndpointRequiresAuthenticatedConsoleAccess() throws Exception {
        Method method = MagicAPIController.class.getMethod("datasourceOptions", MagicHttpServletRequest.class);

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertEquals("${magic-api.web:/magic-api/web}/datasource/options", getMapping.value()[0]);

        Valid valid = method.getAnnotation(Valid.class);
        assertNotNull(valid);
        assertTrue(valid.requireLogin());

        assertEquals(JsonBean.class, method.getReturnType());
        ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
        assertEquals(MagicJdbcClassOptions.class, returnType.getActualTypeArguments()[0]);
        assertFalse(method.isAnnotationPresent(Deprecated.class));
    }

    @Test
    void datasourceOptionsRejectsRequestsWithoutViewPermission() {
        MagicConfiguration configuration = new MagicConfiguration();
        configuration.setAuthorizationInterceptor(new AuthorizationInterceptor() {
            @Override
            public boolean allowVisit(MagicUser magicUser, MagicHttpServletRequest request, Authorization authorization) {
                return false;
            }
        });

        MagicAPIController controller = new MagicAPIController(configuration, mock(MagicJdbcClassOptionProvider.class));

        Assertions.assertThrows(RuntimeException.class, () -> controller.datasourceOptions(mock(MagicHttpServletRequest.class)));
    }

    @Test
    void optionListKeepsRepeatSubmitOptionsAndMillisecondTimeoutLabel() {
        MagicAPIController controller = new MagicAPIController(new MagicConfiguration(), mock(MagicJdbcClassOptionProvider.class));

        JsonBean<List<List<String>>> result = controller.options();
        @SuppressWarnings("unchecked")
        List<List<String>> options = (List<List<String>>) result.getData();

        assertThat(options).contains(
                Arrays.asList(Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue(), "启动避免重复提交", "true"),
                Arrays.asList(Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue(), "避免重复提交超时时间(毫秒)", "1000"),
                Arrays.asList(Options.AVOID_REPEAT_SUBMIT_KEY.getValue(), "避免重复提交的 Key（支持 SpEL 表达式）", "#{magicapi:repeat:submit}"),
                Arrays.asList(Options.AVOID_REPEAT_SUBMIT_KEY_SUBFIX_SPRING_BEAN.getValue(), "避免重复提交的 Key 后缀（Spring Bean 名称）", "springbean"),
                Arrays.asList(Options.AVOID_REPEAT_SUBMIT_MSG.getValue(), "避免重复提交提示信息", "重复提交，请稍后再试。"));
    }

    @Test
    void managementServiceOptionListKeepsRepeatSubmitTimeoutPayload() {
        MagicAPIManagementService managementService = new MagicAPIManagementService(
                new MagicConfiguration(), mock(MagicJdbcClassOptionProvider.class));

        JsonBean<List<List<String>>> result = managementService.options();

        @SuppressWarnings("unchecked")
        List<List<String>> options = (List<List<String>>) result.getData();
        assertThat(options).contains(Arrays.asList(
                Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue(),
                "避免重复提交超时时间(毫秒)",
                "1000"));
    }

    @Test
    void controllerDelegatesOptionListWithoutChangingPayload() {
        MagicAPIController controller = new MagicAPIController(new MagicConfiguration(), mock(MagicJdbcClassOptionProvider.class));

        JsonBean<List<List<String>>> result = controller.options();

        @SuppressWarnings("unchecked")
        List<List<String>> options = (List<List<String>>) result.getData();
        assertThat(options).contains(Arrays.asList(
                Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue(),
                "避免重复提交超时时间(毫秒)",
                "1000"));
    }
}
