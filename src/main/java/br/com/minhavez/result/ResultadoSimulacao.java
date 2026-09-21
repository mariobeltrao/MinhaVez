package br.com.minhavez.result;

import java.util.*;

public final class ResultadoSimulacao {
    private final boolean rodizioAtivo;
    private final List<ResultadoDia> resultadosDias = new ArrayList<>();

    public ResultadoSimulacao(boolean rodizioAtivo) { this.rodizioAtivo = rodizioAtivo; }
    public void adicionarResultadoDia(ResultadoDia resultado) {
        Objects.requireNonNull(resultado);
        if (resultado.getDia() != resultadosDias.size()) throw new IllegalArgumentException("Dias devem ser adicionados em ordem, a partir de zero");
        if (rodizioAtivo != (resultado.getGrupoRestrito() != null)) throw new IllegalArgumentException("Rodízio incompatível");
        resultadosDias.add(resultado);
    }
    public List<ResultadoDia> getResultadosDias() { return List.copyOf(resultadosDias); }
    public boolean isRodizioAtivo() { return rodizioAtivo; }
    private double dividir(double soma, double peso) { return peso == 0 ? 0 : soma / peso; }
    public double calcularTempoMedioGeral() {
        return dividir(resultadosDias.stream().mapToDouble(ResultadoDia::getTempoTotalViagensMinutos).sum(),
                resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeViagensConcluidas).sum());
    }
    public double calcularTempoMedioIda() {
        return dividir(resultadosDias.stream().mapToDouble(ResultadoDia::getTempoTotalIdasMinutos).sum(),
                resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeIdasConcluidas).sum());
    }
    public double calcularTempoMedioVolta() {
        return dividir(resultadosDias.stream().mapToDouble(ResultadoDia::getTempoTotalVoltasMinutos).sum(),
                resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeVoltasConcluidas).sum());
    }
    public double calcularVelocidadeMediaGeral() {
        return dividir(resultadosDias.stream().mapToDouble(ResultadoDia::getDistanciaTotalPercorridaKm).sum(),
                resultadosDias.stream().mapToDouble(ResultadoDia::getTempoTotalMovimentoHoras).sum());
    }
    public double calcularCongestionamentoMedioGeral() {
        return dividir(resultadosDias.stream().mapToDouble(d -> d.getCongestionamentoMedioPercentual()
                        * d.getQuantidadeAmostrasCongestionamento()).sum(),
                resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeAmostrasCongestionamento).sum());
    }
    public double getMaiorCongestionamentoPico() {
        return resultadosDias.stream().mapToDouble(ResultadoDia::getCongestionamentoPicoPercentual).max().orElse(0);
    }
    /** Soma de veículos-dia restritos, e não contagem de placas distintas no período. */
    public int getTotalVeiculosRestritos() { return resultadosDias.stream().mapToInt(ResultadoDia::getVeiculosRestritos).sum(); }
    public double calcularPercentualAmostrasComViaAcimaCapacidade() {
        return percentualAmostras(ResultadoDia::getAmostrasComViaAcimaCapacidade);
    }
    public double calcularPercentualAmostrasComViaSevera() {
        return percentualAmostras(ResultadoDia::getAmostrasComViaSevera);
    }
    public double calcularPercentualAmostrasComViaEmColapso() {
        return percentualAmostras(ResultadoDia::getAmostrasComViaEmColapso);
    }
    public double calcularMediaViasAcimaCapacidadeNoPico() {
        return dividir(resultadosDias.stream().mapToLong(ResultadoDia::getSomaViasAcimaCapacidadeNoPico).sum(),
                resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeAmostrasNoPico).sum());
    }
    public double calcularOcupacaoMediaViasAtivas() {
        return dividir(resultadosDias.stream().mapToDouble(ResultadoDia::getSomaOcupacaoViasAtivas).sum(),
                resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeObservacoesViasAtivas).sum());
    }
    public double calcularNumeroMedioViasAtivas() {
        return dividir(resultadosDias.stream().mapToLong(ResultadoDia::getSomaViasAtivas).sum(),
                resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeAmostrasCongestionamento).sum());
    }
    public double calcularPercentualViasAtivasCongestionadas() {
        return dividir(resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeObservacoesViasAcimaCapacidade).sum() * 100.0,
                resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeObservacoesViasAtivas).sum());
    }
    public double calcularTempoMedioAcimaCapacidadePorViaMinutos() {
        int quantidadeVias = resultadosDias.stream().mapToInt(ResultadoDia::getQuantidadeVias).max().orElse(0);
        long observacoes = resultadosDias.stream()
                .mapToLong(ResultadoDia::getQuantidadeObservacoesViasAcimaCapacidade).sum();
        return dividir(observacoes * 0.5, quantidadeVias);
    }
    public List<DiagnosticoVia> getTopViasMaisUtilizadas(int limite) {
        if (limite < 0) throw new IllegalArgumentException("Limite inválido");
        Map<String, DiagnosticoVia> agregados = new HashMap<>();
        for (ResultadoDia dia : resultadosDias) {
            for (DiagnosticoVia atual : dia.getDiagnosticosVias().values()) {
                agregados.merge(atual.viaId(), atual, (a, b) -> new DiagnosticoVia(a.viaId(),
                        a.somaOcupacaoPercentual() + b.somaOcupacaoPercentual(),
                        Math.max(a.ocupacaoMaximaPercentual(), b.ocupacaoMaximaPercentual()),
                        a.totalEntradas() + b.totalEntradas(),
                        a.amostrasAcimaCapacidade() + b.amostrasAcimaCapacidade(),
                        a.quantidadeAmostras() + b.quantidadeAmostras()));
            }
        }
        return agregados.values().stream()
                .sorted(Comparator.comparingLong(DiagnosticoVia::totalEntradas).reversed()
                        .thenComparing(Comparator.comparingDouble(DiagnosticoVia::ocupacaoMediaPercentual).reversed())
                        .thenComparing(DiagnosticoVia::viaId))
                .limit(limite).toList();
    }
    private double percentualAmostras(java.util.function.ToLongFunction<ResultadoDia> medida) {
        return dividir(resultadosDias.stream().mapToLong(medida).sum() * 100.0,
                resultadosDias.stream().mapToLong(ResultadoDia::getQuantidadeAmostrasCongestionamento).sum());
    }
}
