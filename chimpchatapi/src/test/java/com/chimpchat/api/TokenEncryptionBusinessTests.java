package com.chimpchat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.chimpchat.api.channel.business.TokenEncryptionBusiness;

class TokenEncryptionBusinessTests {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsAndDecryptsCredentialsWithoutStoringPlaintext() {
        TokenEncryptionBusiness encryption = new TokenEncryptionBusiness(KEY);
        String plaintext = "meta-access-token-value";

        String ciphertext = encryption.encrypt(plaintext);

        assertFalse(ciphertext.contains(plaintext));
        assertEquals(plaintext, encryption.decrypt(ciphertext));
    }

    @Test
    void rejectsTamperedCiphertext() {
        TokenEncryptionBusiness encryption = new TokenEncryptionBusiness(KEY);
        String ciphertext = encryption.encrypt("meta-access-token-value");
        char replacement = ciphertext.charAt(ciphertext.length() - 1) == 'A' ? 'B' : 'A';

        String tampered = ciphertext.substring(0, ciphertext.length() - 1) + replacement;

        assertThrows(TokenEncryptionBusiness.ChannelException.class, () -> encryption.decrypt(tampered));
    }
}
