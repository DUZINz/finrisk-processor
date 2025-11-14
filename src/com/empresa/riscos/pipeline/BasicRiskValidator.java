package com.empresa.riscos.pipeline;

import com.empresa.riscos.model.FinancialData;

/**
 * Valida requisitos básicos (ex.: score mínimo).
 */
public class BasicRiskValidator extends RiskHandler {
    @Override
    protected boolean process(FinancialData data) {
        System.out.println("Validando requisitos básicos...");
        return data.getScore() > 300;
    }
}
