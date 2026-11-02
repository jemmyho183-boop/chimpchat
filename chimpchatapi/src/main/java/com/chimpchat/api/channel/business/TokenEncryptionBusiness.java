package com.chimpchat.api.channel.business;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TokenEncryptionBusiness {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String encodedKey;

    public TokenEncryptionBusiness(@Value("${app.security.token-encryption-key:}") String encodedKey) {
        this.encodedKey = encodedKey;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new ChannelException(HttpStatus.SERVICE_UNAVAILABLE, "token_encryption_unavailable",
                    "Channel credential encryption is not configured.");
        }
    }

    public boolean isConfigured() {
        try {
            return key() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] encrypted = Base64.getDecoder().decode(ciphertext);
            if (encrypted.length <= NONCE_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted token.");
            }
            ByteBuffer input = ByteBuffer.wrap(encrypted);
            byte[] nonce = new byte[NONCE_LENGTH];
            input.get(nonce);
            byte[] content = new byte[input.remaining()];
            input.get(content);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(content), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new ChannelException(HttpStatus.INTERNAL_SERVER_ERROR, "channel_credentials_unavailable",
                    "Stored channel credentials could not be decrypted.");
        }
    }

    private SecretKeySpec key() {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalArgumentException("Token encryption key is not configured.");
        }
        byte[] key = Base64.getDecoder().decode(encodedKey);
        if (key.length != 32) {
            throw new IllegalArgumentException("Token encryption key must decode to 32 bytes.");
        }
        return new SecretKeySpec(key, "AES");
    }

    public static class ChannelException extends RuntimeException {
        private final HttpStatus status;
        private final String code;

        public ChannelException(HttpStatus status, String code, String message) {
            super(message);
            this.status = status;
            this.code = code;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getCode() {
            return code;
        }
    }
}
