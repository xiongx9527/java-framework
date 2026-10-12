package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.common;

import net.dreamlu.iot.mqtt.codec.MqttQoS;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttClientConfigSupportTest {

    @Test
    void parsePortTrimsValueAndFallsBackToDefaultMqttPort() {
        assertThat(MqttClientConfigSupport.parsePort("1884")).isEqualTo(1884);
        assertThat(MqttClientConfigSupport.parsePort(" 1884 ")).isEqualTo(1884);
        assertThat(MqttClientConfigSupport.parsePort("bad")).isEqualTo(1883);
        assertThat(MqttClientConfigSupport.parsePort("0")).isEqualTo(1883);
        assertThat(MqttClientConfigSupport.parsePort("65536")).isEqualTo(1883);
        assertThat(MqttClientConfigSupport.parsePort(null)).isEqualTo(1883);
    }

    @Test
    void parseQosTrimsValueSupportsZeroOneTwoAndFallsBackToZero() {
        assertThat(MqttClientConfigSupport.parseQos("0")).isEqualTo(MqttQoS.QOS0);
        assertThat(MqttClientConfigSupport.parseQos("1")).isEqualTo(MqttQoS.QOS1);
        assertThat(MqttClientConfigSupport.parseQos(" 2 ")).isEqualTo(MqttQoS.QOS2);
        assertThat(MqttClientConfigSupport.parseQos("bad")).isEqualTo(MqttQoS.QOS0);
        assertThat(MqttClientConfigSupport.parseQos(null)).isEqualTo(MqttQoS.QOS0);
    }

    @Test
    void buildSubscribeClientIdKeepsBaseReadableAndAddsTopicSuffix() {
        String clientId = MqttClientConfigSupport.buildSubscribeClientId("device-client", "iot", "device/+/status");

        assertThat(clientId).startsWith("device-client-sub-");
        assertThat(clientId).contains("device___status");
        assertThat(clientId.length()).isLessThanOrEqualTo(64);
    }

    @Test
    void buildSubscribeClientIdFallsBackToDataSourceKeyWhenBaseIsBlank() {
        String clientId = MqttClientConfigSupport.buildSubscribeClientId("", "iot", "device/a/status");

        assertThat(clientId).startsWith("iot-sub-");
        assertThat(clientId.length()).isLessThanOrEqualTo(64);
    }
}
