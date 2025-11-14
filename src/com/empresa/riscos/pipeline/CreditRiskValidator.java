package com.empresa.riscos.pipeline;

import com.empresa.riscos.model.FinancialData;

/**
 * Valida risco de crédito (ex.: renda mínima).
 */
public class CreditRiskValidator extends RiskHandler {
    @Override
    protected boolean process(FinancialData data) {
        System.out.println("Validando risco de crédito...");
        return data.getIncome() > 10000;
    }
}
