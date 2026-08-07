package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.DefaultTinyEngineExternalCapability;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineExternalCapability;
import com.xiongx9527.fw.plus.tinyengine.model.dto.SchemaCodeParam;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TinyEnginePlatformCodegenMvcApiContractTest {

	private static final String PLATFORM_CONTROLLER =
			"com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEnginePlatformMvcController";
	private static final String CODEGEN_CONTROLLER =
			"com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineCodegenMvcController";

	@Test
	@SuppressWarnings("unchecked")
	void hostCapabilityReceivesEveryOfficialParameterAndResultsAreWrappedOnce() throws Exception {
		RecordingCapability recording = new RecordingCapability();
		MockMvc mvc = mvc(recording.proxy());

		mvc.perform(get("/platform-center/api/courses")
						.param("category", "appDev"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id", is(11)))
				.andExpect(jsonPath("$.data[0].videos[0].video", is("intro.mp4")));
		assertThat(recording.coursesQuery).containsEntry("category", "appDev");

		mvc.perform(get("/platform-center/api/courses/11"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name", is("应用开发")));
		assertThat(recording.courseId).isEqualTo(Integer.valueOf(11));

		MvcResult schemaResult = mvc.perform(post("/app-center/api/schema2code")
						.header("x-lowcode-org", "secret-tenant-key")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"platform\":1,\"app\":2,\"pageInfo\":{\"name\":\"home\",\"schema\":{\"componentName\":\"Page\"}}}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.file", is("Home.vue")))
				.andReturn();
		assertThat(read(recording.schemaCodeRequest, "getPlatform")).isEqualTo(Integer.valueOf(1));
		assertThat(read(recording.schemaCodeRequest, "getApp")).isEqualTo(Integer.valueOf(2));
		assertThat((Map<String, Object>) read(recording.schemaCodeRequest, "getPageInfo"))
				.containsEntry("name", "home");
		assertThat(schemaResult.getResponse().getContentAsString()).doesNotContain("secret-tenant-key");

		MvcResult codeResult = mvc.perform(get("/app-center/api/code")
						.header("x-lowcode-org", "another-secret-key")
						.param("platform", "1")
						.param("app", "2")
						.param("type", "page")
						.param("id", "3")
						.param("history", "4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.file", is("Generated.vue")))
				.andReturn();
		assertThat(read(recording.codeQuery, "getPlatform")).isEqualTo(Integer.valueOf(1));
		assertThat(read(recording.codeQuery, "getApp")).isEqualTo(Integer.valueOf(2));
		assertThat(read(recording.codeQuery, "getType")).isEqualTo("page");
		assertThat(read(recording.codeQuery, "getId")).isEqualTo(Integer.valueOf(3));
		assertThat(read(recording.codeQuery, "getHistory")).isEqualTo(Integer.valueOf(4));
		assertThat(codeResult.getResponse().getContentAsString()).doesNotContain("another-secret-key");

		mvc.perform(get("/app-center/api/source_tpl").param("platform", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].id", is(21)));
		mvc.perform(get("/app-center/api/source_tpl").param("id", "21"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].data.columns[0]", is("name")));
		assertThat(recording.sourceQueries).containsExactly(
				Collections.<String, Object>singletonMap("platform", "1"),
				Collections.<String, Object>singletonMap("id", "21"));
	}

	@Test
	void sourceTemplateRequiresExactlyOneOfficialSelector() throws Exception {
		RecordingCapability recording = new RecordingCapability();
		MockMvc mvc = mvc(recording.proxy());

		mvc.perform(get("/app-center/api/source_tpl"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success", is(false)));
		mvc.perform(get("/app-center/api/source_tpl").param("platform", "1").param("id", "21"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success", is(false)));
		assertThat(recording.sourceQueries).isEmpty();
	}

	@Test
	void defaultCapabilityReturnsDiagnostic501ForEveryExternalRoute() throws Exception {
		MockMvc mvc = mvc(new DefaultTinyEngineExternalCapability());

		assertUnsupported(mvc, get("/platform-center/api/courses"), "courses");
		assertUnsupported(mvc, get("/platform-center/api/courses/11"), "courseDetail");
		assertUnsupported(mvc, post("/app-center/api/schema2code")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"platform\":1,\"app\":2,\"pageInfo\":{}}"), "schemaToCode");
		assertUnsupported(mvc, get("/app-center/api/code")
				.param("platform", "1").param("app", "2").param("type", "page")
				.param("id", "3").param("history", "4"), "generateCode");
		assertUnsupported(mvc, get("/app-center/api/source_tpl").param("platform", "1"), "sourceTemplate");
	}

	@Test
	void externalCapabilityKeepsLegacyImplementationsCompatible() {
		TinyEngineExternalCapability capability = new TinyEngineExternalCapability() {
			@Override
			public Object unsupported(String capability) {
				return capability;
			}
		};

		assertThat(capability.courses(Collections.<String, Object>emptyMap())).isEqualTo("courses");
		assertThat(capability.courseDetail(Integer.valueOf(11))).isEqualTo("courseDetail");
		assertThat(capability.schemaToCode(new SchemaCodeParam())).isEqualTo("schemaToCode");
		assertThat(capability.generateCode(new SchemaCodeParam())).isEqualTo("generateCode");
		assertThat(capability.sourceTemplate(Collections.<String, Object>emptyMap())).isEqualTo("sourceTemplate");
	}

	@Test
	void externalJvmErrorsAreNotConvertedToHttpResponses() {
		TinyEngineExternalCapability capability = new DefaultTinyEngineExternalCapability() {
			@Override
			public Object courses(Map<String, Object> query) {
				throw new AssertionError("boom");
			}
		};

		assertThatThrownBy(() -> new TinyEnginePlatformMvcController(capability)
				.courses(Collections.<String, Object>emptyMap()))
				.isInstanceOf(AssertionError.class)
				.hasMessage("boom");
	}

	private static void assertUnsupported(MockMvc mvc,
			org.springframework.test.web.servlet.RequestBuilder request, String capability) throws Exception {
		mvc.perform(request)
				.andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.code", is("501")))
				.andExpect(jsonPath("$.message", is("TinyEngine capability is not configured: " + capability)));
	}

	private static MockMvc mvc(TinyEngineExternalCapability capability) {
		return MockMvcBuilders.standaloneSetup(controller(PLATFORM_CONTROLLER, capability),
				controller(CODEGEN_CONTROLLER, capability)).build();
	}

	private static Object controller(String className, TinyEngineExternalCapability capability) {
		try {
			Class<?> type = Class.forName(className);
			Constructor<?> constructor = type.getConstructor(TinyEngineExternalCapability.class);
			return constructor.newInstance(capability);
		} catch (Exception ex) {
			throw new AssertionError("Missing TinyEngine MVC controller: " + className, ex);
		}
	}

	private static Object read(Object target, String getter) throws Exception {
		return target.getClass().getMethod(getter).invoke(target);
	}

	private static final class RecordingCapability implements InvocationHandler {

		private Map<String, Object> coursesQuery;
		private Integer courseId;
		private Object schemaCodeRequest;
		private Object codeQuery;
		private final List<Map<String, Object>> sourceQueries = new ArrayList<Map<String, Object>>();

		private TinyEngineExternalCapability proxy() {
			return (TinyEngineExternalCapability) Proxy.newProxyInstance(
					TinyEngineExternalCapability.class.getClassLoader(),
					new Class<?>[]{TinyEngineExternalCapability.class}, this);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object invoke(Object proxy, Method method, Object[] args) {
			if ("courses".equals(method.getName())) {
				coursesQuery = new LinkedHashMap<String, Object>((Map<String, Object>) args[0]);
				Map<String, Object> video = new LinkedHashMap<String, Object>();
				video.put("id", Integer.valueOf(1));
				video.put("title", "介绍");
				video.put("video", "intro.mp4");
				Map<String, Object> course = new LinkedHashMap<String, Object>();
				course.put("id", Integer.valueOf(11));
				course.put("name", "应用开发");
				course.put("videos", Collections.singletonList(video));
				return Collections.singletonList(course);
			}
			if ("courseDetail".equals(method.getName())) {
				courseId = (Integer) args[0];
				Map<String, Object> course = new LinkedHashMap<String, Object>();
				course.put("id", courseId);
				course.put("name", "应用开发");
				return course;
			}
			if ("schemaToCode".equals(method.getName())) {
				schemaCodeRequest = args[0];
				return Collections.<String, Object>singletonMap("file", "Home.vue");
			}
			if ("generateCode".equals(method.getName())) {
				codeQuery = args[0];
				return Collections.<String, Object>singletonMap("file", "Generated.vue");
			}
			if ("sourceTemplate".equals(method.getName())) {
				Map<String, Object> query = new LinkedHashMap<String, Object>((Map<String, Object>) args[0]);
				sourceQueries.add(query);
				Map<String, Object> templateData = new LinkedHashMap<String, Object>();
				templateData.put("columns", Arrays.asList("name", "value"));
				Map<String, Object> template = new LinkedHashMap<String, Object>();
				template.put("id", Integer.valueOf(21));
				template.put("data", templateData);
				return Collections.singletonList(template);
			}
			if ("toString".equals(method.getName())) {
				return "RecordingCapability";
			}
			throw new AssertionError("Unexpected external capability call: " + method.getName());
		}
	}
}
