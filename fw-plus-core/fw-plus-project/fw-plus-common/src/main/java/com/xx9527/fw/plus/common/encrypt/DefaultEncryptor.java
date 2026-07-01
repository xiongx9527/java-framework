package com.xx9527.fw.plus.common.encrypt;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.symmetric.*;
import com.xx9527.fw.plus.common.mate.annotation.Algorithm;
import com.xx9527.fw.plus.common.mate.annotation.FieldEncrypt;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.SM2;
import cn.hutool.crypto.digest.SM3;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * 默认的加密器实现类，实现了FieldEncrypt.IEncryptor接口
 */
public class DefaultEncryptor implements FieldEncrypt.IEncryptor {

    /**
     * 默认构造函数，用于创建DefaultEncryptor实例。
     * 该构造函数不接受任何参数，使用默认配置初始化加密器。
     */
    public DefaultEncryptor() {
    }


    /**
     * 加密数据
     *
     * 根据指定的算法、密钥、IV（初始化向量）和盐值对数据进行加密
     * 支持的算法包括MD5、SM3、SM2、RSA以及对称加密算法
     *
     * @param algorithm 加密算法枚举，指定了使用哪种算法进行加密
     * @param key 密钥字符串，用于对称加密算法
     * @param iv 初始化向量字符串，用于某些对称加密算法
     * @param salt 盐值字符串，用于增加加密的安全性
     * @param data 要加密的数据对象
     * @return 加密后的字符串
     * @throws Exception 如果加密过程中发生错误，则抛出异常
     */
    @Override
    public String encrypt(Algorithm algorithm, String key, String iv, String salt, Object data) throws Exception {
        // 将盐值转换为字节数组，如果盐值为null，则不进行转换
        byte[] saltBytes = salt != null ? salt.getBytes(StandardCharsets.UTF_8) : null;
        // 将数据对象转换为字符串形式，准备进行加密
        String plainText = data.toString();
        // 根据不同的算法执行相应的加密操作
        switch (algorithm) {
            case BASE64:
                // 如果是Base64编码，则直接返回原始数据
                return Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8));
            case MD5_32:
                // 使用MD5算法进行加密，生成32位的哈希值
                return DigestUtil.md5Hex(plainText);
            case MD5_16:
                // 使用MD5算法进行加密，生成16位的哈希值
                return DigestUtil.md5Hex16(plainText);
            case SM3:
                // 使用SM3算法进行加密，如果提供了盐值，则使用盐值进行加密
                return salt != null? new SM3(saltBytes).digestHex(plainText) : SM3.create().digestHex(plainText);
            case SM2:
                // SM2 加密需要公钥
                PublicKey publicKey = SecureUtil.generatePublicKey("SM2", key.getBytes(StandardCharsets.UTF_8));
                SM2 sm2 = new SM2(null,publicKey);
                // 使用SM2算法进行加密，返回加密后的字符串
                return new String(sm2.encrypt(plainText.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            case RSA:
                // RSA 加密需要公钥
                PublicKey rsaPublicKey = SecureUtil.generatePublicKey("RSA", key.getBytes(StandardCharsets.UTF_8));
                RSA rsa = new RSA(null, rsaPublicKey);
                // 使用RSA算法进行加密，返回加密后的字符串
                return new String(rsa.encrypt(plainText.getBytes(StandardCharsets.UTF_8), KeyType.PublicKey), StandardCharsets.UTF_8);
            default:
                // 对于其他对称加密算法，获取相应的加密器并进行加密
                SymmetricCrypto crypto = generateSymmetricCrypto(algorithm, key, iv);
                byte[] bytes = crypto.encrypt(plainText.getBytes());
                return HexUtil.encodeHexStr(bytes);
        }
    }

