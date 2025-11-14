package com.empresa.riscos.pipeline;

import com.empresa.riscos.model.FinancialData;

/**
 * Verifica sinalizadores de fraude.
 */
public class FraudRiskValidator extends RiskHandler {
    @Override
    protected boolean process(FinancialData data) {
        System.out.println("Verificando risco de fraude...");
        return !data.isFraudFlag();
    }
}
