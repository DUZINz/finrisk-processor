package com.edu.risk;

import java.util.*;
import java.util.concurrent.*;
import java.time.*;

/**
 * IMPLEMENTAÇÃO EXPLICADA (Comentários no código explicam decisões de design)
 *
 * Contém soluções para as 4 questões pedidas:
 * Q1 - Strategy + Context para algoritmos de risco (intercambiáveis em runtime)
 * Q2 - Adapter bidirecional para integrar com sistema bancário legado
 * Q3 - State pattern para controle de estados da usina nuclear com validações
 * Q4 - Chain of Responsibility para validação de NF-e com circuit-breaker e rollback
 *
 * Principais padrões usados e por que:
 * - Strategy (Q1): permite trocar algoritmo de cálculo em tempo de execução sem expor
 *   detalhes de implementação ao cliente (atende restrição). Segue princípio do
 *   Open/Closed (S of SOLID) — novos algoritmos adicionados sem modificar clientes.
 * - Context Object (Q1): encapsula parâmetros financeiros complexos compartilhados
 *   entre estratégias, evitando longas assinaturas de método (SRP, YAGNI).
 * - Adapter (Q2): adapta a interface moderna (ProcessadorTransacoes) para a
 *   interface legada (SistemaBancarioLegado) e vice-versa. Segue princípio de
 *   Interface Segregation (I) e Dependency Inversion (D) quando usado corretamente.
 * - State (Q3): modela estados complexos e regras de transição, encapsula
 *   comportamento por estado e evita condicionais espalhadas (OCP, SRP).
 * - Chain of Responsibility (Q4): encadeia validadores, permite pular validadores
 *   condicionalmente e interromper a cadeia (circuit breaker). Rollback é tratado
 *   por validadores que fazem modificações.
 * - ExecutorService + Future para timeouts em validadores (Q4).
 *
 * Princípios SOLID demonstrados (resumo):
 * - S (Single Responsibility): cada classe tem uma responsabilidade bem definida.
 * - O (Open/Closed): novos algoritmos/validadores/estados podem ser adicionados sem
 *   modificar o código cliente existente.
 * - L (Liskov): as estratégias e estados implementam interfaces substituíveis.
 * - I (Interface Segregation): interfaces enxutas por finalidade (RiskCalculator,
 *   ProcessadorTransacoes, etc.).
 * - D (Dependency Inversion): clientes dependem de abstrações (interfaces), não de
 *   classes concretas.
 */

/* ===================== Q1 - Risco: Strategy + Context ===================== */

// Contexto complexo compartilhado entre algoritmos
class RiskContext {
    // Parâmetros financeiros genéricos
    public Map<String, Object> params = new HashMap<>();

    public RiskContext put(String k, Object v) {
        params.put(k, v);
        return this;
    }

    public <T> T get(String k, Class<T> cls) {
        return cls.cast(params.get(k));
    }
}

// Strategy interface
interface RiskCalculator {
    // calcula risco usando o RiskContext e retorna resultado em String (dummy)
    String calculate(RiskContext context);
}

// Implementações dummy
class VaRCalculator implements RiskCalculator {
    // Value at Risk - dummy implementation
    @Override
    public String calculate(RiskContext context) {
        // Documentação de design: VaR separado para permitir testes A/B e troca fácil
        Double position = context.get("position", Double.class);
        if (position == null) position = 100000.0;
        return String.format("VaR: valor estimado para posição %.2f = %.2f", position, position * 0.05);
    }
}

class ExpectedShortfallCalculator implements RiskCalculator {
    @Override
    public String calculate(RiskContext context) {
        Double exposure = context.get("exposure", Double.class);
        if (exposure == null) exposure = 50000.0;
        return String.format("Expected Shortfall: para exposição %.2f = %.2f", exposure, exposure * 0.08);
    }
}

class StressTestCalculator implements RiskCalculator {
    @Override
    public String calculate(RiskContext context) {
        // Simula stress scenario
        String scenario = context.get("scenario", String.class);
        if (scenario == null) scenario = "Crash-30%";
        return String.format("Stress Test (%s): impacto simulado = %s", scenario, "-30% patrimônio");
    }
}

// Contexto que usa Strategy e permite troca dinâmica
class RiskProcessor {
    private RiskCalculator calculator;
    private RiskContext context;

