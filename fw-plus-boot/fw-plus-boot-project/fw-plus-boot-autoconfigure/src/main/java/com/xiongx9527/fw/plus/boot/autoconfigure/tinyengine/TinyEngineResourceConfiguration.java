package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Configuration
@ConditionalOnClass(name = "com.xiongx9527.fw.plus.tinyengine.service.ResourceService")
@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.resources", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TinyEngineResourceConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.sql", name = "enabled", havingValue = "true", matchIfMissing = true)
	public TinyEngineSqlResources tinyEngineSqlResources(ApplicationContext applicationContext, TinyEngineProperties properties) {
		ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(applicationContext);
		String manifestLocation = properties.getSql().getManifestLocation();
		Resource manifest = resolver.getResource(manifestLocation);
		return new TinyEngineSqlResources(manifestLocation, properties.getSql().getDatabase(),
				properties.getSql().isAutoInitialize(), manifest,
				loadScriptResources(resolver, manifest, properties.getSql().getDatabase(), properties.getSql().isAutoInitialize()));
	}

	@Bean
	@ConditionalOnBean(DataSource.class)
	@ConditionalOnMissingBean(name = "tinyEngineSqlDataSourceInitializer")
	@ConditionalOnProperty(prefix = "fw.plus.tiny-engine.sql", name = "auto-initialize", havingValue = "true")
	public DataSourceInitializer tinyEngineSqlDataSourceInitializer(DataSource dataSource,
			TinyEngineSqlResources sqlResources) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setContinueOnError(false);
		populator.setIgnoreFailedDrops(false);
		populator.addScripts(sqlResources.getScripts().toArray(new Resource[sqlResources.getScripts().size()]));

		DataSourceInitializer initializer = new DataSourceInitializer();
		initializer.setDataSource(dataSource);
		initializer.setDatabasePopulator(populator);
		return initializer;
	}

	private static List<Resource> loadScriptResources(ResourcePatternResolver resolver, Resource manifest,
													 String database, boolean autoInitialize) {
		if (!StringUtils.hasText(database)) {
			return Collections.emptyList();
		}
		try {
			if (!manifest.exists()) {
				if (autoInitialize) {
					throw new IllegalStateException("TinyEngine SQL manifest does not exist: " + manifest);
				}
				return Collections.emptyList();
			}
			Properties manifestProperties = PropertiesLoaderUtils.loadProperties(manifest);
			List<String> keys = new ArrayList<String>(manifestProperties.stringPropertyNames());
			Collections.sort(keys);
			List<Resource> resources = new ArrayList<Resource>();
			String prefix = database + ".";
			for (String key : keys) {
				if (key.startsWith(prefix)) {
					resources.add(resolver.getResource(manifestProperties.getProperty(key)));
				}
			}
			return resources;
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to load TinyEngine SQL manifest from " + manifest, ex);
		}
	}

	public static class TinyEngineSqlResources {

		private final String manifestLocation;
		private final String database;
		private final boolean autoInitialize;
		private final Resource manifest;
		private final List<Resource> scripts;

		public TinyEngineSqlResources(String manifestLocation, String database, boolean autoInitialize,
									  Resource manifest, List<Resource> scripts) {
			this.manifestLocation = manifestLocation;
			this.database = database;
			this.autoInitialize = autoInitialize;
			this.manifest = manifest;
			this.scripts = Collections.unmodifiableList(new ArrayList<Resource>(scripts));
		}

		public String getManifestLocation() {
			return manifestLocation;
		}

		public String getDatabase() {
			return database;
		}

		public boolean isAutoInitialize() {
			return autoInitialize;
		}

		public Resource getManifest() {
			return manifest;
		}

		public List<Resource> getScripts() {
			return scripts;
		}
	}
}
