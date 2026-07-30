package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fw.plus.tiny-engine")
public class TinyEngineProperties {

	private boolean enabled = false;
	private Web web = new Web();
	private Service service = new Service();
	private Mapper mapper = new Mapper();
	private Resources resources = new Resources();
	private Sql sql = new Sql();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Web getWeb() {
		return web;
	}

	public void setWeb(Web web) {
		this.web = web;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public Mapper getMapper() {
		return mapper;
	}

	public void setMapper(Mapper mapper) {
		this.mapper = mapper;
	}

	public Resources getResources() {
		return resources;
	}

	public void setResources(Resources resources) {
		this.resources = resources;
	}

	public Sql getSql() {
		return sql;
	}

	public void setSql(Sql sql) {
		this.sql = sql;
	}

	public static class Web {

		private boolean enabled = true;
		private String basePath = "";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getBasePath() {
			return basePath;
		}

		public void setBasePath(String basePath) {
			this.basePath = basePath;
		}
	}

	public static class Service {

		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	public static class Mapper {

		private boolean enabled = true;
		private String basePackage = "com.xiongx9527.fw.plus.tinyengine.mapper";
		private String[] mapperLocations = new String[] {"classpath*:mapper/tinyengine/**/*.xml"};

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getBasePackage() {
			return basePackage;
		}

		public void setBasePackage(String basePackage) {
			this.basePackage = basePackage;
		}

		public String[] getMapperLocations() {
			return mapperLocations;
		}

		public void setMapperLocations(String[] mapperLocations) {
			this.mapperLocations = mapperLocations;
		}
	}

	public static class Resources {

		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

	public static class Sql {

		private boolean enabled = true;
		private boolean autoInitialize = false;
		private String database = "mysql";
		private String manifestLocation = "classpath:META-INF/tiny-engine/sql/manifest.properties";

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isAutoInitialize() {
			return autoInitialize;
		}

		public void setAutoInitialize(boolean autoInitialize) {
			this.autoInitialize = autoInitialize;
		}

		public String getDatabase() {
			return database;
		}

		public void setDatabase(String database) {
			this.database = database;
		}

		public String getManifestLocation() {
			return manifestLocation;
		}

		public void setManifestLocation(String manifestLocation) {
			this.manifestLocation = manifestLocation;
		}
	}
}
