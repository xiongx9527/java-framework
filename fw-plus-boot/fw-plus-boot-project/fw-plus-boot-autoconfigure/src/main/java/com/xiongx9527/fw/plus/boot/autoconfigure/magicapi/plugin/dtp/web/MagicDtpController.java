package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

public class MagicDtpController extends MagicController implements MagicExceptionHandler, BaseService {

    private final MagicDynamicDtpDataSource magicDynamicDtpDataSource;

    public MagicDtpController(MagicConfiguration configuration, MagicDynamicDtpDataSource magicDynamicDtpDataSource) {
        super(configuration);
        this.magicDynamicDtpDataSource = magicDynamicDtpDataSource;
    }

    @RequestMapping("/dtp/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody DtpDataSourceInfo properties) {
        DtpExecutor client = null;
        try {
            validateField(properties);
            client = magicDynamicDtpDataSource.buildClient(properties);
            return new JsonBean<>("ok");
        } catch (Exception e) {
            return new JsonBean<>(e.getMessage());
        } finally {
            if (client != null) {
                magicDynamicDtpDataSource.destroyClient(client);
            }
        }
    }




}