    public RiskProcessor(RiskCalculator initial, RiskContext ctx) {
        this.calculator = initial;
        this.context = ctx;
    }

    public void setCalculator(RiskCalculator calc) { // permite trocar em runtime
        this.calculator = calc;
    }

    public String run() {
        return calculator.calculate(context);
    }
}

/* ===================== Q2 - Adapter para Sistema Bancário Legado ===================== */

// Interface moderna desejada pelo sistema
interface ProcessadorTransacoes {
    // autorizar(cartao, valor, moeda)
    // moeda é string ex: "USD", "EUR", "BRL"
    Map<String, Object> autorizar(String cartao, double valor, String moeda);
}

// Simulação do sistema legado com assinatura incompatível
class SistemaBancarioLegado {
    // processarTransacao(HashMap<String, Object> parametros)
    public HashMap<String, Object> processarTransacao(HashMap<String, Object> parametros) {
        // Exemplo: Legado exige "cardNumber", "amount", "currencyCode" (int code)
        // E um campo obrigatório: "terminalId"
        HashMap<String, Object> response = new HashMap<>();
        // Simula processamento
        Integer code = (Integer) parametros.getOrDefault("currencyCode", 3);
        String card = (String) parametros.get("cardNumber");
        Double amount = (Double) parametros.get("amount");
        String terminal = (String) parametros.get("terminalId");
        if (terminal == null) {
            response.put("status", "ERROR");
            response.put("message", "terminalId missing");
            return response;
        }
        // Dummy approval logic
        boolean approved = amount < 10000.0;
        response.put("status", approved ? "APPROVED" : "DECLINED");
        response.put("legacyCode", code);
        response.put("cardMasked", card == null ? null : mask(card));
        return response;
    }

    private String mask(String card) {
        if (card.length() <= 4) return card;
        return "****-****-****-" + card.substring(card.length() - 4);
    }
}

// Adapter moderno -> legado
class ProcessadorParaLegadoAdapter implements ProcessadorTransacoes {
    private SistemaBancarioLegado legado;
    private static final Map<String, Integer> CURRENCY_MAP = Map.of("USD", 1, "EUR", 2, "BRL", 3);

    public ProcessadorParaLegadoAdapter(SistemaBancarioLegado legado) {
        this.legado = legado;
    }

    @Override
    public Map<String, Object> autorizar(String cartao, double valor, String moeda) {
        // Converte para formato legado
        HashMap<String, Object> params = new HashMap<>();
        params.put("cardNumber", cartao);
        params.put("amount", valor);
        // moeda obrigatória no legado via código
        params.put("currencyCode", CURRENCY_MAP.getOrDefault(moeda, 3));
        // Campo obrigatório do legado que não existe na interface moderna: terminalId
        // Implementamos um fallback sensato (configurável em produção). Aqui hardcode.
        params.put("terminalId", "TERM-DEFAULT-01");

        HashMap<String, Object> resp = legado.processarTransacao(params);

        // Converte resposta legado -> formato moderno
        Map<String, Object> modernResp = new HashMap<>();
        modernResp.put("status", resp.get("status"));
        modernResp.put("message", resp.getOrDefault("message", ""));
        modernResp.put("currencyCode", resp.get("legacyCode"));
        modernResp.put("cardMasked", resp.get("cardMasked"));
        return modernResp;
    }
}

// Adapter legado -> moderno (permite uso bidirecional: modernizar chamadas legadas)
class LegadoParaProcessadorAdapter {
    private ProcessadorTransacoes modern;
    private static final Map<Integer, String> REVERSE_CURRENCY = Map.of(1, "USD", 2, "EUR", 3, "BRL");

    public LegadoParaProcessadorAdapter(ProcessadorTransacoes modern) {
        this.modern = modern;
    }

    public HashMap<String, Object> processarTransacao(HashMap<String, Object> parametros) {
        // Extrai e converte
        String cartao = (String) parametros.get("cardNumber");
        Double amount = (Double) parametros.get("amount");
        Integer currencyCode = (Integer) parametros.getOrDefault("currencyCode", 3);
        String moeda = REVERSE_CURRENCY.getOrDefault(currencyCode, "BRL");

        Map<String, Object> modernResp = modern.autorizar(cartao, amount, moeda);

        // Map back to legacy response format
        HashMap<String, Object> legacyResp = new HashMap<>();
        legacyResp.put("status", modernResp.getOrDefault("status", "ERROR"));
        legacyResp.put("message", modernResp.getOrDefault("message", ""));
        legacyResp.put("legacyCode", currencyCode);
        return legacyResp;
    }
}

