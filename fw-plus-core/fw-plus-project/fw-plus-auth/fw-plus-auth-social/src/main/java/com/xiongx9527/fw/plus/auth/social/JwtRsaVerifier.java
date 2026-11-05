package com.xiongx9527.fw.plus.auth.social;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * OIDC id_token 校验（RS256 签名 + iss/aud/exp/nonce）。
 *
 * <p>公钥来源：配置的 jwks-url（远程拉取，按 kid 匹配）或 id-token-cert（X.509 PEM）。
 * 缺失校验材料、alg 非 RS256、签名不符、issuer/audience 不匹配、已过期或 nonce 不一致时一律拒绝，
 * 避免仅凭 token 响应体就建立会话。</p>
 */
public final class JwtRsaVerifier {

    private JwtRsaVerifier() {
    }

    public static Map<String, Object> verify(String idToken, IdpConfig cfg, String expectedNonce) {
        if (idToken == null || idToken.trim().isEmpty()) {
            throw new IllegalStateException("oidc token response has no id_token");
        }
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new IllegalStateException("oidc id_token is not a compact JWS");
        }
        Map<String, Object> header = SocialHttpUtils.parseJsonMap(decode(parts[0]));
        Map<String, Object> claims = SocialHttpUtils.parseJsonMap(decode(parts[1]));
        String alg = str(header, "alg");
        if (!"RS256".equals(alg)) {
            throw new IllegalStateException("oidc id_token alg is not supported: " + alg);
        }
        PublicKey key = publicKey(cfg, str(header, "kid"));
        if (!verifySignature(key, parts[0] + "." + parts[1], parts[2])) {
            throw new IllegalStateException("oidc id_token signature is invalid");
        }
        assertIssuer(cfg, str(claims, "iss"));
        assertAudience(cfg, claims.get("aud"));
        assertNotExpired(claims.get("exp"));
        if (expectedNonce != null && !expectedNonce.isEmpty()
                && !expectedNonce.equals(str(claims, "nonce"))) {
            throw new IllegalStateException("oidc id_token nonce does not match the authorization request");
        }
        return claims;
    }

    private static String decode(String part) {
        return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
    }

    private static PublicKey publicKey(IdpConfig cfg, String kid) {
        String jwksUrl = cfg.extra("jwks-url");
        if (jwksUrl != null && !jwksUrl.isEmpty()) {
            String body = SocialHttpUtils.get(jwksUrl, null);
            Object rawKeys = SocialHttpUtils.parseJsonMap(body).get("keys");
            if (!(rawKeys instanceof List)) {
                throw new IllegalStateException("oidc jwks response has no keys array");
            }
            for (Object item : (List<?>) rawKeys) {
                if (!(item instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> jwk = (Map<String, Object>) item;
                if (kid != null && !kid.equals(str(jwk, "kid"))) {
                    continue;
                }
                String modulus = str(jwk, "n");
                String exponent = str(jwk, "e");
                if (modulus == null || exponent == null) {
                    continue;
                }
                try {
                    BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(modulus));
                    BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(exponent));
                    return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
                } catch (Exception ex) {
                    throw new IllegalStateException("oidc jwks key is invalid: " + ex.getMessage(), ex);
                }
            }
            throw new IllegalStateException("oidc jwks has no usable key for kid " + kid);
        }
        String certText = cfg.extra("id-token-cert");
        if (certText == null || certText.isEmpty()) {
            throw new IllegalStateException("oidc id_token verification requires jwks-url or id-token-cert");
        }
        try {
            String base64 = certText.replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
            return certificate.getPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException("oidc id-token-cert is invalid: " + e.getMessage(), e);
        }
    }

    private static boolean verifySignature(PublicKey key, String signingInput, String signature) {
        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getUrlDecoder().decode(signature));
        } catch (Exception e) {
            return false;
        }
    }

    private static void assertIssuer(IdpConfig cfg, String issuer) {
        String expected = cfg.extra("issuer");
        if (expected == null || expected.isEmpty()) {
            return;
        }
        if (!expected.equals(issuer)) {
            throw new IllegalStateException("oidc id_token issuer mismatch: " + issuer);
        }
    }

    private static void assertAudience(IdpConfig cfg, Object audience) {
        String clientId = cfg.getClientId();
        if (clientId == null || clientId.isEmpty() || audience == null) {
            return;
        }
        if (audience instanceof List) {
            for (Object item : (List<?>) audience) {
                if (clientId.equals(String.valueOf(item))) {
                    return;
                }
            }
            throw new IllegalStateException("oidc id_token audience does not contain the client id");
        }
        if (!clientId.equals(String.valueOf(audience))) {
            throw new IllegalStateException("oidc id_token audience does not match the client id");
        }
    }

    private static void assertNotExpired(Object exp) {
        if (exp == null) {
            throw new IllegalStateException("oidc id_token has no exp claim");
        }
        long expiresAt = ((Number) exp).longValue() * 1000L;
        if (System.currentTimeMillis() > expiresAt) {
            throw new IllegalStateException("oidc id_token expired");
        }
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        return v == null ? null : String.valueOf(v);
    }
}
