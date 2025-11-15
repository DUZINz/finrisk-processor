# Risk Processing System

Este projeto demonstra o uso de **Design Patterns** e **princípios SOLID** em um sistema modular de avaliação de risco financeiro. A aplicação segue uma arquitetura limpa, separada em pacotes, com código extensível e fácil de manter.

## 📌 Objetivo

Criar um sistema de processamento de risco para empresas financeiras usando:

* **Chain of Responsibility** para validações encadeadas.
* **Strategy** para permitir troca dinâmica de algoritmos de avaliação de risco.
* **Modelos imutáveis/claros** para dados financeiros.
* **Orquestração limpa** via serviço dedicado.

A estrutura do código segue exatamente:

```
src/
 └── com/empresa/riscos/
       ├── Main.java
       ├── pipeline/
       │      ├── RiskHandler.java
       │      ├── BasicRiskValidator.java
       │      ├── CreditRiskValidator.java
       │      └── FraudRiskValidator.java
       │
       ├── strategy/
       │      ├── RiskStrategy.java
       │      ├── HighRiskStrategy.java
       │      └── LowRiskStrategy.java
       │
       ├── model/
       │      └── FinancialData.java
       │
       └── service/
              └── RiskProcessor.java
```

---

# 🧩 Design Patterns Utilizados

## 1. **Chain of Responsibility** — (pacote `pipeline/`)

Usado para criar uma cadeia de validadores de risco. Cada validador decide:

* se aprova e passa adiante
* ou se bloqueia a cadeia

### ✔ Por que usar?

* Evita centenas de if/else.
* Permite adicionar novos validadores sem alterar código existente (**OCP**).
* Facilita ativar/desativar validadores conforme regras do negócio.

### ✔ Onde é aplicado?

Classe base `RiskHandler` e validadores:

* `BasicRiskValidator`
* `CreditRiskValidator`
* `FraudRiskValidator`

---

## 2. **Strategy** — (pacote `strategy/`)

Permite trocar dinamicamente o algoritmo de classificação de risco.

### ✔ Por que usar?

* Evita lógica fixa ou muitos switches.
* Permite mudar política de risco em tempo real.
* Segue **SRP** e **OCP**.

### ✔ Exemplos implementados

* `HighRiskStrategy`
* `LowRiskStrategy`

A escolha é mostrada em `Main.java`.

---

## 3. **Context Object** — (`FinancialData`)

Agrupa dados necessários para todos os cálculos e validações.

### ✔ Por que usar?

* Evita passar dezenas de parâmetros entre métodos.
* Padroniza o fluxo de dados.

---

## 4. **Service Layer** — (`RiskProcessor`)

Faz a orquestração entre *pipeline* e *strategy*.

### ✔ Por que usar?

* Separa responsabilidades.
* Evita lógica misturada no `Main`.
* Segue **D - Dependency Inversion (SOLID)**.

---

# ▶ Como executar

1. Estruture o projeto conforme o diretório indicado.
2. Compile:

```
javac src/com/empresa/riscos/**/*.java
```

3. Rode:

```
java com.empresa.riscos.Main
```

Saída esperada:

```
Validando requisitos básicos...
Validando risco de crédito...
Verificando risco de fraude...
Cliente classificado como BAIXO risco.
```

---

# 📚 Explicação SOLID

### ✔ **S – Single Responsibility Principle**

Cada classe possui apenas uma responsabilidade.

### ✔ **O – Open/Closed Principle**

Novos validadores ou estratégias podem ser adicionados sem alterar código existente.

### ✔ **L – Liskov Substitution Principle**

Todos os `RiskHandler` e `RiskStrategy` podem substituir suas superclasses/interfaces.

### ✔ **I – Interface Segregation Principle**

A interface `RiskStrategy` é pequena e específica.

### ✔ **D – Dependency Inversion Principle**

`RiskProcessor` não cria concretos — recebe via construtor.

---

# 📌 Possíveis Extensões

* Loggers específicos para cada validador.
* Estratégias avançadas de risco real.
* Versão com Spring Boot.
* Configuração dinâmica de pipeline via arquivo externo.

---

# ✔ Conclusão

Este projeto demonstra com clareza o uso de **Chain of Responsibility + Strategy**, estruturado para ser simples de entender, extensível e adequado para avaliação acadêmica ou profissional.

Se quiser, posso gerar também:
✅ UML completo do projeto
✅ Versão Maven/Gradle
✅ Testes JUnit
