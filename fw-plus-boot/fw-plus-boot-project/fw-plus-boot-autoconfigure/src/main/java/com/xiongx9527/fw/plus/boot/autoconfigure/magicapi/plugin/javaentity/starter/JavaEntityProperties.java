package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "magic-api.plugin.java-entity")
public class JavaEntityProperties {

    private long maxSourceBytes = 262144L;
    private long maxTotalSourceBytes = 2097152L;
    private Duration compileTimeout = Duration.ofSeconds(30L);
    private int maxDiagnostics = 100;
    private int maxDiagnosticMessageLength = 500;

    public long getMaxSourceBytes() {
        return maxSourceBytes;
    }

    public void setMaxSourceBytes(long maxSourceBytes) {
        this.maxSourceBytes = maxSourceBytes;
    }

    public long getMaxTotalSourceBytes() {
        return maxTotalSourceBytes;
    }

    public void setMaxTotalSourceBytes(long maxTotalSourceBytes) {
        this.maxTotalSourceBytes = maxTotalSourceBytes;
    }

    public Duration getCompileTimeout() {
        return compileTimeout;
    }

    public void setCompileTimeout(Duration compileTimeout) {
        this.compileTimeout = compileTimeout;
    }

    public int getMaxDiagnostics() {
        return maxDiagnostics;
    }

    public void setMaxDiagnostics(int maxDiagnostics) {
        this.maxDiagnostics = maxDiagnostics;
    }

    public int getMaxDiagnosticMessageLength() {
        return maxDiagnosticMessageLength;
    }

    public void setMaxDiagnosticMessageLength(int maxDiagnosticMessageLength) {
        this.maxDiagnosticMessageLength = maxDiagnosticMessageLength;
    }
}
