package com.vietbank.atm.model;

public class LoginResult {
    public boolean success;
    public boolean locked;
    public String errorCode;
    public int remainingAttempts;
    public int accId;
    public String tenChuTk;
    public String soTaiKhoan;
    public long soDu;
}
