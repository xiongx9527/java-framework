package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.common.BaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.MagicDynamicWeaviateDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.WeaviateDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.misc.model.Meta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MagicWeaviateController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数
     * 
     * @param configuration Magic配置实例
     */
    public MagicWeaviateController(MagicConfiguration configuration) {
        super(configuration);
    }


    @RequestMapping("/weaviate/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody WeaviateDataSourceInfo properties) {
        WeaviateClient client = null;
        try {
            validateField(properties);
            client = SpringUtils.getBean(MagicDynamicWeaviateDataSource.class).buildClient(properties);
            // 验证连接
            Result<Meta> result = client.misc().metaGetter().run();
            if (result.hasErrors()) {
                List<String> errorMessages = result.getError().getMessages().stream().map(e->e.getMessage()).collect(Collectors.toList());
                throw new RuntimeException(String.join("|", errorMessages));
            }
            return new JsonBean<>("ok");
        } catch (Exception e) {
            return new JsonBean<>(e.getMessage());
        } finally {
            // 确保在所有情况下都销毁客户端，释放资源
            if (client != null) {
                try {
                    SpringUtils.getBean(MagicDynamicWeaviateDataSource.class).destroyClient(client);
                } catch (Exception e) {
                }
            }
        }
    }
}
