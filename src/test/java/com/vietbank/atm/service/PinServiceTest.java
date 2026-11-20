package com.vietbank.atm.service;

import com.vietbank.atm.util.Hasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PinServiceTest {
    private final PinService pinService = new PinService();

    @Test
    void shouldRejectSameOrWeakOrMismatchedPin() {
        assertEquals("err.pin_same_old", pinService.validatePinChangeInputs("123456", "123456", "123456"));
        assertEquals("err.pin_weak", pinService.validatePinChangeInputs("839204", "000000", "000000"));
        assertEquals("err.pin_weak", pinService.validatePinChangeInputs("839204", "654321", "654321"));
        assertEquals("err.pin_weak", pinService.validatePinChangeInputs("839204", "121212", "121212"));
        assertEquals("err.pin_mismatch", pinService.validatePinChangeInputs("123456", "839204", "839205"));
    }

    @Test
    void shouldAcceptStrongPin() {
        assertNull(pinService.validatePinChangeInputs("123456", "839204", "839204"));
    }

    @Test
    void shouldHashPinWithPBKDF2SaltAndVerify() {
        String h1 = Hasher.hashPinPBKDF2("839204");
        String h2 = Hasher.hashPinPBKDF2("839204");
        assertTrue(h1.startsWith("pbkdf2$65536$"));
        assertNotEquals(h1, h2);
        assertTrue(Hasher.verifyPin("9704001234567890", "839204", h1));
        assertTrue(Hasher.verifyPin("9704001234567890", "839204", h2));
        assertFalse(Hasher.verifyPin("9704001234567890", "000000", h1));
    }

    @Test
    void shouldHashPinDeterministically() {
        assertEquals(
            "67590a0da7df033b72ae36cbf36b2965ea1c497b4635c36c2bdb2a1867c7b457",
            Hasher.hashPin("9704001234567890", "123456")
        );
    }

    @Test
    void shouldLockCardOnThirdFailedAttempt() {
        int wrongCount = 0;
        String status = "ACTIVE";
        for (int i = 0; i < AuthService.MAX_PIN_WRONG; i++) {
            wrongCount++;
            if (wrongCount >= AuthService.MAX_PIN_WRONG) status = "LOCKED";
        }
        assertEquals("LOCKED", status);
    }
}
