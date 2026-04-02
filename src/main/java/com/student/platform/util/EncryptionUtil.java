package com.student.platform.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密工具类
 * <p>
 * 提供AES-GCM加密、SHA-256哈希等安全功能
 * 使用AES-256-GCM模式，提供认证加密，防止篡改
 * </p>
 *
 * @author student-platform
 * @since 1.0.0
 */
@Slf4j
public class EncryptionUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // GCM标准IV长度
    private static final int GCM_TAG_LENGTH = 128; // GCM认证标签长度
    private static final int AES_KEY_SIZE = 256; // AES-256

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private EncryptionUtil() {
        // 工具类私有构造方法，防止实例化
        throw new UnsupportedOperationException("工具类不能实例化");
    }

    /**
     * 使用AES-256-GCM加密数据
     *
     * @param data 待加密数据
     * @param key  密钥(Base64编码)
     * @return 加密后的数据(Base64编码，包含IV)
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException         如果加密失败
     */
    public static String encrypt(String data, String key) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("加密数据不能为空");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("密钥不能为空");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);

            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // 将IV和密文组合
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("加密数据失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 使用AES-256-GCM解密数据
     *
     * @param encryptedData 加密数据(Base64编码，包含IV)
     * @param key           密钥(Base64编码)
     * @return 解密后的明文
     * @throws IllegalArgumentException 如果参数无效
     * @throws RuntimeException         如果解密失败或数据被篡改
     */
    public static String decrypt(String encryptedData, String key) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            throw new IllegalArgumentException("解密数据不能为空");
        }
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("密钥不能为空");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);

            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

            // 提取IV和密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密数据失败", e);
            throw new RuntimeException("解密失败，数据可能已被篡改", e);
        }
    }

    /**
     * 生成安全的随机密钥(AES-256)
     *
     * @return Base64编码的密钥
     * @throws RuntimeException 如果密钥生成失败
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
            keyGenerator.init(AES_KEY_SIZE, SECURE_RANDOM);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            log.error("生成密钥失败", e);
            throw new RuntimeException("密钥生成失败", e);
        }
    }

    /**
     * 计算SHA-256哈希值
     *
     * @param data 待哈希数据
     * @return 十六进制格式的哈希值
     * @throws IllegalArgumentException 如果数据为空
     */
    public static String sha256(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("数据不能为空");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256哈希计算失败", e);
            throw new RuntimeException("哈希计算失败", e);
        }
    }

    /**
     * 计算带盐值的SHA-256哈希
     *
     * @param data 待哈希数据
     * @param salt 盐值
     * @return 十六进制格式的哈希值
     */
    public static String sha256WithSalt(String data, String salt) {
        if (salt == null || salt.isEmpty()) {
            throw new IllegalArgumentException("盐值不能为空");
        }
        return sha256(data + salt);
    }

    /**
     * 生成随机盐值
     *
     * @param length 盐值长度(字节)
     * @return Base64编码的盐值
     */
    public static String generateSalt(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