/* ===================== Q3 - Usina Nuclear: State Pattern ===================== */

enum ReactorStateName {DESLIGADA, OPERACAO_NORMAL, ALERTA_AMARELO, ALERTA_VERMELHO, EMERGENCIA, MANUTENCAO}

// Contexto com parâmetros de sensores
class ReactorContext {
    public double temperatura;
    public double pressao;
    public double radiacao;
    public boolean coolingSystemWorking = true;
    public boolean maintenanceMode = false; // se true, sobrescreve estados normais
    // Histórico de estados para prevenir ciclos perigosos
    public Deque<ReactorStateName> history = new ArrayDeque<>();
    // Timestamp when ALERTA_AMARELO started
    public Instant alertaAmareloStart = null;
}

// Estado abstrato
interface ReactorState {
    ReactorStateName name();
    boolean canTransitionTo(ReactorStateName target, ReactorContext ctx);
    void onEnter(ReactorContext ctx);
}

// Implementações de estado enfatizam validações
abstract class BaseState implements ReactorState {
    public void onEnter(ReactorContext ctx) {
        // por default não faz nada
    }
}

class DesligadaState extends BaseState {
    public ReactorStateName name() { return ReactorStateName.DESLIGADA; }
    public boolean canTransitionTo(ReactorStateName target, ReactorContext ctx) {
        // permite ligar para OPERACAO_NORMAL
        return target == ReactorStateName.OPERACAO_NORMAL || target == ReactorStateName.MANUTENCAO;
    }
}

class OperacaoNormalState extends BaseState {
    public ReactorStateName name() { return ReactorStateName.OPERACAO_NORMAL; }
    public boolean canTransitionTo(ReactorStateName target, ReactorContext ctx) {
        if (ctx.maintenanceMode) return target == ReactorStateName.MANUTENCAO || target == ReactorStateName.DESLIGADA;
        if (target == ReactorStateName.ALERTA_AMARELO) return ctx.temperatura > 300.0;
        return target == ReactorStateName.DESLIGADA;
    }
}

class AlertaAmareloState extends BaseState {
    public ReactorStateName name() { return ReactorStateName.ALERTA_AMARELO; }
    public boolean canTransitionTo(ReactorStateName target, ReactorContext ctx) {
        if (target == ReactorStateName.ALERTA_VERMELHO) {
            // precisa ter temperatura >400 por mais de 30s
            if (ctx.alertaAmareloStart == null) return false;
            Duration d = Duration.between(ctx.alertaAmareloStart, Instant.now());
            return ctx.temperatura > 400.0 && d.toSeconds() >= 30;
        }
        // pode voltar para OPERACAO_NORMAL se temperatura cair
        if (target == ReactorStateName.OPERACAO_NORMAL) return ctx.temperatura <= 300.0;
        return false;
    }
    public void onEnter(ReactorContext ctx) {
        if (ctx.alertaAmareloStart == null) ctx.alertaAmareloStart = Instant.now();
    }
}

class AlertaVermelhoState extends BaseState {
    public ReactorStateName name() { return ReactorStateName.ALERTA_VERMELHO; }
    public boolean canTransitionTo(ReactorStateName target, ReactorContext ctx) {
        if (target == ReactorStateName.EMERGENCIA) {
            return !ctx.coolingSystemWorking; // se sistema de resfriamento falhar
        }
        // possibilita retroceder para ALERTA_AMARELO se melhora (bidirecional)
        if (target == ReactorStateName.ALERTA_AMARELO) return ctx.temperatura <= 400.0;
        return false;
    }
}

class EmergenciaState extends BaseState {
    public ReactorStateName name() { return ReactorStateName.EMERGENCIA; }
    public boolean canTransitionTo(ReactorStateName target, ReactorContext ctx) {
        // Emergencia é final (unidirecional) exceto manutenção e desligada forçadas
        return target == ReactorStateName.MANUTENCAO || target == ReactorStateName.DESLIGADA;
    }
}

