package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.common.BaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MagicDynamicMilvusDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MilvusDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import io.milvus.v2.client.MilvusClientV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

@Slf4j
public class MagicMilvusController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数，用于创建MagicMilvusController实例
     *
     * @param configuration 魔法配置对象，用于初始化控制器的基础配置
     */
    public MagicMilvusController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 测试Milvus数据源连接
     * 验证提供的Milvus数据源配置信息是否正确，并能够成功建立连接
     *
     * @param properties Milvus数据源配置信息
     * @return JsonBean 测试结果，"ok"表示连接成功，否则返回错误信息
     */
    @RequestMapping("/milvus/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody MilvusDataSourceInfo properties) {
        MilvusClientV2 client = null;
        MagicDynamicMilvusDataSource dataSource = SpringUtils.getBean(MagicDynamicMilvusDataSource.class);
        try {
            validateField(properties);
            if (dataSource == null) {
                throw new IllegalStateException("Milvus数据源服务未初始化");
            }
            client = dataSource.buildClient(properties);
            client.checkHealth();
            return new JsonBean<>("ok");
        } catch (Exception e) {
            log.error("测试 Milvus 连接失败，数据源key: {}", properties != null ? properties.getKey() : "unknown", e);
            // 避免将详细的异常信息返回给前端，防止信息泄露
            return new JsonBean<>("连接测试失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        } finally {
            // 确保在所有情况下都销毁客户端，释放资源
            if (client != null) {
                try {
                    dataSource.destroyClient(client);
                } catch (Exception e) {
                    log.warn("销毁Milvus测试客户端时发生异常", e);
                }
            }
        }
    }
}
