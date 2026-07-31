package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "fw.plus.tiny-engine")
public class TinyEngineProperties {

	private boolean enabled = false;
	private Web web = new Web();
	private Webflux webflux = new Webflux();
	private Security security = new Security();
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

	public Webflux getWebflux() {
		return webflux;
	}

	public void setWebflux(Webflux webflux) {
		this.webflux = webflux;
	}

	public Security getSecurity() {
		return security;
	}

	public void setSecurity(Security security) {
		this.security = security;
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

	public static class Webflux {

		private Scheduler scheduler = new Scheduler();

		public Scheduler getScheduler() {
			return scheduler;
		}

		public void setScheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
		}

		public static class Scheduler {

			private int threadCount = 8;
			private int queueCapacity = 256;
			private Duration timeout = Duration.ofSeconds(30);

			public int getThreadCount() {
				return threadCount;
			}

			public void setThreadCount(int threadCount) {
				this.threadCount = threadCount;
			}

			public int getQueueCapacity() {
				return queueCapacity;
			}

			public void setQueueCapacity(int queueCapacity) {
				this.queueCapacity = queueCapacity;
			}

			public Duration getTimeout() {
				return timeout;
			}

			public void setTimeout(Duration timeout) {
				this.timeout = timeout;
			}
		}
	}

	public static class Security {

		private boolean enabled = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
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
