package com.empresa.riscos.model;

/**
 * Modelo simples que representa dados financeiros compartilhados.
 * Pode ser expandido para incluir mais parâmetros (context object).
 */
public class FinancialData {
    private int score;
    private double income;
    private boolean fraudFlag;

    public FinancialData(int score, double income, boolean fraudFlag) {
        this.score = score;
        this.income = income;
        this.fraudFlag = fraudFlag;
    }

    public int getScore() { return score; }
    public double getIncome() { return income; }
    public boolean isFraudFlag() { return fraudFlag; }
}
