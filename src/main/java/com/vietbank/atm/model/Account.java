package com.vietbank.atm.model;

public class Account {
    public int id;
    public String soTaiKhoan;
    public String tenChuTk;
    public long soDu;

    public Account() {}

    public Account(int id, String soTaiKhoan, String tenChuTk, long soDu) {
        this.id = id;
        this.soTaiKhoan = soTaiKhoan;
        this.tenChuTk = tenChuTk;
        this.soDu = soDu;
    }
}
