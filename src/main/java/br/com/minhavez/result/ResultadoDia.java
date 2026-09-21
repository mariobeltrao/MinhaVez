package br.com.minhavez.result;

import br.com.minhavez.enums.*;
import java.util.*;

/** Resultado imutável, incluindo cópias das séries temporais em ordem cronológica. */
public final class ResultadoDia {
    private final int dia;
    private final GrupoRodizio grupoRestrito;
    private final int veiculosQueCircularam;
    private final int veiculosRestritos;
    private final int veiculosEmCirculacaoAs23h;
    private final int quantidadeIdasConcluidas;
    private final int quantidadeVoltasConcluidas;
    private final double tempoTotalIdasMinutos;
    private final double tempoTotalVoltasMinutos;
    private final double distanciaTotalPercorridaKm;
    private final double tempoTotalMovimentoHoras;
    private final double congestionamentoMedioPercentual;
    private final double congestionamentoPicoPercentual;
    private final int maximoViasCongestionadas;
    private final long amostrasComViaAcimaCapacidade;
    private final long amostrasComViaSevera;
    private final long amostrasComViaEmColapso;
    private final long somaViasAcimaCapacidadeNoPico;
    private final long quantidadeAmostrasNoPico;
    private final double somaOcupacaoViasAtivas;
    private final long quantidadeObservacoesViasAtivas;
    private final long somaViasAtivas;
    private final long quantidadeObservacoesViasAcimaCapacidade;
    private final int quantidadeVias;
    private final Map<String, DiagnosticoVia> diagnosticosVias;
    private final Map<Regiao, Double> congestionamentoMedioPorRegiao;
    private final double congestionamentoMedioInterregional;
    private final Map<Long, Integer> serieVeiculosCirculando;
    private final Map<Long, Double> serieCongestionamentoCidade;
    private final Map<Long, Double> serieVelocidadeMedia;

