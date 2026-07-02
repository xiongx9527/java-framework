package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

@Slf4j
public class MagicDtpController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数
     * 
     * @param configuration Magic配置实例
     */
    public MagicDtpController(MagicConfiguration configuration) {
        super(configuration);
    }


    /**
     * 测试数据源连接功能
     *
     * @param properties 数据源配置信息对象，包含连接数据库所需的各种属性
     * @return JsonBean<Object> 返回操作结果，成功时返回"ok"，失败时返回错误信息
     */
    @RequestMapping("/dtp/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody DtpDataSourceInfo properties) {
        DtpExecutor client = null;
        try {
            // 验证数据源配置字段的有效性
            validateField(properties);
            // 构建数据源客户端连接
            client = SpringUtils.getBean(MagicDynamicDtpDataSource.class).buildClient(properties);
            return new JsonBean<>("ok");
        } catch (Exception e) {
            // 处理连接异常，返回错误信息
            return new JsonBean<>(e.getMessage());
        } finally {
            // 确保在所有情况下都销毁客户端，释放资源
            if (client != null) {
                try {
                    SpringUtils.getBean(MagicDynamicDtpDataSource.class).destroyClient(client);
                } catch (Exception e) {
                }
            }
        }
    }




}
