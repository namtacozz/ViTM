package com.vietbank.atm.model;

public class BankInfo {
    public String code;
    public String name;
    public long fee;

    public BankInfo(String code, String name, long fee) {
        this.code = code;
        this.name = name;
        this.fee = fee;
    }

    @Override
    public String toString() {
        return code + " - " + name;
    }
}
