package com.empresa.riscos.service;

import com.empresa.riscos.model.FinancialData;
import com.empresa.riscos.pipeline.RiskHandler;
import com.empresa.riscos.strategy.RiskStrategy;

/**
 * Componente que orquestra pipeline (Chain of Responsibility) e política (Strategy).
 * Demonstra injeção por construtor e separação de responsabilidades (SRP, D of SOLID).
 */
public class RiskProcessor {
    private final RiskHandler handler;
    private final RiskStrategy strategy;

    public RiskProcessor(RiskHandler handler, RiskStrategy strategy) {
        this.handler = handler;
        this.strategy = strategy;
    }

    public void process(FinancialData data) {
        handler.handle(data);      // validações / pipeline
        strategy.evaluate(data);   // decisão de risco baseada na estratégia atual
    }
}
