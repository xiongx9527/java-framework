package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptionProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptions;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.resource.MyDefaultMagicResourceService;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.interceptor.Authorization;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.utils.JdbcUtils;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MagicAPIManagementService implements JsonCodeConstants {

    private final MagicConfiguration configuration;
    private final MagicJdbcClassOptionProvider magicJdbcClassOptionProvider;

    public MagicAPIManagementService(MagicConfiguration configuration,
                                     MagicJdbcClassOptionProvider magicJdbcClassOptionProvider) {
        this.configuration = configuration;
        this.magicJdbcClassOptionProvider = magicJdbcClassOptionProvider;
    }

    public JsonBean<List<List<String>>> options() {
        return new JsonBean<>(Stream.of(Options.values())
                .map(item -> Arrays.asList(item.getValue(), item.getName(), item.getDefaultValue()))
                .collect(Collectors.toList()));
    }

    public JsonBean<MagicJdbcClassOptions> datasourceOptions(MagicAPIRequestAccess access) {
        isTrue(allowVisit(access, Authorization.VIEW), PERMISSION_INVALID);
        return new JsonBean<>(magicJdbcClassOptionProvider.getOptions());
    }

    public JsonBean<Boolean> reloadResource(String resource, MagicAPIRequestAccess access) {
        isTrue(allowVisit(access, Authorization.RELOAD), PERMISSION_INVALID);
        MyDefaultMagicResourceService service = (MyDefaultMagicResourceService) MagicConfiguration.getMagicResourceService();
        service.refreshResource(resource);
        return new JsonBean<>(true);
    }

    public JsonBean<Boolean> reloadSingleResource(String type, String id, MagicAPIRequestAccess access) {
        isTrue(allowVisit(access, Authorization.RELOAD), PERMISSION_INVALID);
        MyDefaultMagicResourceService service = (MyDefaultMagicResourceService) MagicConfiguration.getMagicResourceService();
        return new JsonBean<>(service.refreshResourceById(type, id));
    }

    public JsonBean<String> test(DataSourceInfo info) {
        try {
            String url = MagicAPIConfigUtils.getDbConfig(info.getKey(), "url", info.getUrl());
            String username = MagicAPIConfigUtils.getDbConfig(info.getKey(), "username", info.getUsername());
            String password = MagicAPIConfigUtils.getDbConfig(info.getKey(), "password", info.getPassword());
            String driverClassName = MagicAPIConfigUtils.getDbConfig(info.getKey(), "driverClassName", info.getDriverClassName());
            Connection connection = JdbcUtils.getConnection(driverClassName, url, username, password);
            JdbcUtils.close(connection);
        } catch (Exception e) {
            return new JsonBean<>(e.getMessage());
        }
        return new JsonBean<>("ok");
    }

    boolean allowVisit(MagicAPIRequestAccess access, Authorization authorization) {
        if (authorization == null) {
            return true;
        }
        return configuration.getAuthorizationInterceptor()
                .allowVisit(access.getMagicUser(), access.asServletRequest(), authorization);
    }
}
