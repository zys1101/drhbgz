package com.example.demo.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 加解密工具（对应需求 3.3.3 数据保密性：后端 secretUtil 相关加密解密）
 * 密码存储格式：salt$sha256(salt + 明文)
 */
public class SecretUtil {

    /** 默认盐值 */
    private static final String SALT = "nep";

    private SecretUtil() {
    }

    /** 计算 sha256 十六进制摘要 */
    public static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    /** 加密：生成可存储的密码串 */
    public static String encrypt(String raw) {
        return SALT + "$" + sha256(SALT + raw);
    }

    /** 校验：raw 明文是否与存储串匹配 */
    public static boolean verify(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        int idx = stored.indexOf('$');
        if (idx <= 0) {
            return false;
        }
        String salt = stored.substring(0, idx);
        return (salt + "$" + sha256(salt + raw)).equals(stored);
    }
}