    /**
     * 根据指定的算法、密钥、IV、盐和加密数据来解密字符串
     *
     * @param algorithm 加密算法枚举，决定了使用哪种加密算法进行解密
     * @param key 加密密钥，用于解密数据
     * @param iv 初始化向量，某些加密算法（如CBC模式）需要
     * @param salt 盐值，用于增加加密算法的安全性
     * @param encryptedData 加密后的数据，可以是任何类型，但通常为字符串
     * @return 解密后的字符串
     * @throws Exception 如果解密过程中发生错误，则抛出异常
     */
    @Override
    public String decrypt(Algorithm algorithm, String key, String iv, String salt, Object encryptedData) throws Exception {
        // 将加密数据转换为字符串形式
        String encryptedText = encryptedData.toString();
        // 根据不同的加密算法执行相应的解密操作
        switch (algorithm) {
            case BASE64:
                return new String(Base64.getDecoder().decode(encryptedText),StandardCharsets.UTF_8);
            case MD5_32:
            case MD5_16:
            case SM3:
                // MD5和SM3是哈希算法，不支持解密操作，因此抛出异常
                throw new IllegalArgumentException("MD5 and SM3 is a hash algorithm and does not support decryption.");
            case SM2:
                // SM2解密需要使用私钥
                PrivateKey privateKey = SecureUtil.generatePrivateKey("SM2", key.getBytes(StandardCharsets.UTF_8));
                SM2 sm2 = new SM2(privateKey,null);
                // 使用SM2私钥解密数据并返回解密后的字符串
                return new String(sm2.decrypt(encryptedText.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            case RSA:
                // RSA解密需要使用私钥
                PrivateKey rsaPrivateKey = SecureUtil.generatePrivateKey("RSA", key.getBytes(StandardCharsets.UTF_8));
                RSA rsa = new RSA(rsaPrivateKey,null);
                // 使用RSA私钥解密数据并返回解密后的字符串
                return new String(rsa.decrypt(encryptedText.getBytes(StandardCharsets.UTF_8), KeyType.PrivateKey), StandardCharsets.UTF_8);
            default:
                // 对于其他对称加密算法，获取相应的加密器实例并进行解密操作
                SymmetricCrypto crypto = generateSymmetricCrypto(algorithm, key, iv);
                // 使用对称加密器解密数据并返回解密后的字符串
                byte[] bytes = HexUtil.decodeHex(encryptedText);
                return new String(crypto.decrypt(bytes));
        }
    }

    /**
     * 生成对称加密器
     *
     * 根据提供的算法、密钥和初始化向量（iv）生成一个对称加密器对象
     * 如果算法是PBE（Password-Based Encryption）类型，则使用PBE密钥生成算法
     * 否则，直接使用UTF-8编码的密钥字节创建对称加密器
     * 如果提供了初始化向量（iv），则将其设置到加密器中
     *
     * @param algorithm 加密算法名称
     * @param key 加密密钥
     * @param iv 初始化向量
     * @return SymmetricCrypto对象，用于执行对称加密和解密
     * @throws Exception 如果生成对称加密器过程中发生错误
     */
    private static SymmetricCrypto generateSymmetricCrypto(String algorithm, String key, String iv) throws Exception {
        SymmetricCrypto crypto = null;
        // 根据算法类型生成密钥
        if(algorithm.toUpperCase().startsWith("PBE")) {
            // 对于PBE算法，使用密钥生成PBE密钥对象
            SecretKey pbeKey = KeyUtil.generatePBEKey(algorithm, key.toCharArray());
            crypto = new SymmetricCrypto(algorithm, pbeKey);
        } else {
            // 对于其他算法，直接使用UTF-8编码的密钥创建对称加密器
            crypto = new SymmetricCrypto(algorithm, key.getBytes(StandardCharsets.UTF_8));
        }
        // 设置初始化向量（如果提供）
        if(iv != null) {
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            crypto.setIv(ivSpec);
        }
        return crypto;
    }

    /**
     * 获取对称加密器实例
     *
     * @param algorithm 加密算法枚举，用于选择加密算法
     * @param key        加密密钥，用于加密和解密数据
     * @param iv         初始化向量，用于增强加密安全性
     * @return SymmetricCrypto 对称加密器实例
     * @throws Exception 如果算法不受支持或加密密钥无效，则抛出异常
     */
    private SymmetricCrypto generateSymmetricCrypto(Algorithm algorithm, String key, String iv) throws Exception {
        // 根据提供的算法选择合适的对称加密器实例
        switch (algorithm) {
            case AES:
                // 返回AES算法的对称加密器实例
                return generateSymmetricCrypto(SymmetricAlgorithm.AES.getValue(), key,iv);
            case DES:
                // 返回DES算法的对称加密器实例
                return generateSymmetricCrypto(SymmetricAlgorithm.DES.getValue(), key,iv);
            case SM4:
                // 返回SM4算法的对称加密器实例
                return generateSymmetricCrypto(SM4.ALGORITHM_NAME, key,iv);
            case PBEWithMD5AndDES:
                // 返回PBEWithMD5AndDES算法的对称加密器实例
                return generateSymmetricCrypto(SymmetricAlgorithm.PBEWithMD5AndDES.getValue(), key,iv);
            case PBEWithSHA1AndDESede:
                // 返回PBEWithSHA1AndDESede算法的对称加密器实例
                return generateSymmetricCrypto(SymmetricAlgorithm.PBEWithSHA1AndDESede.getValue(), key,iv);
            case PBEWithSHA1AndRC2_40:
                // 返回PBEWithSHA1AndRC2_40算法的对称加密器实例
                return generateSymmetricCrypto(SymmetricAlgorithm.PBEWithSHA1AndRC2_40.getValue(), key,iv);
            case RSA:
            case BASE64:
                // 抛出异常，因为RSA和BASE64不是对称加密算法
                throw new IllegalArgumentException("Unsupported symmetric algorithm: " + algorithm);
            default:
                // 如果算法不受支持，抛出异常
                throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }
}