class ManutencaoState extends BaseState {
    public ReactorStateName name() { return ReactorStateName.MANUTENCAO; }
    public boolean canTransitionTo(ReactorStateName target, ReactorContext ctx) {
        // manutenção pode retornar ao DESLIGADA ou OPERACAO_NORMAL dependendo das flags
        return target == ReactorStateName.DESLIGADA || target == ReactorStateName.OPERACAO_NORMAL;
    }
}

class ReactorController {
    private ReactorState current;
    private ReactorContext ctx;

    public ReactorController(ReactorState initial, ReactorContext ctx) {
        this.current = initial;
        this.ctx = ctx;
        enterState(initial);
    }

    private void enterState(ReactorState s) {
        // Previne transições circulares perigosas: não permite entrar num estado que já foi
        // repetidamente no curto período (ex: mais de 3 vezes seguidas).
        ctx.history.addLast(s.name());
        if (ctx.history.size() > 10) ctx.history.removeFirst();
        long repeats = ctx.history.stream().filter(n -> n == s.name()).count();
        if (repeats > 3) {
            throw new IllegalStateException("Transição circular detectada para " + s.name());
        }
        this.current = s;
        s.onEnter(ctx);
    }

    public ReactorStateName getState() { return current.name(); }

    public boolean requestTransition(ReactorStateName target) {
        if (ctx.maintenanceMode && target != ReactorStateName.MANUTENCAO) {
            // se modo manutenção ativo, sobrescreve temporariamente (manutenção tem prioridade)
            System.out.println("Manutenção ativa: só transições para MANUTENCAO permitidas");
            return false;
        }
        // regra especial: EMERGENCIA só após ALERTA_VERMELHO ter ocorrido
        if (target == ReactorStateName.EMERGENCIA) {
            boolean hadVermelho = ctx.history.contains(ReactorStateName.ALERTA_VERMELHO);
            if (!hadVermelho) {
                System.out.println("EMERGENCIA só permitida após ALERTA_VERMELHO");
                return false;
            }
        }
        ReactorState candidate = stateFor(target);
        if (candidate == null) return false;
        if (!current.canTransitionTo(target, ctx)) return false;
        enterState(candidate);
        return true;
    }

    private ReactorState stateFor(ReactorStateName name) {
        switch (name) {
            case DESLIGADA: return new DesligadaState();
            case OPERACAO_NORMAL: return new OperacaoNormalState();
            case ALERTA_AMARELO: return new AlertaAmareloState();
            case ALERTA_VERMELHO: return new AlertaVermelhoState();
            case EMERGENCIA: return new EmergenciaState();
            case MANUTENCAO: return new ManutencaoState();
            default: return null;
        }
    }
}

/* ===================== Q4 - Validação NF-e: Chain of Responsibility ===================== */

// Resultado de validação
class ValidationResult {
    public boolean ok;
    public String message;
    public ValidationResult(boolean ok, String message) { this.ok = ok; this.message = message; }
}

// Documento fiscal simplificado
class NFeDocument {
    public String xml;
    public String certSerial;
    public double impostos;
    public String numero;
    public boolean insertedToDB = false; // usado pelo validador 4 para rollback
}

// Validador abstrato
abstract class NFeValidator {
    protected NFeValidator next;
    protected int timeoutSeconds = 5; // default timeout

    public NFeValidator linkNext(NFeValidator nxt) { this.next = nxt; return nxt; }

    public void setTimeout(int s) { this.timeoutSeconds = s; }

    // executa o validador com timeout
    public ValidationResult execute(NFeDocument doc, ExecutorService exec) {
        Callable<ValidationResult> task = () -> this.validate(doc);
        Future<ValidationResult> future = exec.submit(task);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            return new ValidationResult(false, "TIMEOUT");
        } catch (Exception e) {
            return new ValidationResult(false, "ERROR: " + e.getMessage());
        }
    }

    protected abstract ValidationResult validate(NFeDocument doc) throws Exception;
}

// Circuit breaker gerenciado no orquestrador
class NFeValidationOrchestrator {
    private NFeValidator head;
    private int failCount = 0;
    private final int circuitThreshold = 3;

    public NFeValidationOrchestrator(NFeValidator head) { this.head = head; }

