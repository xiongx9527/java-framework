package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.tinyengine.service.AppService;
import com.xiongx9527.fw.plus.tinyengine.service.AppTemplateService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.BlockService;
import com.xiongx9527.fw.plus.tinyengine.service.BusinessCategoryService;
import com.xiongx9527.fw.plus.tinyengine.service.DatasourceService;
import com.xiongx9527.fw.plus.tinyengine.service.I18nEntryService;
import com.xiongx9527.fw.plus.tinyengine.service.ModelService;
import com.xiongx9527.fw.plus.tinyengine.service.PageService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceGroupService;
import com.xiongx9527.fw.plus.tinyengine.service.ResourceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TinyEngineApiConfiguration {

	@Bean
	@ConditionalOnBean({AppService.class, PageService.class, BlockService.class, BlockGroupService.class,
			BusinessCategoryService.class, ModelService.class, DatasourceService.class, AppTemplateService.class,
			ResourceGroupService.class, ResourceService.class, I18nEntryService.class})
	@ConditionalOnMissingBean
	public TinyEngineApiFacade tinyEngineApiFacade(AppService appService, PageService pageService,
			BlockService blockService, BlockGroupService blockGroupService,
			BusinessCategoryService businessCategoryService, ModelService modelService,
			DatasourceService datasourceService, AppTemplateService appTemplateService,
			ResourceGroupService resourceGroupService, ResourceService resourceService,
			I18nEntryService i18nEntryService, TinyEngineExternalCapability externalCapability) {
		return new DefaultTinyEngineApiFacade(appService, pageService, blockService, blockGroupService,
				businessCategoryService, modelService, datasourceService, appTemplateService, resourceGroupService,
				resourceService, i18nEntryService, externalCapability);
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyEngineExternalCapability tinyEngineExternalCapability() {
		return new DefaultTinyEngineExternalCapability();
	}
}
