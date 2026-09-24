package com.xiongx9527.fw.plus.boot.ws.sample;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import io.jsonwebtoken.Jwts;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 演示用 token 签发接口。
 *
 * <p>握手要求 query 参数 {@code token} 是合法的 JWT，其 {@code subject} 即 appId。
 * 本接口复用地基的密钥与算法选择逻辑（{@link WsBaseService#getSecretKeyAndSignatureAlgorithm()}）
 * 按 {@code fw.plus.ws.token.secret} 签发 token，方便本地联调：
 * {@code curl 'http://localhost:8080/demo/token?subject=default'}。</p>
 *
 * @author zzy
 */
@RestController
public class DemoTokenController {

	private final WsBaseService tokenFactory;

	public DemoTokenController(WsConfig wsConfig) {
		this.tokenFactory = new WsBaseService() {
			@Override
			public Map<String, Object> getConfig() {
				return wsConfig.flatten();
			}
		};
	}

	/**
	 * 按当前配置的密钥签发一个握手用 token。
	 *
	 * @param subject token 的 subject，也是握手后的 appId
	 * @return 包含 subject 与 token 的响应
	 */
	@GetMapping("/demo/token")
	public Map<String, String> issueToken(@RequestParam(defaultValue = "default") String subject) {
		WsBaseService.SecretKeyAndSignatureAlgorithm key = tokenFactory.getSecretKeyAndSignatureAlgorithm();
		String token = Jwts.builder()
				.subject(subject)
				.signWith(key.getSecretKey())
				.compact();
		Map<String, String> result = new LinkedHashMap<>(4);
		result.put("subject", subject);
		result.put("token", token);
		return result;
	}
}