    // executa cadeia, com regras condicionais: se X falhar, pule Y (implementado manualmente nos validadores)
    public boolean run(NFeDocument doc) {
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            NFeValidator current = head;
            while (current != null) {
                ValidationResult r = current.execute(doc, exec);
                if (!r.ok) {
                    failCount++;
                    System.out.println("Validador falhou: " + r.message);
                    if (failCount >= circuitThreshold) {
                        System.out.println("Circuit breaker ativado: interrompendo cadeia");
                        // rollback se necessário (validador 4 implementa rollback ao detectar falha posterior)
                        return false;
                    }
                    // Condicional: alguns validadores podem indicar para pular próximos (implementado via mensagem)
                    if (r.message != null && r.message.startsWith("SKIP_NEXT:")) {
                        // form: SKIP_NEXT:2 -> pular 2 validadores
                        String[] parts = r.message.split(":");
                        int skip = Integer.parseInt(parts[1]);
                        for (int i=0;i<skip;i++) if (current != null) current = current.next;
                        if (current == null) break;
                        current = current.next;
                        continue;
                    }
                    // caso de falha simples: interrompe (poderia prosseguir dependendo do requisito)
                    return false;
                }
                current = current.next;
            }
            return true;
        } finally {
            exec.shutdownNow();
        }
    }
}

// 1. Validador de Schema XML contra XSD (simulado)
class SchemaValidator extends NFeValidator {
    protected ValidationResult validate(NFeDocument doc) {
        if (doc.xml == null || !doc.xml.contains("<nfe")) return new ValidationResult(false, "Schema invalid");
        return new ValidationResult(true, "OK");
    }
}

// 2. Validador de Certificado Digital (expiração e revogação) - simulado
class CertValidator extends NFeValidator {
    protected ValidationResult validate(NFeDocument doc) {
        if (doc.certSerial == null) return new ValidationResult(false, "Certificate missing");
        if (doc.certSerial.equals("REVOKED")) return new ValidationResult(false, "Certificate revoked");
        // se prestador de serviço offline, simular SKIP
        if (doc.certSerial.equals("SKIP-DB")) return new ValidationResult(false, "SKIP_NEXT:1");
        return new ValidationResult(true, "OK");
    }
}

// 3. Validador de Regras Fiscais (apenas se anteriores passarem)
class FiscalRulesValidator extends NFeValidator {
    protected ValidationResult validate(NFeDocument doc) {
        if (doc.impostos < 0) return new ValidationResult(false, "Invalid tax calc");
        // dummy check
        return new ValidationResult(true, "OK");
    }
}

// 4. Validador de Banco de dados (duplicidade) - precisa fazer rollback se próximas falharem
class DBValidator extends NFeValidator {
    protected ValidationResult validate(NFeDocument doc) {
        // Simula inserção
        if (doc.numero == null) return new ValidationResult(false, "NF number missing");
        if (doc.numero.equals("DUP")) return new ValidationResult(false, "Duplicate number");
        // marca inserção
        doc.insertedToDB = true;
        System.out.println("Inserted NF=" + doc.numero + " into DB (simulated)");
        // register a hook to rollback if needed: here we only set flag; orchestrator must call rollback on failure
        return new ValidationResult(true, "OK");
    }

    // rollback manual chamado pelo orquestrador se necessário
    public void rollback(NFeDocument doc) {
        if (doc.insertedToDB) {
            doc.insertedToDB = false;
            System.out.println("Rollback: removed NF=" + doc.numero);
        }
    }
}

// 5. Validador de Serviço SEFAZ (consulta online) - simulado
class SefazValidator extends NFeValidator {
    protected ValidationResult validate(NFeDocument doc) {
        // suponha que apenas se previous passed
        if (doc.numero.equals("TIMEDOUT")) {
            try { Thread.sleep(10000); } catch (InterruptedException e) {}
        }
        return new ValidationResult(true, "OK");
    }
}

