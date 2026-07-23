package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "magic-api.plugin.s3.platform-options")
public class MagicS3PlatformOptionProperties {

	private List<MagicS3PlatformCandidate> platformCandidates = new ArrayList<MagicS3PlatformCandidate>();

	public List<MagicS3PlatformCandidate> getPlatformCandidates() {
		return platformCandidates;
	}

	public void setPlatformCandidates(List<MagicS3PlatformCandidate> platformCandidates) {
		this.platformCandidates = platformCandidates == null
				? new ArrayList<MagicS3PlatformCandidate>()
				: platformCandidates;
	}
}
