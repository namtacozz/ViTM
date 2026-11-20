package com.vietbank.atm.model;

public class CashVault extends CashBreakdown {
    public CashVault() {}
    public CashVault(int to500k, int to200k, int to100k, int to50k) { super(to500k, to200k, to100k, to50k); }
}
