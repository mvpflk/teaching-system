package com.school.teaching.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import com.school.teaching.exception.BusinessException;

public class EncryptionUtil {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec aesKey;

    public EncryptionUtil(SecretKeySpec aesKey) {
        this.aesKey = aesKey;
    }

    public EncryptionUtil(String secret) {
        byte[] keyBytes = new byte[16];
        byte[] src = secret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 16));
        this.aesKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) return "";
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BusinessException(500, "AES/GCM 加密失败");
        }
    }

    public String decrypt(String b64) {
        if (b64 == null || b64.isEmpty()) return "";
        try {
            byte[] data = Base64.getDecoder().decode(b64);
            if (data.length > GCM_IV_LENGTH) {
                try {
                    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                    GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, data, 0, GCM_IV_LENGTH);
                    c.init(Cipher.DECRYPT_MODE, aesKey, spec);
                    return new String(c.doFinal(data, GCM_IV_LENGTH, data.length - GCM_IV_LENGTH), StandardCharsets.UTF_8);
                } catch (Exception ignored) { }
            }
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, aesKey);
            return new String(c.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(500, "AES 解密失败");
        }
    }
}