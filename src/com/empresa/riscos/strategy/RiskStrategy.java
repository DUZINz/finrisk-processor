package com.empresa.riscos.strategy;

import com.empresa.riscos.model.FinancialData;

/**
 * Strategy: define comportamento de avaliação de risco (intercambiável em runtime).
 * Justificativa: Strategy permite trocar políticas sem alterar o processamento.
 */
public interface RiskStrategy {
    void evaluate(FinancialData data);
}