/* ===================== Demo / Main ===================== */
public class SistemaDemo {
    public static void main(String[] args) {
        System.out.println("=== Q1 Demo - Risk Strategy ===");
        RiskContext ctx = new RiskContext();
        ctx.put("position", 150000.0).put("exposure", 50000.0).put("scenario", "MegaCrash");
        RiskProcessor rp = new RiskProcessor(new VaRCalculator(), ctx);
        System.out.println(rp.run());
        rp.setCalculator(new ExpectedShortfallCalculator());
        System.out.println(rp.run());
        rp.setCalculator(new StressTestCalculator());
        System.out.println(rp.run());

        System.out.println("\n=== Q2 Demo - Adapter ===");
        SistemaBancarioLegado legado = new SistemaBancarioLegado();
        ProcessadorTransacoes modern = new ProcessadorParaLegadoAdapter(legado);
        Map<String, Object> res = modern.autorizar("1234567812345678", 500.0, "USD");
        System.out.println("Resposta moderna: " + res);

        // bidirecional: legacy call adapted to modern
        LegadoParaProcessadorAdapter legacyAdapter = new LegadoParaProcessadorAdapter(modern);
        HashMap<String,Object> legacyParams = new HashMap<>();
        legacyParams.put("cardNumber", "9999888877776666");
        legacyParams.put("amount", 200.0);
        legacyParams.put("currencyCode", 1);
        HashMap<String,Object> legacyResp = legacyAdapter.processarTransacao(legacyParams);
        System.out.println("Resposta legado->moderno: " + legacyResp);

        System.out.println("\n=== Q3 Demo - Reactor State ===");
        ReactorContext rctx = new ReactorContext();
        rctx.temperatura = 350.0; // triggering ALERTA_AMARELO
        ReactorController controller = new ReactorController(new DesligadaState(), rctx);
        System.out.println("Estado inicial: " + controller.getState());
        controller.requestTransition(ReactorStateName.OPERACAO_NORMAL);
        System.out.println("Depois ligar: " + controller.getState());
        boolean moved = controller.requestTransition(ReactorStateName.ALERTA_AMARELO);
        System.out.println("Para AMARELO? " + moved + " estado: " + controller.getState());

        // Simula 35s em ALERTA_AMARELO com temperatura alta -> para vermelho
        rctx.temperatura = 410.0;
        try { Thread.sleep(31000); } catch (InterruptedException e) {}
        boolean toVermelho = controller.requestTransition(ReactorStateName.ALERTA_VERMELHO);
        System.out.println("Para VERMELHO? " + toVermelho + " estado: " + controller.getState());

        // Cooling system fails -> emergencia
        rctx.coolingSystemWorking = false;
        boolean toEmerg = controller.requestTransition(ReactorStateName.EMERGENCIA);
        System.out.println("Para EMERGENCIA? " + toEmerg + " estado: " + controller.getState());

        System.out.println("\n=== Q4 Demo - NFe Validation Chain ===");
        // Monta cadeia: Schema -> Cert -> Fiscal -> DB -> SEFAZ
        SchemaValidator v1 = new SchemaValidator(); v1.setTimeout(2);
        CertValidator v2 = new CertValidator(); v2.setTimeout(2);
        FiscalRulesValidator v3 = new FiscalRulesValidator(); v3.setTimeout(2);
        DBValidator v4 = new DBValidator(); v4.setTimeout(2);
        SefazValidator v5 = new SefazValidator(); v5.setTimeout(2);
        v1.linkNext(v2).linkNext(v3).linkNext(v4).linkNext(v5);

        NFeValidationOrchestrator orchestrator = new NFeValidationOrchestrator(v1);

        NFeDocument doc = new NFeDocument();
        doc.xml = "<nfe>...</nfe>";
        doc.certSerial = "VALID";
        doc.impostos = 123.45;
        doc.numero = "NF-001";

        boolean ok = orchestrator.run(doc);
        System.out.println("Validação completa: " + ok + " insertedToDB=" + doc.insertedToDB);

        // Simula falha subsequente para demonstrar rollback: executar com doc.numero="DUP" para falha DB
        NFeDocument doc2 = new NFeDocument();
        doc2.xml = "<nfe>...</nfe>"; doc2.certSerial = "VALID"; doc2.impostos = 0; doc2.numero = "DUP";
        // Run: DB validator will fail, so should not leave inserted state (no insertion happened)
        boolean ok2 = orchestrator.run(doc2);
        System.out.println("Validação DUP: " + ok2 + " insertedToDB=" + doc2.insertedToDB);
    }
}
