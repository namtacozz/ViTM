package com.vietbank.atm.state;

import com.vietbank.atm.config.AppConfig;

public class SessionManager {
    private String cardNo = "", holderName = "", accountNo = "";
    private int accId = 0, secondsRemaining = AppConfig.getInt("session.timeout.seconds", 30);
    private boolean active = false;

    public void startSession(String cardNo, int accId, String holderName, String accountNo) {
        this.cardNo = cardNo;
        this.accId = accId;
        this.holderName = holderName;
        this.accountNo = accountNo;
        this.active = true;
        resetTimer();
    }

    public void clearSession() {
        startSession("", 0, "", "");
        active = false;
    }

    public void resetTimer() { secondsRemaining = AppConfig.getInt("session.timeout.seconds", 30); }
    public int tick() { return secondsRemaining > 0 ? --secondsRemaining : 0; }
    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }
    public int getAccId() { return accId; }
    public String getHolderName() { return holderName; }
    public String getAccountNo() { return accountNo; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getSecondsRemaining() { return secondsRemaining; }
}
