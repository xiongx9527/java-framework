package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.web;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MongoDataSourceInfo;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

@Slf4j
public class MagicMongoController extends MagicController implements MagicExceptionHandler , BaseService {

    /**
     * 构造函数，用于创建MagicMongoController实例
     *
     * @param configuration 魔法配置对象，用于初始化控制器的基础配置
     */
    public MagicMongoController(MagicConfiguration configuration) {
        super(configuration);
    }


    /**
     * 测试Mongo数据源配置是否正确
     * 通过创建一个临时的Mongo引擎并尝试连接数据库来验证配置
     *
     * @param properties Mongo数据源配置信息
     * @return JsonBean 包含测试结果的对象，成功时返回"OK"，失败时返回错误信息
     */
    @RequestMapping("/mongo/datasource/test")
    @ResponseBody
    public JsonBean<Object> test(@RequestBody MongoDataSourceInfo properties) {
        MagicDynamicMongoDataSource magicDynamicMongoDataSource = SpringUtils.getBean(MagicDynamicMongoDataSource.class);
        MongoDatabaseFactory mongoDatabaseFactory = null;
        try {
            validateField(properties);
            mongoDatabaseFactory = magicDynamicMongoDataSource.createMongoDatabaseFactory(properties);
            mongoDatabaseFactory.getMongoDatabase().runCommand(new org.bson.Document("ping", 1));
            return new JsonBean<>("ok");
        } catch (IllegalArgumentException e) {
            log.warn("Mongo数据源配置验证失败: {}", e.getMessage());
            return new JsonBean<>("配置验证失败: " + e.getMessage());
        } catch (com.mongodb.MongoSocketException e) {
            log.warn("MongoDB连接失败: {}", e.getMessage());
            return new JsonBean<>("连接失败: " + e.getMessage());
        } catch (com.mongodb.MongoSecurityException e) {
            log.warn("MongoDB认证失败: {}", e.getMessage());
            return new JsonBean<>("认证失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("测试Mongo数据源配置失败", e);
            return new JsonBean<>("测试失败: " + e.getMessage());
        } finally {
            // 确保资源被正确释放
            if (mongoDatabaseFactory != null) {
                try {
                    magicDynamicMongoDataSource.destroyMongoDatabaseFactory(mongoDatabaseFactory);
                } catch (Exception e) {
                    log.warn("关闭MongoDatabaseFactory时发生异常", e);
                }
            }
        }
    }
}
