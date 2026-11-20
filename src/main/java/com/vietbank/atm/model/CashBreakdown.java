package com.vietbank.atm.model;

public class CashBreakdown {
    public int to500k;
    public int to200k;
    public int to100k;
    public int to50k;

    public CashBreakdown() {}

    public CashBreakdown(int to500k, int to200k, int to100k, int to50k) {
        this.to500k = to500k;
        this.to200k = to200k;
        this.to100k = to100k;
        this.to50k = to50k;
    }

    public int tongTo() {
        return to500k + to200k + to100k + to50k;
    }

    public long tongTien() {
        return to500k * 500_000L + to200k * 200_000L + to100k * 100_000L + to50k * 50_000L;
    }
}
