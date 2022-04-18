package com.gigaspaces.datavalidator.utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import org.apache.commons.codec.binary.Base64;

public class EncryptionDecryptionUtils {
    private static final String UNICODE_FORMAT = "UTF8";
    public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    private static KeySpec ks;
    private static SecretKeyFactory skf;
    private static Cipher cipher;
    static byte[] arrayBytes;
    private static String myEncryptionKey;
    private static String myEncryptionScheme;
    static SecretKey key;

    static {
        myEncryptionKey = "ThisIsSpartaThisIsSparta";//""rotAdIlAvAtAd";
        myEncryptionScheme = DESEDE_ENCRYPTION_SCHEME;
        try {
            arrayBytes = myEncryptionKey.getBytes(UNICODE_FORMAT);
            System.out.println("myEncryptionKey: "+myEncryptionKey );
            System.out.println("arrayBytes: "+arrayBytes);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            ks = new DESedeKeySpec(arrayBytes);
            System.out.println("ks:  "+ks);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        try {
            skf = SecretKeyFactory.getInstance(myEncryptionScheme);
            System.out.println("skf: "+skf);
            cipher = Cipher.getInstance(myEncryptionScheme);
            System.out.println("cipher: "+cipher);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        try {
            key = skf.generateSecret(ks);
            System.out.println("key: "+key);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }


    public static String encrypt(String unencryptedString) {
        String encryptedString = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] plainText = unencryptedString.getBytes(UNICODE_FORMAT);
            byte[] encryptedText = cipher.doFinal(plainText);
            encryptedString = new String(org.apache.commons.codec.binary.Base64.encodeBase64(encryptedText));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedString;
    }


    public static String decrypt(String encryptedString) {
        String decryptedText=null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] encryptedText = Base64.decodeBase64(encryptedString);
            byte[] plainText = cipher.doFinal(encryptedText);
            decryptedText= new String(plainText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedText;
    }
}