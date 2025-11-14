package com.empresa.riscos.pipeline;

import com.empresa.riscos.model.FinancialData;

/**
 * Chain of Responsibility base: cada handler processa e decide se passa adiante.
 * Justificativa: facilita composição/condicionais sem if/else centralizados.
 * Segue SRP e OCP (podemos adicionar novos handlers sem modificar existentes).
 */
public abstract class RiskHandler {
    protected RiskHandler next;

    public RiskHandler setNext(RiskHandler next) {
        this.next = next;
        return next;
    }

    public void handle(FinancialData data) {
        if (process(data) && next != null) {
            next.handle(data);
        }
    }

    protected abstract boolean process(FinancialData data);
}
