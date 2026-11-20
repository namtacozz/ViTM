package com.vietbank.atm.model;

public class Card {
    public String soThe;
    public String pinHash;
    public int accountId;
    public int soLanSai;
    public String trangThai;

    public Card() {}

    public Card(String soThe, String pinHash, int accountId, int soLanSai, String trangThai) {
        this.soThe = soThe;
        this.pinHash = pinHash;
        this.accountId = accountId;
        this.soLanSai = soLanSai;
        this.trangThai = trangThai;
    }
}