    public ResultadoDia(int dia, GrupoRodizio grupoRestrito, int veiculosQueCircularam,
                        int veiculosRestritos, int veiculosEmCirculacaoAs23h,
                        List<Double> temposIda, List<Double> temposVolta,
                        double distanciaTotalPercorridaKm, double tempoTotalMovimentoHoras,
                        double congestionamentoPicoPercentual, int maximoViasCongestionadas,
                        long amostrasComViaAcimaCapacidade, long amostrasComViaSevera,
                        long amostrasComViaEmColapso, long somaViasAcimaCapacidadeNoPico,
                        long quantidadeAmostrasNoPico,
                        double somaOcupacaoViasAtivas, long quantidadeObservacoesViasAtivas,
                        long somaViasAtivas, long quantidadeObservacoesViasAcimaCapacidade,
                        int quantidadeVias, Map<String, DiagnosticoVia> diagnosticosVias,
                        Map<Regiao, Double> congestionamentoMedioPorRegiao,
                        double congestionamentoMedioInterregional,
                        Map<Long, Integer> serieVeiculosCirculando,
                        Map<Long, Double> serieCongestionamentoCidade,
                        Map<Long, Double> serieVelocidadeMedia) {
        if (dia < 0 || veiculosQueCircularam < 0 || veiculosRestritos < 0 || veiculosEmCirculacaoAs23h < 0
                || maximoViasCongestionadas < 0 || amostrasComViaAcimaCapacidade < 0
                || amostrasComViaSevera < 0 || amostrasComViaEmColapso < 0
                || somaViasAcimaCapacidadeNoPico < 0 || quantidadeAmostrasNoPico < 0
                || quantidadeObservacoesViasAtivas < 0 || somaViasAtivas < 0
                || quantidadeObservacoesViasAcimaCapacidade < 0 || quantidadeVias < 0) {
            throw new IllegalArgumentException("Contagem inválida");
        }
        validarMedida(distanciaTotalPercorridaKm);
        validarMedida(tempoTotalMovimentoHoras);
        validarMedida(congestionamentoPicoPercentual);
        validarMedida(congestionamentoMedioInterregional);
        validarMedida(somaOcupacaoViasAtivas);
        temposIda.forEach(ResultadoDia::validarMedida);
        temposVolta.forEach(ResultadoDia::validarMedida);
        this.dia = dia;
        this.grupoRestrito = grupoRestrito;
        this.veiculosQueCircularam = veiculosQueCircularam;
        this.veiculosRestritos = veiculosRestritos;
        this.veiculosEmCirculacaoAs23h = veiculosEmCirculacaoAs23h;
        quantidadeIdasConcluidas = temposIda.size();
        quantidadeVoltasConcluidas = temposVolta.size();
        tempoTotalIdasMinutos = temposIda.stream().mapToDouble(Double::doubleValue).sum();
        tempoTotalVoltasMinutos = temposVolta.stream().mapToDouble(Double::doubleValue).sum();
        this.distanciaTotalPercorridaKm = distanciaTotalPercorridaKm;
        this.tempoTotalMovimentoHoras = tempoTotalMovimentoHoras;
        this.congestionamentoPicoPercentual = congestionamentoPicoPercentual;
        this.maximoViasCongestionadas = maximoViasCongestionadas;
        this.amostrasComViaAcimaCapacidade = amostrasComViaAcimaCapacidade;
        this.amostrasComViaSevera = amostrasComViaSevera;
        this.amostrasComViaEmColapso = amostrasComViaEmColapso;
        this.somaViasAcimaCapacidadeNoPico = somaViasAcimaCapacidadeNoPico;
        this.quantidadeAmostrasNoPico = quantidadeAmostrasNoPico;
        this.somaOcupacaoViasAtivas = somaOcupacaoViasAtivas;
        this.quantidadeObservacoesViasAtivas = quantidadeObservacoesViasAtivas;
        this.somaViasAtivas = somaViasAtivas;
        this.quantidadeObservacoesViasAcimaCapacidade = quantidadeObservacoesViasAcimaCapacidade;
        this.quantidadeVias = quantidadeVias;
        this.diagnosticosVias = Map.copyOf(diagnosticosVias);
        this.congestionamentoMedioPorRegiao = Map.copyOf(congestionamentoMedioPorRegiao);
        this.congestionamentoMedioInterregional = congestionamentoMedioInterregional;
        this.serieVeiculosCirculando = Collections.unmodifiableMap(new TreeMap<>(serieVeiculosCirculando));
        this.serieCongestionamentoCidade = Collections.unmodifiableMap(new TreeMap<>(serieCongestionamentoCidade));
        this.serieVelocidadeMedia = Collections.unmodifiableMap(new TreeMap<>(serieVelocidadeMedia));
        congestionamentoMedioPercentual = this.serieCongestionamentoCidade.values().stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
    }
    private static void validarMedida(double valor) {
        if (!Double.isFinite(valor) || valor < 0) throw new IllegalArgumentException("Medida inválida");
    }
    private double media(double soma, int quantidade) { return quantidade == 0 ? 0 : soma / quantidade; }
    public int getQuantidadeViagensConcluidas() { return quantidadeIdasConcluidas + quantidadeVoltasConcluidas; }
    public double getTempoTotalViagensMinutos() { return tempoTotalIdasMinutos + tempoTotalVoltasMinutos; }
    public double getTempoMedioViagemMinutos() { return media(getTempoTotalViagensMinutos(), getQuantidadeViagensConcluidas()); }
    public double getTempoMedioIdaMinutos() { return media(tempoTotalIdasMinutos, quantidadeIdasConcluidas); }
    public double getTempoMedioVoltaMinutos() { return media(tempoTotalVoltasMinutos, quantidadeVoltasConcluidas); }
    public double getVelocidadeMediaKmh() { return tempoTotalMovimentoHoras == 0 ? 0 : distanciaTotalPercorridaKm / tempoTotalMovimentoHoras; }
    public long getQuantidadeAmostrasCongestionamento() { return serieCongestionamentoCidade.size(); }
    public int getDia() { return dia; }
    public GrupoRodizio getGrupoRestrito() { return grupoRestrito; }
    public int getVeiculosQueCircularam() { return veiculosQueCircularam; }
    public int getVeiculosRestritos() { return veiculosRestritos; }
    public int getVeiculosEmCirculacaoAs23h() { return veiculosEmCirculacaoAs23h; }
    public int getQuantidadeIdasConcluidas() { return quantidadeIdasConcluidas; }
    public int getQuantidadeVoltasConcluidas() { return quantidadeVoltasConcluidas; }
    public double getTempoTotalIdasMinutos() { return tempoTotalIdasMinutos; }
    public double getTempoTotalVoltasMinutos() { return tempoTotalVoltasMinutos; }
    public double getDistanciaTotalPercorridaKm() { return distanciaTotalPercorridaKm; }
    public double getTempoTotalMovimentoHoras() { return tempoTotalMovimentoHoras; }
    public double getCongestionamentoMedioPercentual() { return congestionamentoMedioPercentual; }
    public double getCongestionamentoPicoPercentual() { return congestionamentoPicoPercentual; }
    public int getMaximoViasCongestionadas() { return maximoViasCongestionadas; }
    public long getAmostrasComViaAcimaCapacidade() { return amostrasComViaAcimaCapacidade; }
    public long getAmostrasComViaSevera() { return amostrasComViaSevera; }
    public long getAmostrasComViaEmColapso() { return amostrasComViaEmColapso; }
    public long getSomaViasAcimaCapacidadeNoPico() { return somaViasAcimaCapacidadeNoPico; }
    public long getQuantidadeAmostrasNoPico() { return quantidadeAmostrasNoPico; }
    public double getSomaOcupacaoViasAtivas() { return somaOcupacaoViasAtivas; }
    public long getQuantidadeObservacoesViasAtivas() { return quantidadeObservacoesViasAtivas; }
    public long getSomaViasAtivas() { return somaViasAtivas; }
    public long getQuantidadeObservacoesViasAcimaCapacidade() { return quantidadeObservacoesViasAcimaCapacidade; }
    public int getQuantidadeVias() { return quantidadeVias; }
    public Map<String, DiagnosticoVia> getDiagnosticosVias() { return diagnosticosVias; }
    public Map<Regiao, Double> getCongestionamentoMedioPorRegiao() { return congestionamentoMedioPorRegiao; }
    public double getCongestionamentoMedioInterregional() { return congestionamentoMedioInterregional; }
    public Map<Long, Integer> getSerieVeiculosCirculando() { return serieVeiculosCirculando; }
    public Map<Long, Double> getSerieCongestionamentoCidade() { return serieCongestionamentoCidade; }
    public Map<Long, Double> getSerieVelocidadeMedia() { return serieVelocidadeMedia; }
}
