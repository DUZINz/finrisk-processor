package com.empresa.riscos;

import com.empresa.riscos.model.FinancialData;
import com.empresa.riscos.pipeline.*;
import com.empresa.riscos.strategy.*;
import com.empresa.riscos.service.RiskProcessor;

public class Main {
    public static void main(String[] args) {
        FinancialData data = new FinancialData(750, 50000, false);

        RiskHandler pipeline = new BasicRiskValidator()
                .setNext(new CreditRiskValidator()
                        .setNext(new FraudRiskValidator()));

        RiskStrategy strategy = data.getScore() < 600
                ? new HighRiskStrategy()
                : new LowRiskStrategy();

        RiskProcessor processor = new RiskProcessor(pipeline, strategy);
        processor.process(data);
    }
}
