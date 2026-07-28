package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.starter;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.module.MongoModule;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.ssssssss.magicapi.mongo.MongoCollectionExtension;
import org.ssssssss.magicapi.mongo.MongoFindIterableExtension;
import org.ssssssss.script.reflection.JavaReflection;

@ConditionalOnProperty(prefix = "magic-api.plugin.mogno", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.data.mongodb.core.MongoTemplate", "org.ssssssss.magicapi.mongo.MongoModule"})
@AutoConfigureAfter(MongoDataAutoConfiguration.class)
@Configuration
class MagicAPIMongoModuleAutoConfiguration {

	@Bean
	@ConditionalOnBean({MongoTemplate.class, MagicDynamicMongoDataSource.class})
	@ConditionalOnMissingBean
	MongoModule mongoFunctions(@Lazy MongoTemplate mongoTemplate,
			MagicDynamicMongoDataSource magicDynamicMongoDataSource) {
		JavaReflection.registerMethodExtension(MongoCollection.class, new MongoCollectionExtension());
		JavaReflection.registerMethodExtension(FindIterable.class, new MongoFindIterableExtension());
		return new MongoModule(mongoTemplate, magicDynamicMongoDataSource);
	}
}
