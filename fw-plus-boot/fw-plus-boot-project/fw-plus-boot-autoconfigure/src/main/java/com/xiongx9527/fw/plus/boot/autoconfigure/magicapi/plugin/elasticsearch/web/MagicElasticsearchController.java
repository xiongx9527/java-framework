package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticSearchDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

@Slf4j
public class MagicElasticsearchController extends MagicController implements MagicExceptionHandler, BaseService {

    /**
     * 构造函数，用于创建MagicElasticsearchController实例
     *
     * @param configuration 魔法配置对象，用于初始化控制器的配置信息
     */
    public MagicElasticsearchController(MagicConfiguration configuration) {
        super(configuration);
    }

    /**
     * 测试ElasticSearch数据源连接
     * 该接口用于验证提供的ElasticSearch配置信息是否能够成功建立连接
     *
     * @param properties ElasticSearch数据源配置信息，包含连接所需的各种参数
     * @return JsonBean<Object> 返回测试结果，成功返回"ok"，失败返回错误信息
     */
    @RequestMapping("/elasticsearch/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody ElasticSearchDataSourceInfo properties) {
        RestClient restClient = null;
        MagicDynamicElasticSearchDataSource magicDynamicElasticSearchDataSource = SpringUtils.getBean(MagicDynamicElasticSearchDataSource.class) ;
        try {
            validateField(properties);

            // 创建ElasticSearch REST客户端
            restClient = magicDynamicElasticSearchDataSource.createElasticSearchRestClient(properties);

            // 实际测试连接：发送一个简单的请求来验证连通性
            Response response = restClient.performRequest(new Request("GET", "/_cluster/health"));
            if (response.getStatusLine().getStatusCode() != 200) {
                return new JsonBean<>("ElasticSearch连接失败");
            }
            return new JsonBean<>("ok");
        } catch (Exception e) {
            log.error("创建ElasticSearch连接失败", e);
            return new JsonBean<>(e.getMessage());
        } finally {
            // 销毁临时创建的ElasticSearch连接工厂资源
            try {
                magicDynamicElasticSearchDataSource.destroyElasticSearchRestClient(restClient);
            } catch (Exception e) {
                log.error("销毁ElasticSearch连接失败", e);
            }
        }
    }

}
