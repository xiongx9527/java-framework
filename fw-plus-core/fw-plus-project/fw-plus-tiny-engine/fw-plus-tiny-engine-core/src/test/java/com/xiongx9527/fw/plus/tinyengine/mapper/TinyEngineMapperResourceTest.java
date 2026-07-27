package com.xiongx9527.fw.plus.tinyengine.mapper;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TinyEngineMapperResourceTest {

	private static final String MAPPER_RESOURCE_ROOT = "mapper/tinyengine/";
	private static final String MAPPER_PACKAGE = "com.xiongx9527.fw.plus.tinyengine.mapper.";
	private static final String LEGACY_PACKAGE = "com.tiny" + "engine.it";
	private static final String LEGACY_JSON_TYPE_HANDLER = "Jackson" + "TypeHandler";
	private static final String[] MAPPER_NAMES = {
		"AppExtensionMapper", "AppMapper", "AuthUsersUnitsRolesMapper", "BlockCarriersRelationMapper",
		"BlockGroupBlockMapper", "BlockGroupMapper", "BlockHistoryMapper", "BlockMapper",
		"BusinessCategoryMapper", "ComponentLibraryMapper", "ComponentMapper", "DatasourceMapper",
		"I18nEntryMapper", "I18nLangMapper", "MaterialHistoryMapper", "MaterialMapper", "ModelMapper",
		"PageHistoryMapper", "PageMapper", "PageTemplateMapper", "PlatformHistoryMapper", "PlatformMapper",
		"ResourceGroupMapper", "ResourceGroupResourceMapper", "ResourceMapper", "TaskRecordMapper",
		"TenantMapper", "UserMapper"
	};

	@Test
	public void shouldPackageOneXmlForEveryMapperInterface() throws Exception {
		for (String mapperName : MAPPER_NAMES) {
			Class.forName(MAPPER_PACKAGE + mapperName);
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MAPPER_RESOURCE_ROOT + mapperName + ".xml");
			assertNotNull(inputStream, mapperName + " 的 XML 资源不存在");
			inputStream.close();
		}
	}

	@Test
	public void shouldKeepMapperXmlNamespacesAndTypeReferencesInsideTinyEngineCore() throws Exception {
		int resourceCount = 0;
		for (String mapperName : MAPPER_NAMES) {
			String resourceName = MAPPER_RESOURCE_ROOT + mapperName + ".xml";
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
			assertNotNull(inputStream, resourceName + " 不存在");
			String content = new String(readAll(inputStream), StandardCharsets.UTF_8);
			assertFalse(content.contains(LEGACY_PACKAGE), resourceName + " 不得保留源项目包名");
			assertFalse(content.contains(LEGACY_JSON_TYPE_HANDLER), resourceName + " 必须使用 Tiny Engine JSON TypeHandler");

			Document document = parse(resourceName);
			Element mapper = document.getDocumentElement();
			assertEquals(MAPPER_PACKAGE + mapperName, mapper.getAttribute("namespace"));
			assertTypeReferencesUseTinyEnginePackages(resourceName, document);
			resourceCount++;
		}
		assertEquals(MAPPER_NAMES.length, resourceCount, "Mapper XML 数量必须与接口数量一致");
	}

	private Document parse(String resourceName) throws Exception {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
		assertNotNull(inputStream, resourceName + " 不存在");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		try {
			return factory.newDocumentBuilder().parse(inputStream);
		} finally {
			inputStream.close();
		}
	}

	private void assertTypeReferencesUseTinyEnginePackages(String resourceName, Document document) {
		NodeList elements = document.getElementsByTagName("*");
		for (int index = 0; index < elements.getLength(); index++) {
			Element element = (Element) elements.item(index);
			assertTypeReference(resourceName, element, "type");
			assertTypeReference(resourceName, element, "parameterType");
			assertTypeReference(resourceName, element, "resultType");
			assertTypeReference(resourceName, element, "javaType");
			assertTypeReference(resourceName, element, "typeHandler");
		}
	}

	private void assertTypeReference(String resourceName, Element element, String attributeName) {
		String value = element.getAttribute(attributeName);
		if (value.startsWith("com.")) {
			assertTrue(value.startsWith("com.xiongx9527.fw.plus.tinyengine."),
				resourceName + " 的 " + attributeName + " 必须指向 Tiny Engine 新包名: " + value);
		}
	}

	private byte[] readAll(InputStream inputStream) throws Exception {
		java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int count;
		while ((count = inputStream.read(buffer)) >= 0) {
			outputStream.write(buffer, 0, count);
		}
		inputStream.close();
		return outputStream.toByteArray();
	}
}
