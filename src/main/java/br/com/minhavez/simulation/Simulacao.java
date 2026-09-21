package br.com.minhavez.simulation;

import br.com.minhavez.enums.EstadoVeiculo;
import br.com.minhavez.model.*;
import br.com.minhavez.result.*;
import br.com.minhavez.service.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Simulação discreta em fases. Todas as rotas de uma partida simultânea são escolhidas
 * antes das entradas. Após as entradas, as velocidades ficam congeladas durante o tick.
 * Cada veículo usa seu tempo restante; a ocupação final é consolidada em conjunto.
 */
public final class Simulacao {
    public static final long INICIO_DIA_SEGUNDOS = 5 * 3600;
    public static final long FIM_NOVOS_EVENTOS_SEGUNDOS = 23 * 3600;
    public static final long PASSO_SIMULACAO_SEGUNDOS = 30;
    public static final long INTERVALO_EXIBICAO_SEGUNDOS = 120;
    private static final double TOLERANCIA_SEGUNDOS = 1e-9;

    private final Cenario cenario;
    private final boolean rodizioAtivo;
    private final SistemaRodizio sistemaRodizio = new SistemaRodizio();
    private final CalculadorRotas calculadorRotas;
    private ColetorMetricas coletorMetricas;
    private long tempoAtualSegundos;

    private record Partida(Veiculo veiculo, Rota rota, boolean ida) { }
    private record Movimento(double distanciaKm, double segundos) { }

    public Simulacao(Cenario cenario, boolean rodizioAtivo) {
        this(cenario, rodizioAtivo, new CalculadorRotas());
    }
    public Simulacao(Cenario cenario, boolean rodizioAtivo, CalculadorRotas calculadorRotas) {
        this.cenario = Objects.requireNonNull(cenario).copiarParaExecucao();
        this.rodizioAtivo = rodizioAtivo;
        this.calculadorRotas = Objects.requireNonNull(calculadorRotas);
    }
    public ResultadoSimulacao executar() { return executar((tempo, circulando) -> { }, dia -> { }); }

