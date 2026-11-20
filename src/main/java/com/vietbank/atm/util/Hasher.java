package com.vietbank.atm.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Set;

public class Hasher {
    private static final int ITERATIONS = 65536;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();
    private static final Set<String> WEAK_PINS = Set.of("123123", "112233", "121212", "000111", "111000");

    public static String hashPinPBKDF2(String pin) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + HEX.formatHex(salt) + "$" + HEX.formatHex(pbkdf2(pin == null ? "" : pin.trim(), salt, ITERATIONS, 256));
    }

    public static boolean verifyPin(String cardNo, String rawPin, String storedHash) {
        if (rawPin == null || storedHash == null) return false;
        if (storedHash.startsWith("pbkdf2$")) {
            String[] p = storedHash.split("\\$");
            if (p.length != 4) return false;
            byte[] expected = HEX.parseHex(p[3]);
            return MessageDigest.isEqual(expected, pbkdf2(rawPin.trim(), HEX.parseHex(p[2]), Integer.parseInt(p[1]), expected.length * 8));
        }
        return MessageDigest.isEqual(hashPin(cardNo, rawPin).getBytes(StandardCharsets.UTF_8), storedHash.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    public static String hashPin(String cardNo, String pin) {
        String raw = (cardNo == null ? "" : cardNo.replaceAll("\\s+", "")) + ":" + (pin == null ? "" : pin.trim());
        try {
            return HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] pbkdf2(String pin, byte[] salt, int iterations, int keyLenBits) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(new PBEKeySpec(pin.toCharArray(), salt, iterations, keyLenBits)).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isValidPinFormat(String pin) {
        return pin != null && pin.matches("\\d{6}");
    }

    public static boolean isWeakPin(String pin) {
        return !isValidPinFormat(pin) || WEAK_PINS.contains(pin) || pin.chars().distinct().count() == 1
            || "0123456789".contains(pin) || "9876543210".contains(pin);
    }
}
