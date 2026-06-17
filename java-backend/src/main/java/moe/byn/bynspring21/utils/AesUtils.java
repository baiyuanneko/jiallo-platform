package moe.byn.bynspring21.utils;

import cn.hutool.crypto.symmetric.SymmetricCrypto;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import moe.byn.bynspring21.exception.BynBaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class AesUtils {

    private final SymmetricCrypto aes;

    public AesUtils(@Value("${myapp.aes.key}") String aesKey) {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new BynBaseException("AES密钥长度必须为16、24或32字节");
        }
        this.aes = new SymmetricCrypto(SymmetricAlgorithm.AES, keyBytes);
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        return aes.encryptBase64(plainText, StandardCharsets.UTF_8);
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        return aes.decryptStr(cipherText, StandardCharsets.UTF_8);
    }
}