    /** Observadores opcionais permitem ao terminal exibir progresso sem controlar a física. */
    public ResultadoSimulacao executar(BiConsumer<Long, Integer> aCadaExibicao, Consumer<ResultadoDia> aoFinalDoDia) {
        Objects.requireNonNull(aCadaExibicao);
        Objects.requireNonNull(aoFinalDoDia);
        ResultadoSimulacao resultado = new ResultadoSimulacao(rodizioAtivo);
        for (int dia = 0; dia < cenario.getQuantidadeDias(); dia++) {
            prepararDia(dia);
            while (true) {
                if (tempoAtualSegundos == FIM_NOVOS_EVENTOS_SEGUNDOS) {
                    coletorMetricas.registrarVeiculosEmCirculacaoAs23h(contarVeiculosEmMovimento());
                }
                if (tempoAtualSegundos >= FIM_NOVOS_EVENTOS_SEGUNDOS && contarVeiculosEmMovimento() == 0) break;
                processarTick();
                if ((tempoAtualSegundos - INICIO_DIA_SEGUNDOS) % INTERVALO_EXIBICAO_SEGUNDOS == 0) {
                    aCadaExibicao.accept(tempoAtualSegundos, contarVeiculosEmMovimento());
                }
            }
            ResultadoDia resultadoDia = coletorMetricas.gerarResultadoDia(dia,
                    rodizioAtivo ? sistemaRodizio.getGrupoRestrito(dia) : null);
            resultado.adicionarResultadoDia(resultadoDia);
            aoFinalDoDia.accept(resultadoDia);
        }
        return resultado;
    }
    private void prepararDia(int dia) {
        cenario.getCidade().resetarOcupacaoDasVias();
        coletorMetricas = new ColetorMetricas();
        tempoAtualSegundos = INICIO_DIA_SEGUNDOS;
        for (Veiculo veiculo : cenario.getVeiculos()) {
            veiculo.resetarParaNovoDia();
            if (rodizioAtivo && !sistemaRodizio.podeCircular(veiculo, dia)) {
                veiculo.marcarRestrito();
                coletorMetricas.registrarVeiculoRestrito(veiculo);
            }
        }
    }
    private void processarTick() {
        // Fases de decisão: nenhuma ocupação muda enquanto as rotas são calculadas.
        if (tempoAtualSegundos < FIM_NOVOS_EVENTOS_SEGUNDOS) iniciarViagensProgramadas();
        Map<Via, Double> velocidades = new HashMap<>();
        for (Via via : cenario.getCidade().getVias()) velocidades.put(via, via.calcularVelocidadeAtualKmh());

        // Fase de movimento: mudanças em um veículo não influenciam a velocidade dos demais.
        double distanciaTick = 0;
        double segundosTick = 0;
        for (Veiculo veiculo : cenario.getVeiculos()) {
            if (veiculo.estaEmMovimento()) {
                Movimento movimento = movimentarVeiculo(veiculo, velocidades);
                distanciaTick += movimento.distanciaKm();
                segundosTick += movimento.segundos();
            }
        }
        consolidarOcupacoes();
        coletorMetricas.registrarMovimento(distanciaTick, segundosTick);
        tempoAtualSegundos += PASSO_SIMULACAO_SEGUNDOS;
        coletorMetricas.registrarAmostra(tempoAtualSegundos, cenario.getCidade(), cenario.getVeiculos(), distanciaTick, segundosTick);
    }
    private void iniciarViagensProgramadas() {
        List<Partida> partidas = new ArrayList<>();
        for (Veiculo veiculo : cenario.getVeiculos()) {
            boolean ida = veiculo.getEstado() == EstadoVeiculo.EM_CASA
                    && tempoAtualSegundos >= veiculo.getHorarioSaida().toSecondOfDay();
            boolean volta = veiculo.getEstado() == EstadoVeiculo.NO_DESTINO
                    && tempoAtualSegundos >= veiculo.getHorarioRetorno().toSecondOfDay();
            if (ida || volta) {
                Ponto destino = ida ? veiculo.getDestino() : veiculo.getResidencia();
                Rota rota = calculadorRotas.calcularMelhorRota(cenario.getCidade(), veiculo.getPontoAtual(), destino);
                partidas.add(new Partida(veiculo, rota, ida));
            }
        }
        for (Partida partida : partidas) {
            if (partida.ida()) partida.veiculo().iniciarIda(partida.rota(), tempoAtualSegundos);
            else partida.veiculo().iniciarVolta(partida.rota(), tempoAtualSegundos);
            partida.veiculo().getViaAtual().entrarVeiculo();
            coletorMetricas.registrarEntradaVia(partida.veiculo().getViaAtual());
            coletorMetricas.registrarInicioViagem(partida.veiculo());
        }
    }
    private Movimento movimentarVeiculo(Veiculo veiculo, Map<Via, Double> velocidades) {
        double restanteSegundos = PASSO_SIMULACAO_SEGUNDOS;
        double distancia = 0;
        while (restanteSegundos > TOLERANCIA_SEGUNDOS && veiculo.estaEmMovimento()) {
            double velocidade = velocidades.get(veiculo.getViaAtual());
            double segundosAteFim = veiculo.getDistanciaRestanteNaViaKm() / velocidade * 3600;
            double usados = Math.min(restanteSegundos, segundosAteFim);
            double deslocamento = Math.min(veiculo.getDistanciaRestanteNaViaKm(), velocidade * usados / 3600);
            veiculo.avancar(deslocamento);
            distancia += deslocamento;
            restanteSegundos -= usados;
            if (veiculo.atingiuFimDaVia()) {
                veiculo.concluirViaAtual();
                if (veiculo.possuiProximaVia()) {
                    veiculo.definirViaAtual();
                    coletorMetricas.registrarEntradaVia(veiculo.getViaAtual());
                }
                else concluirViagem(veiculo, tempoAtualSegundos + PASSO_SIMULACAO_SEGUNDOS - restanteSegundos);
            }
        }
        return new Movimento(distancia, PASSO_SIMULACAO_SEGUNDOS - restanteSegundos);
    }
    private void concluirViagem(Veiculo veiculo, double instanteConclusao) {
        double minutos = (instanteConclusao - veiculo.getInstanteInicioViagemSegundos()) / 60;
        if (veiculo.getEstado() == EstadoVeiculo.EM_IDA) {
            veiculo.concluirIda();
            coletorMetricas.registrarConclusaoIda(minutos);
        } else {
            veiculo.concluirVolta();
            coletorMetricas.registrarConclusaoVolta(minutos);
        }
    }
    private void consolidarOcupacoes() {
        // A V1 só possui a população residente: a ocupação é a contagem dos veículos nas vias.
        cenario.getCidade().resetarOcupacaoDasVias();
        for (Veiculo veiculo : cenario.getVeiculos()) {
            if (veiculo.estaEmMovimento()) veiculo.getViaAtual().entrarVeiculo();
        }
    }
    private int contarVeiculosEmMovimento() {
        return (int) cenario.getVeiculos().stream().filter(Veiculo::estaEmMovimento).count();
    }
}
