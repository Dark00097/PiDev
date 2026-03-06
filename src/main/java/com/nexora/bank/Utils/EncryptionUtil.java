package com.nexora.bank.Utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitaire de chiffrement AES-256-CBC pour les données sensibles (ex: montants).
 *
 * ⚠️  IMPORTANT : La clé SECRET_KEY doit être stockée dans une variable
 *     d'environnement ou un fichier de config sécurisé (jamais en dur en prod !).
 */
public class EncryptionUtil {

    // 🔑 Clé AES-256 : exactement 32 caractères
    // ⚠️ En production, charge-la depuis une variable d'environnement :
    //    System.getenv("NEXORA_AES_KEY")
    private static final String SECRET_KEY = "NexoraBank@SecureKey#2025!!12345"; // 32 chars

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    /**
     * Chiffre un montant (Double) → String base64 stockable en BDD.
     * Format retourné : "IV_base64:CipherText_base64"
     */
    public static String encrypt(Double montant) throws Exception {
        if (montant == null) return null;

        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        // Génération d'un IV aléatoire (16 bytes) pour chaque chiffrement
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] encrypted = cipher.doFinal(
            String.valueOf(montant).getBytes(StandardCharsets.UTF_8)
        );

        String ivBase64 = Base64.getEncoder().encodeToString(iv);
        String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);

        return ivBase64 + ":" + encryptedBase64;
    }

    /**
     * Déchiffre une chaîne base64 (depuis BDD) → Double montant original.
     */
    public static Double decrypt(String encryptedData) throws Exception {
        if (encryptedData == null || !encryptedData.contains(":")) return null;

        String[] parts = encryptedData.split(":", 2);
        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] encryptedBytes = Base64.getDecoder().decode(parts[1]);

        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

        byte[] decrypted = cipher.doFinal(encryptedBytes);
        return Double.parseDouble(new String(decrypted, StandardCharsets.UTF_8));
    }
}