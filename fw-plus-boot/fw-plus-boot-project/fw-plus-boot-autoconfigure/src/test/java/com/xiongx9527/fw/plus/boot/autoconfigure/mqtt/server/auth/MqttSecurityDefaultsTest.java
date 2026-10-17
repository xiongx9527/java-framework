package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.auth;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.properties.FwPlusMqttServerSecurityProperties;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.spring.server.config.MqttServerProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttSecurityDefaultsTest {

	@Test
	void authRejectsBlankClientId() {
		MqttAuthHandler handler = new MqttAuthHandler(new MqttServerProperties(),
				new FwPlusMqttServerSecurityProperties());

		boolean authenticated = handler.authenticate(null, null, "", null, null);

		assertThat(authenticated).isFalse();
	}

	@Test
	void authAllowsAnonymousOnlyWhenMicaAuthDisabledAndAnonymousAllowed() {
		MqttServerProperties properties = new MqttServerProperties();
		properties.getAuth().setEnable(false);
		FwPlusMqttServerSecurityProperties security = new FwPlusMqttServerSecurityProperties();
		security.setAllowAnonymousWhenAuthDisabled(true);
		MqttAuthHandler handler = new MqttAuthHandler(properties, security);

		boolean authenticated = handler.authenticate(null, null, "client-a", null, null);

		assertThat(authenticated).isTrue();
	}

	@Test
	void authRejectsWhenMicaAuthEnabledAndCredentialsDoNotMatch() {
		MqttServerProperties properties = new MqttServerProperties();
		properties.getAuth().setEnable(true);
		properties.getAuth().setUsername("user");
		properties.getAuth().setPassword("secret");
		MqttAuthHandler handler = new MqttAuthHandler(properties, new FwPlusMqttServerSecurityProperties());

		boolean authenticated = handler.authenticate(null, null, "client-a", "user", "bad");

		assertThat(authenticated).isFalse();
	}

	@Test
	void authAllowsWhenMicaAuthEnabledAndCredentialsMatch() {
		MqttServerProperties properties = new MqttServerProperties();
		properties.getAuth().setEnable(true);
		properties.getAuth().setUsername("user");
		properties.getAuth().setPassword("secret");
		MqttAuthHandler handler = new MqttAuthHandler(properties, new FwPlusMqttServerSecurityProperties());

		boolean authenticated = handler.authenticate(null, null, "client-a", "user", "secret");

		assertThat(authenticated).isTrue();
	}

	@Test
	void publishPermissionRejectsBlankClientIdBlankTopicAndWildcardTopic() {
		MqttPublishPermission permission = new MqttPublishPermission(new FwPlusMqttServerSecurityProperties());

		assertThat(permission.hasPermission(null, "", "sensor/temp", MqttQoS.QOS0, false)).isFalse();
		assertThat(permission.hasPermission(null, "client-a", "", MqttQoS.QOS0, false)).isFalse();
		assertThat(permission.hasPermission(null, "client-a", "sensor/+", MqttQoS.QOS0, false)).isFalse();
		assertThat(permission.hasPermission(null, "client-a", "sensor/#", MqttQoS.QOS0, false)).isFalse();
	}

	@Test
	void publishPermissionCanBeDisabled() {
		FwPlusMqttServerSecurityProperties security = new FwPlusMqttServerSecurityProperties();
		security.setPublishAuthEnabled(false);
		MqttPublishPermission permission = new MqttPublishPermission(security);

		assertThat(permission.hasPermission(null, "", "", MqttQoS.QOS0, false)).isTrue();
	}

	@Test
	void subscribeValidatorRejectsBlankClientIdBlankFilterAndInvalidWildcard() {
		MqttSubscribeValidator validator = new MqttSubscribeValidator(new FwPlusMqttServerSecurityProperties());

		assertThat(validator.isValid(null, "", "sensor/temp", MqttQoS.QOS0)).isFalse();
		assertThat(validator.isValid(null, "client-a", "", MqttQoS.QOS0)).isFalse();
		assertThat(validator.isValid(null, "client-a", "sensor/#/bad", MqttQoS.QOS0)).isFalse();
		assertThat(validator.isValid(null, "client-a", "sensor/te+mp", MqttQoS.QOS0)).isFalse();
		assertThat(validator.isValid(null, "client-a", "sensor/+", MqttQoS.QOS0)).isTrue();
	}

	@Test
	void subscribeValidatorCanBeDisabled() {
		FwPlusMqttServerSecurityProperties security = new FwPlusMqttServerSecurityProperties();
		security.setSubscribeAuthEnabled(false);
		MqttSubscribeValidator validator = new MqttSubscribeValidator(security);

		assertThat(validator.isValid(null, "", "", MqttQoS.QOS0)).isTrue();
	}
}
