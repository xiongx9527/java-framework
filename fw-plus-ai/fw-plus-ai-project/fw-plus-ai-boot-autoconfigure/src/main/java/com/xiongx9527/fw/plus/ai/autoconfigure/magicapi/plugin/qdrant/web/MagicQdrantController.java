package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.web;

import com.google.common.util.concurrent.ListenableFuture;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.common.BaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.MagicDynamicQdrantDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.QdrantDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.QdrantOuterClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

@Slf4j
public class MagicQdrantController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数
     * 
     * @param configuration Magic配置实例
     */
    public MagicQdrantController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 测试Qdrant数据源连接
     * 
     * @param properties Qdrant数据源信息
     * @return 连接测试结果
     */
    @RequestMapping("/qdrant/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody QdrantDataSourceInfo properties) {
        QdrantClient client = null;
        try {
            validateField(properties);
            client = SpringUtils.getBean(MagicDynamicQdrantDataSource.class).buildClient(properties);
            ListenableFuture<QdrantOuterClass.HealthCheckReply> healthCheck = client.healthCheckAsync();
            QdrantOuterClass.HealthCheckReply reply = healthCheck.get();
            String version = reply.getVersion();
            log.info("Qdrant版本: {}", version);
            return new JsonBean<>("ok");
        } catch (Exception e) {
            log.error("测试 Qdrant 连接失败", e);
            return new JsonBean<>(e.getMessage());
        } finally {
            // 确保在所有情况下都销毁客户端，释放资源
            if (client != null) {
                try {
                    SpringUtils.getBean(MagicDynamicQdrantDataSource.class).destroyClient(client);
                } catch (Exception e) {
                    log.warn("销毁Qdrant测试客户端时发生异常", e);
                }
            }
        }
    }
}
