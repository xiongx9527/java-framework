package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.container;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.config.ClusterConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.service.MagicNotifyListener;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.ssssssss.magicapi.core.model.MagicNotify;
import org.ssssssss.magicapi.core.service.MagicAPIService;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MagicAPIRedisMessageListenerContainerTest {

	@Test
	void shouldDispatchNotifyToListenersAndMagicApiService() {
		ClusterConfig config = new ClusterConfig();
		RecordingMagicAPIService magicAPIService = new RecordingMagicAPIService();
		RecordingNotifyListener listener = new RecordingNotifyListener();
		RecordingContainer container = new RecordingContainer(config, magicAPIService,
				Collections.singletonList(listener));
		MagicNotify notify = new MagicNotify("node-a", "id-1", null, "mcp-change");

		container.init();
		container.listener.onMessage(new Message() {
			@Override
			public byte[] getBody() {
				return JsonUtils.toJsonString(notify).getBytes();
			}

			@Override
			public byte[] getChannel() {
				return config.getChannel().getBytes();
			}
		}, null);

		assertThat(listener.notify).isNotNull();
		assertThat(listener.notify.getType()).isEqualTo("mcp-change");
		assertThat(magicAPIService.notify).isNotNull();
	}

	private static class RecordingContainer extends MagicAPIRedisMessageListenerContainer {

		private MessageListener listener;

		private RecordingContainer(ClusterConfig config, MagicAPIService magicAPIService,
								   List<MagicNotifyListener> listeners) {
			super(config, null, magicAPIService, listeners);
		}

		@Override
		public void setConnectionFactory(RedisConnectionFactory connectionFactory) {
		}

		@Override
		public void addMessageListener(MessageListener listener, org.springframework.data.redis.listener.Topic topic) {
			this.listener = listener;
		}
	}

	private static class RecordingNotifyListener implements MagicNotifyListener {

		private MagicNotify notify;

		@Override
		public void onNotify(MagicNotify notify) {
			this.notify = notify;
		}
	}

	private static class RecordingMagicAPIService implements MagicAPIService {

		private MagicNotify notify;

		@Override
		public <T> T execute(String method, String path, Map<String, Object> parameters) {
			return null;
		}

		@Override
		public <T> T call(String method, String path, Map<String, Object> parameters) {
			return null;
		}

		@Override
		public <T> T invoke(String functionName, Map<String, Object> parameters) {
			return null;
		}

		@Override
		public boolean upload(InputStream inputStream, String mode) throws IOException {
			return false;
		}

		@Override
		public void download(String mode, List<org.ssssssss.magicapi.core.model.SelectedResource> resources,
							 OutputStream outputStream) throws IOException {
		}

		@Override
		public org.ssssssss.magicapi.core.model.JsonBean<?> push(String mode, String headerId, String headerName,
																 List<org.ssssssss.magicapi.core.model.SelectedResource> resources) {
			return null;
		}

		@Override
		public boolean processNotify(MagicNotify magicNotify) {
			this.notify = magicNotify;
			return true;
		}
	}
}
