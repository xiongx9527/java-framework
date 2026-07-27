package com.xiongx9527.fw.plus.tinyengine.support.sql;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Reads the packaged Tiny Engine SQL scripts without executing them.
 */
public class TinyEngineSqlScriptManifest {

	private static final String MANIFEST_RESOURCE = "META-INF/tiny-engine/sql/manifest.properties";

	private final Properties properties;

	public TinyEngineSqlScriptManifest() {
		properties = loadProperties();
	}

	public List<String> getScripts(String database) {
		if (database == null || database.trim().isEmpty()) {
			return Collections.emptyList();
		}
		String prefix = database.trim() + ".";
		List<String> keys = new ArrayList<String>();
		for (Object key : properties.keySet()) {
			String propertyName = String.valueOf(key);
			if (propertyName.startsWith(prefix)) {
				keys.add(propertyName);
			}
		}
		Collections.sort(keys);
		List<String> scripts = new ArrayList<String>(keys.size());
		for (String key : keys) {
			scripts.add(properties.getProperty(key));
		}
		return Collections.unmodifiableList(scripts);
	}

	private Properties loadProperties() {
		InputStream inputStream = TinyEngineSqlScriptManifest.class.getClassLoader().getResourceAsStream(MANIFEST_RESOURCE);
		if (inputStream == null) {
			throw new IllegalStateException("Tiny Engine SQL manifest does not exist: " + MANIFEST_RESOURCE);
		}
		Properties loadedProperties = new Properties();
		try {
			loadedProperties.load(inputStream);
			return loadedProperties;
		} catch (IOException exception) {
			throw new IllegalStateException("Unable to read Tiny Engine SQL manifest: " + MANIFEST_RESOURCE, exception);
		} finally {
			try {
				inputStream.close();
			} catch (IOException ignored) {
				// Nothing to do when the classpath resource has already been read.
			}
		}
	}
}
