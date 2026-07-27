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

		assertIllegalArgument("missing", new Runnable() {
			@Override
			public void run() {
				registry.get("missing");
			}
		});
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

	private static AskDataCapabilityProvider provider(final AskDataCapabilityDescriptor descriptor) {
		return new AskDataCapabilityProvider() {
			@Override
			public AskDataCapabilityDescriptor getDescriptor() {
				return descriptor;
			}
		};
	}

	private static AskDataCapabilityDescriptor descriptor(String key, AskDataCapabilityType type, String version,
			String minimumCoreVersion, String maximumCoreVersion, int priority, boolean clusterCompatible) {
		return new AskDataCapabilityDescriptor(key, type, version, minimumCoreVersion, maximumCoreVersion, priority,
				clusterCompatible);
	}

	private static void assertIllegalArgument(String messagePart, Runnable action) {
		try {
			action.run();
			fail("预期抛出 IllegalArgumentException");
		} catch (IllegalArgumentException exception) {
			assertTrue(exception.getMessage().contains(messagePart), "异常信息应包含: " + messagePart);
		}
	}
}
