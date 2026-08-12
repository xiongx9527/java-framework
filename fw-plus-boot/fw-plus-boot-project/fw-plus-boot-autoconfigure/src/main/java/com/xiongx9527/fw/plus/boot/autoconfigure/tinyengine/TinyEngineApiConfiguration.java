package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.tinyengine.service.AppExtensionService;
import com.xiongx9527.fw.plus.tinyengine.service.AppAiModelConfigService;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.service.AppRoleService;
import com.xiongx9527.fw.plus.tinyengine.service.AppTemplateService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockService;
import com.xiongx9527.fw.plus.tinyengine.service.BusinessCategoryService;
import com.xiongx9527.fw.plus.tinyengine.service.CanvasService;
import com.xiongx9527.fw.plus.tinyengine.service.DatasourceService;
import com.xiongx9527.fw.plus.tinyengine.service.I18nEntryService;
import com.xiongx9527.fw.plus.tinyengine.service.ModelService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import com.xiongx9527.fw.plus.tinyengine.service.PageHistoryService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import com.xiongx9527.fw.plus.tinyengine.service.TaskRecordService;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TinyEngineApiConfiguration {

	@Bean
	@Conditional(TinyEngineApiAvailableCondition.class)
	@ConditionalOnMissingBean
	public TinyEngineApiFacade tinyEngineApiFacade(AppService appService, PageService pageService,
			BlockService blockService, BlockGroupService blockGroupService,
			BusinessCategoryService businessCategoryService, ModelService modelService,
			DatasourceService datasourceService, AppTemplateService appTemplateService,
			ResourceGroupService resourceGroupService, ResourceService resourceService,
			I18nEntryService i18nEntryService, AppExtensionService appExtensionService,
			CanvasService canvasService, PageHistoryService pageHistoryService, TaskRecordService taskRecordService,
			TinyEngineExternalCapability externalCapability, AppRoleService appRoleService,
			AppAiModelConfigService appAiModelConfigService) {
		return new DefaultTinyEngineApiFacade(appService, pageService, blockService, blockGroupService,
				businessCategoryService, modelService, datasourceService, appTemplateService, resourceGroupService,
				resourceService, i18nEntryService, appExtensionService, canvasService, pageHistoryService,
				taskRecordService, externalCapability, appRoleService, appAiModelConfigService);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyEngineExternalCapability tinyEngineExternalCapability() {
		return new DefaultTinyEngineExternalCapability();
	}

	static class TinyEngineApiAvailableCondition extends AnyNestedCondition {

		TinyEngineApiAvailableCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(name = "tinyEngineServiceBeanDefinitionRegistryPostProcessor")
		static class DefaultServices {
		}

		@ConditionalOnBean({AppService.class, PageService.class, BlockService.class, BlockGroupService.class,
				AppRoleService.class, AppAiModelConfigService.class,
				BusinessCategoryService.class, ModelService.class, DatasourceService.class, AppTemplateService.class,
				ResourceGroupService.class, ResourceService.class, I18nEntryService.class, AppExtensionService.class,
				CanvasService.class, PageHistoryService.class, TaskRecordService.class})
		static class HostServices {
		}
	}
}
