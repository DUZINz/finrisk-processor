package com.empresa.riscos.strategy;

import com.empresa.riscos.model.FinancialData;

/**
 * Policy para clientes de baixo risco.
 */
public class LowRiskStrategy implements RiskStrategy {
    @Override
    public void evaluate(FinancialData data) {
        System.out.println("Cliente classificado como BAIXO risco.");
    }
}
