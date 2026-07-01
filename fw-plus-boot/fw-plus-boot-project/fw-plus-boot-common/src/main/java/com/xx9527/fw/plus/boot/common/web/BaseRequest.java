package com.xx9527.fw.plus.boot.common.web;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public interface BaseRequest {


    Logger log = LoggerFactory.getLogger(BaseRequest.class);

    MyHttpEncryptor encryptor();

    MyHttpEncryptorProperties encryptorProperties();

    /**
     * Parses the decrypted parameters into a map of key-value pairs.
     * This method primarily handles the parsing of a string containing multiple parameters into a map,
     * where the parameters are URL-encoded strings separated by "&".
     *
     * @param decryptedParams The string containing the decrypted parameters, which should include multiple key-value pairs separated by "&".
     * @return Returns a map containing the parsed parameters, where the key is the parameter name, and the value is an array containing the parameter value.
     * @throws UnsupportedEncodingException If the decoding process fails due to unsupported encoding.
     */
    default Map<String, String[]> parseDecryptedParams(String decryptedParams) throws UnsupportedEncodingException {
        // Initialize a map to store the parsed parameters
        Map<String, String[]> params = new HashMap<>();

        // Split the decrypted parameters string by "&" to get each parameter, handling null values
        for (String param : StrUtil.emptyIfNull(decryptedParams).split("&")) {
            // Split each parameter into a key-value pair by "="
            String[] keyValue = param.split("=");

            // If the key-value pair is complete, decode and add it to the map
            if (keyValue.length == 2) {
                // Decode the key and value using URLDecoder and add them to the map
                params.put(URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name()),
                        new String[]{URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name())});
            }
        }

        // Return the map containing all parsed parameters
        return params;
    }


    /**
     * Decrypts a given string.
     *
     * This method aims to decrypt an encrypted string back to its original form. It uses an encryption/decryption object and related properties for the decryption process.
     * If the encryption/decryption object is not null, it attempts to decrypt using the specified algorithm, private key, IV, and salt.
     * If an exception occurs during decryption, it logs the error information. If the encryption/decryption object is null or decryption fails, it returns the original string directly.
     *
     * @param str The string to be decrypted.
     * @return The decrypted string, or the original string if decryption fails.
     */
    default String decrypt(String str) {
        // Check if the encryption/decryption object is not null
        if (null != encryptor()) {
            try {
                // Call the decrypt method of the encryption/decryption object, passing in encryption properties for decryption
                return encryptor().decrypt(encryptorProperties().getAlgorithm(), encryptorProperties().getPrivateKey(),
                        encryptorProperties().getIv(), encryptorProperties().getSalt(), str);
            } catch (Throwable e) {
                // Log any exceptions that occur during decryption
                log.error(e.getMessage(), e);
            }
        }
        // If decryption fails or the encryption/decryption object is null, return the original string directly
        return str;
    }
}
