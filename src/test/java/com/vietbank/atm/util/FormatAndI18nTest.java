package com.vietbank.atm.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FormatAndI18nTest {
    @Test
    void shouldMaskCardAndAccountNumbers() {
        assertEquals("9704 •••• •••• 7890", MoneyFormat.maskCardNumber("9704001234567890"));
        assertEquals("••••••7890", MoneyFormat.maskAccountNumber("1234567890"));
        assertEquals("NGUYEN VAN AN", MoneyFormat.toAsciiUpperName("NGUYỄN VĂN AN"));
    }

    @Test
    void shouldSwitchLanguagesViaResourceBundle() {
        I18n.setEnglish(false);
        assertEquals("RÚT TIỀN", I18n.get("menu.withdraw"));
        I18n.setEnglish(true);
        assertEquals("CASH WITHDRAWAL", I18n.get("menu.withdraw"));
        I18n.setEnglish(false);
    }
}
