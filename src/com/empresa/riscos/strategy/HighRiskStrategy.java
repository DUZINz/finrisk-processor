package com.empresa.riscos.strategy;

import com.empresa.riscos.model.FinancialData;

/**
 * Policy para clientes de alto risco.
 */
public class HighRiskStrategy implements RiskStrategy {
    @Override
    public void evaluate(FinancialData data) {
        System.out.println("Cliente classificado como ALTO risco.");
    }
}
