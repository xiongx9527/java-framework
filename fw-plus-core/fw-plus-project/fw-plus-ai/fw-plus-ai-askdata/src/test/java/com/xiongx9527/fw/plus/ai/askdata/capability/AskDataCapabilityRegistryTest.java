package com.xiongx9527.fw.plus.ai.askdata.capability;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class AskDataCapabilityRegistryTest {

	@Test
	public void shouldRejectDuplicateCapabilityKey() {
		DefaultAskDataCapabilityRegistry registry = new DefaultAskDataCapabilityRegistry("1.0", false);
		registry.register(provider(descriptor("chat", AskDataCapabilityType.MODEL, "1.0", "1.0", "1.0", 10, true)));

		assertIllegalArgument("重复", new Runnable() {
			@Override
			public void run() {
				registry.register(provider(descriptor("chat", AskDataCapabilityType.MODEL, "1.0", "1.0", "1.0", 1, true)));
			}
		});
	}

	@Test
	public void shouldRejectProviderOutsideCoreVersionRange() {
		final DefaultAskDataCapabilityRegistry registry = new DefaultAskDataCapabilityRegistry("1.0", false);

		assertIllegalArgument("不兼容", new Runnable() {
			@Override
			public void run() {
				registry.register(provider(descriptor("future-store", AskDataCapabilityType.STORE, "1.0", "2.0", "2.0", 10, true)));
			}
		});
	}

	@Test
	public void shouldOrderCapabilitiesByDescendingPriorityThenKey() {
		DefaultAskDataCapabilityRegistry registry = new DefaultAskDataCapabilityRegistry("1.0", false);
		registry.register(provider(descriptor("beta", AskDataCapabilityType.MODEL, "1.0", "1.0", "1.0", 10, true)));
		registry.register(provider(descriptor("alpha", AskDataCapabilityType.MODEL, "1.0", "1.0", "1.0", 10, true)));
		registry.register(provider(descriptor("fallback", AskDataCapabilityType.MODEL, "1.0", "1.0", "1.0", 1, true)));

		List<AskDataCapabilityDescriptor> capabilities = registry.list(AskDataCapabilityType.MODEL);

		assertEquals("alpha", capabilities.get(0).getKey());
		assertEquals("beta", capabilities.get(1).getKey());
		assertEquals("fallback", capabilities.get(2).getKey());
	}

	@Test
	public void shouldDescribeAvailableKeysForUnknownCapability() {
		final DefaultAskDataCapabilityRegistry registry = new DefaultAskDataCapabilityRegistry("1.0", false);
		registry.register(provider(descriptor("chat", AskDataCapabilityType.MODEL, "1.0", "1.0", "1.0", 1, true)));

		IllegalArgumentException exception = captureIllegalArgument(new Runnable() {
			@Override
			public void run() {
				registry.get("missing");
			}
		});
		assertTrue(exception.getMessage().contains("missing"));
		assertTrue(exception.getMessage().contains("chat"));
	}

	@Test
	public void shouldRejectClusterIncompatibleCapabilityWhenClusterEnabled() {
		final DefaultAskDataCapabilityRegistry registry = new DefaultAskDataCapabilityRegistry("1.0", true);

		assertIllegalArgument("集群", new Runnable() {
			@Override
			public void run() {
				registry.register(provider(descriptor("local-runtime", AskDataCapabilityType.RUNTIME_ADAPTER,
						"1.0", "1.0", "1.0", 1, false)));
			}
		});
	}

	@Test
	public void shouldAcceptSnapshotCoreVersionWithinReleaseRange() {
		DefaultAskDataCapabilityRegistry registry = new DefaultAskDataCapabilityRegistry("0.1.1-SNAPSHOT", false);
		registry.register(provider(descriptor("chat", AskDataCapabilityType.MODEL, "0.1.1-SNAPSHOT",
				"0.1.1", "0.1.1", 1, true)));

		assertEquals("chat", registry.get("chat").getKey());
	}

	@Test
	public void shouldReturnUnmodifiableCapabilityList() {
		DefaultAskDataCapabilityRegistry registry = new DefaultAskDataCapabilityRegistry("1.0", false);
		registry.register(provider(descriptor("chat", AskDataCapabilityType.MODEL, "1.0", "1.0", "1.0", 1, true)));

		List<AskDataCapabilityDescriptor> capabilities = registry.list(AskDataCapabilityType.MODEL);
		boolean unsupported = false;
		try {
			capabilities.clear();
		} catch (UnsupportedOperationException exception) {
			unsupported = true;
		}
		assertTrue(unsupported, "能力列表必须不可修改");
	}

	@Test
	public void shouldRejectUnsupportedVersionQualifier() {
		assertIllegalArgument("版本格式非法", new Runnable() {
			@Override
			public void run() {
				descriptor("chat", AskDataCapabilityType.MODEL, "1.0", "0.1.1-RC1", "1.0", 1, true);
			}
		});
	}

	@Test
	public void shouldResolveTypedCapabilityInstanceByStableKey() {
		final Runnable capability = new Runnable() {
			@Override
			public void run() {
			}
		};
		DefaultAskDataCapabilityRegistry registry = new DefaultAskDataCapabilityRegistry("1.0", false);
		registry.register(provider(descriptor("business-store", AskDataCapabilityType.STORE, "1.0", "1.0", "1.0",
				10, true, "business.askdata.store"), capability));

		assertEquals(capability, registry.getCapability("business-store", AskDataCapabilityType.STORE, Runnable.class));
		assertEquals("business.askdata.store", registry.get("business-store").getConfigurationNamespace());
	}

	@Test
	public void shouldRejectCapabilityTypeOrImplementationMismatch() {
		DefaultAskDataCapabilityRegistry registry = new DefaultAskDataCapabilityRegistry("1.0", false);
		registry.register(provider(descriptor("business-store", AskDataCapabilityType.STORE, "1.0", "1.0", "1.0",
				10, true, "business.askdata.store"), "not-a-store"));

		assertIllegalArgument("能力类型", new Runnable() {
			@Override
			public void run() {
				registry.getCapability("business-store", AskDataCapabilityType.MODEL, String.class);
			}
		});
		assertIllegalArgument("实现类型", new Runnable() {
			@Override
			public void run() {
				registry.getCapability("business-store", AskDataCapabilityType.STORE, Runnable.class);
			}
		});
	}

	private static AskDataCapabilityProvider provider(final AskDataCapabilityDescriptor descriptor) {
		return provider(descriptor, null);
	}

	private static AskDataCapabilityProvider provider(final AskDataCapabilityDescriptor descriptor,
			final Object capability) {
		return new AskDataCapabilityProvider() {
			@Override
			public AskDataCapabilityDescriptor getDescriptor() {
				return descriptor;
			}

			@Override
			public Object getCapability() {
				return capability == null ? this : capability;
			}
		};
	}

	private static AskDataCapabilityDescriptor descriptor(String key, AskDataCapabilityType type, String version,
			String minimumCoreVersion, String maximumCoreVersion, int priority, boolean clusterCompatible) {
		return new AskDataCapabilityDescriptor(key, type, version, minimumCoreVersion, maximumCoreVersion, priority,
				clusterCompatible);
	}

	private static AskDataCapabilityDescriptor descriptor(String key, AskDataCapabilityType type, String version,
			String minimumCoreVersion, String maximumCoreVersion, int priority, boolean clusterCompatible,
			String configurationNamespace) {
		return new AskDataCapabilityDescriptor(key, type, version, minimumCoreVersion, maximumCoreVersion, priority,
				clusterCompatible, configurationNamespace);
	}

	private static void assertIllegalArgument(String messagePart, Runnable action) {
		captureIllegalArgument(action, messagePart);
	}

	private static IllegalArgumentException captureIllegalArgument(Runnable action) {
		return captureIllegalArgument(action, null);
	}

	private static IllegalArgumentException captureIllegalArgument(Runnable action, String messagePart) {
		try {
			action.run();
			fail("预期抛出 IllegalArgumentException");
		} catch (IllegalArgumentException exception) {
			if (messagePart != null) {
				assertTrue(exception.getMessage().contains(messagePart), "异常信息应包含: " + messagePart);
			}
			return exception;
		}
		throw new AssertionError("fail 应中止测试");
	}
}
