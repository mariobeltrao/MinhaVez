package br.com.minhavez.service;

import br.com.minhavez.enums.Regiao;
import br.com.minhavez.result.*;
import java.util.Locale;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

public final class ComparadorResultados {
    private static final Locale LOCAL = Locale.forLanguageTag("pt-BR");

    /** Base e novo iguais a zero: 0%; base zero e novo diferente: NaN (não definido). */
    public double calcularVariacaoPercentual(double valorBase, double valorNovo) {
        if (!Double.isFinite(valorBase) || !Double.isFinite(valorNovo)) throw new IllegalArgumentException("Valores devem ser finitos");
        if (valorBase == 0) return valorNovo == 0 ? 0 : Double.NaN;
        return ((valorNovo - valorBase) / valorBase) * 100;
    }
    public double calcularVariacaoTempoMedio(ResultadoSimulacao sem, ResultadoSimulacao com) {
        return calcularVariacaoPercentual(sem.calcularTempoMedioGeral(), com.calcularTempoMedioGeral());
    }
    public double calcularVariacaoVelocidadeMedia(ResultadoSimulacao sem, ResultadoSimulacao com) {
        return calcularVariacaoPercentual(sem.calcularVelocidadeMediaGeral(), com.calcularVelocidadeMediaGeral());
    }
    public double calcularVariacaoCongestionamento(ResultadoSimulacao sem, ResultadoSimulacao com) {
        return calcularVariacaoPercentual(sem.calcularCongestionamentoMedioGeral(), com.calcularCongestionamentoMedioGeral());
    }
    public String gerarRelatorioComparativo(ResultadoSimulacao sem, ResultadoSimulacao com) {
        Objects.requireNonNull(sem);
        Objects.requireNonNull(com);
        if (sem.isRodizioAtivo() || !com.isRodizioAtivo() || sem.getResultadosDias().isEmpty()
                || sem.getResultadosDias().size() != com.getResultadosDias().size()) {
            throw new IllegalArgumentException("Compare SEM e COM rodízio para o mesmo período não vazio");
        }
        StringBuilder texto = new StringBuilder();
        texto.append("\n===============================================================\n")
                .append("                  MINHA VEZ - RESULTADOS\n")
                .append("===============================================================\n")
                .append("Período: ").append(sem.getResultadosDias().size()).append(" dia(s)\n\n")
                .append(String.format("%-30s %15s %15s%n", "Métrica", "SEM RODÍZIO", "COM RODÍZIO"));
        linha(texto, "Tempo médio (min)", sem.calcularTempoMedioGeral(), com.calcularTempoMedioGeral());
        linha(texto, "Tempo de ida (min)", sem.calcularTempoMedioIda(), com.calcularTempoMedioIda());
        linha(texto, "Tempo de volta (min)", sem.calcularTempoMedioVolta(), com.calcularTempoMedioVolta());
        linha(texto, "Velocidade média (km/h)", sem.calcularVelocidadeMediaGeral(), com.calcularVelocidadeMediaGeral());
        linha(texto, "Ocupação média (%)", sem.calcularCongestionamentoMedioGeral(), com.calcularCongestionamentoMedioGeral());
        linha(texto, "Pico 17h–19h (%)", sem.getMaiorCongestionamentoPico(), com.getMaiorCongestionamentoPico());
        contagem(texto, "Máx. simultaneamente nas vias", maximoCirculando(sem), maximoCirculando(com));
        contagem(texto, "Veículos-dia que circularam", soma(sem, ResultadoDia::getVeiculosQueCircularam), soma(com, ResultadoDia::getVeiculosQueCircularam));
        contagem(texto, "Veículos-dia restritos", sem.getTotalVeiculosRestritos(), com.getTotalVeiculosRestritos());
        contagem(texto, "Viagens concluídas", soma(sem, ResultadoDia::getQuantidadeViagensConcluidas), soma(com, ResultadoDia::getQuantidadeViagensConcluidas));
        contagem(texto, "Veículos-dia nas vias às 23h", soma(sem, ResultadoDia::getVeiculosEmCirculacaoAs23h), soma(com, ResultadoDia::getVeiculosEmCirculacaoAs23h));
        contagem(texto, "Máx. vias acima de 100%", maximoVias(sem), maximoVias(com));
        texto.append("\nANÁLISE DE SATURAÇÃO\n");
        linha(texto, "Amostras com via >100% (%)", sem.calcularPercentualAmostrasComViaAcimaCapacidade(),
                com.calcularPercentualAmostrasComViaAcimaCapacidade());
        linha(texto, "Amostras com via SEVERA (%)", sem.calcularPercentualAmostrasComViaSevera(),
                com.calcularPercentualAmostrasComViaSevera());
        linha(texto, "Amostras com via COLAPSO (%)", sem.calcularPercentualAmostrasComViaEmColapso(),
                com.calcularPercentualAmostrasComViaEmColapso());
        linha(texto, "Média vias >100% no pico", sem.calcularMediaViasAcimaCapacidadeNoPico(),
                com.calcularMediaViasAcimaCapacidadeNoPico());
        texto.append("\nDIAGNÓSTICO DAS VIAS ATIVAS\n");
        linha(texto, "Ocupação vias ativas (%)", sem.calcularOcupacaoMediaViasAtivas(),
                com.calcularOcupacaoMediaViasAtivas());
        linha(texto, "Número médio de vias ativas", sem.calcularNumeroMedioViasAtivas(),
                com.calcularNumeroMedioViasAtivas());
        linha(texto, "Vias ativas >100% (%)", sem.calcularPercentualViasAtivasCongestionadas(),
                com.calcularPercentualViasAtivasCongestionadas());
        linha(texto, "Tempo médio >100%/via (min)", sem.calcularTempoMedioAcimaCapacidadePorViaMinutos(),
                com.calcularTempoMedioAcimaCapacidadePorViaMinutos());
        texto.append("\nOcupação média por categoria (%)\n");
        for (Regiao regiao : Regiao.values()) {
            linha(texto, regiao.name(), mediaAmostras(sem, d -> d.getCongestionamentoMedioPorRegiao().getOrDefault(regiao, 0.0)),
                    mediaAmostras(com, d -> d.getCongestionamentoMedioPorRegiao().getOrDefault(regiao, 0.0)));
        }
        linha(texto, "INTERREGIONAIS", mediaAmostras(sem, ResultadoDia::getCongestionamentoMedioInterregional),
                mediaAmostras(com, ResultadoDia::getCongestionamentoMedioInterregional));
        topVias(texto, "SEM RODÍZIO", sem);
        topVias(texto, "COM RODÍZIO", com);
        texto.append("\nVARIAÇÕES (COM em relação a SEM)\n");
        variacao(texto, "Tempo médio", "min", sem.calcularTempoMedioGeral(),
                com.calcularTempoMedioGeral());
        variacao(texto, "Tempo médio de ida", "min", sem.calcularTempoMedioIda(),
                com.calcularTempoMedioIda());
        variacao(texto, "Tempo médio de volta", "min", sem.calcularTempoMedioVolta(),
                com.calcularTempoMedioVolta());
        variacao(texto, "Velocidade média", "km/h", sem.calcularVelocidadeMediaGeral(),
                com.calcularVelocidadeMediaGeral());
        variacao(texto, "Congestionamento", "%", sem.calcularCongestionamentoMedioGeral(),
                com.calcularCongestionamentoMedioGeral());
        variacao(texto, "Pico de ocupação", "%", sem.getMaiorCongestionamentoPico(),
                com.getMaiorCongestionamentoPico());
        variacao(texto, "Máximo simultâneo nas vias", "veículos", maximoCirculando(sem), maximoCirculando(com));
        variacao(texto, "Máximo de vias acima de 100%", "vias", maximoVias(sem), maximoVias(com));
        variacao(texto, "Veículos-dia", "veículos-dia", soma(sem, ResultadoDia::getVeiculosQueCircularam),
                soma(com, ResultadoDia::getVeiculosQueCircularam));
        variacao(texto, "Viagens concluídas", "viagens", soma(sem, ResultadoDia::getQuantidadeViagensConcluidas),
                soma(com, ResultadoDia::getQuantidadeViagensConcluidas));
        variacao(texto, "Veículos restritos", "veículos-dia", sem.getTotalVeiculosRestritos(),
                com.getTotalVeiculosRestritos());
        variacao(texto, "Amostras com via acima de 100%", "%",
                sem.calcularPercentualAmostrasComViaAcimaCapacidade(),
                com.calcularPercentualAmostrasComViaAcimaCapacidade());
        variacao(texto, "Amostras com via severa", "%", sem.calcularPercentualAmostrasComViaSevera(),
                com.calcularPercentualAmostrasComViaSevera());
        variacao(texto, "Amostras com via em colapso", "%", sem.calcularPercentualAmostrasComViaEmColapso(),
                com.calcularPercentualAmostrasComViaEmColapso());
        variacao(texto, "Média de vias acima de 100% no pico", "vias",
                sem.calcularMediaViasAcimaCapacidadeNoPico(), com.calcularMediaViasAcimaCapacidadeNoPico());
        variacao(texto, "Ocupação média das vias ativas", "%", sem.calcularOcupacaoMediaViasAtivas(),
                com.calcularOcupacaoMediaViasAtivas());
        variacao(texto, "Número médio de vias ativas", "vias", sem.calcularNumeroMedioViasAtivas(),
                com.calcularNumeroMedioViasAtivas());
        variacao(texto, "Vias ativas congestionadas", "%", sem.calcularPercentualViasAtivasCongestionadas(),
                com.calcularPercentualViasAtivasCongestionadas());
        variacao(texto, "Tempo médio acima de 100% por via", "min",
                sem.calcularTempoMedioAcimaCapacidadePorViaMinutos(),
                com.calcularTempoMedioAcimaCapacidadePorViaMinutos());
        texto.append("\nOcupação = média das taxas das vias; pode ultrapassar 100%.\n")
                .append("Pico = maior ocupação média da cidade entre 17h e 19h.\n")
                .append("Saturação = percentual de amostras com ao menos uma via na condição indicada.\n")
                .append("Veículos-dia contam a participação de um veículo em cada dia.\n")
                .append("Parâmetros fictícios: estes resultados não descrevem trânsito real.\n");
        return texto.toString();
    }
    private void linha(StringBuilder texto, String nome, double a, double b) {
        texto.append(String.format(LOCAL, "%-30s %15.2f %15.2f%n", nome, a, b));
    }
    private void contagem(StringBuilder texto, String nome, long a, long b) {
        texto.append(String.format(LOCAL, "%-30s %15d %15d%n", nome, a, b));
    }
    private String formatarVariacao(double valor) {
        return Double.isNaN(valor) ? "N/D (base zero)" : String.format(LOCAL, "%+.2f%%", valor);
    }
    private void variacao(StringBuilder texto, String nome, String unidade, double sem, double com) {
        double percentual = calcularVariacaoPercentual(sem, com);
        texto.append(nome).append(":\n")
                .append(String.format(LOCAL, "  SEM: %.2f %s%n", sem, unidade))
                .append(String.format(LOCAL, "  COM: %.2f %s%n", com, unidade))
                .append("  Variação: ").append(formatarVariacao(percentual)).append('\n')
                .append("  Resultado: ").append(interpretarVariacao(percentual)).append("\n\n");
    }
    private String interpretarVariacao(double valor) {
        if (Double.isNaN(valor)) return "não definido (base zero)";
        if (Math.abs(valor) < 0.005) return "estabilidade (0,00%)";
        return String.format(LOCAL, "%s de %.2f%%", valor < 0 ? "redução" : "aumento", Math.abs(valor));
    }
    private long soma(ResultadoSimulacao resultado, java.util.function.ToIntFunction<ResultadoDia> medida) {
        return resultado.getResultadosDias().stream().mapToLong(d -> medida.applyAsInt(d)).sum();
    }
    private int maximoCirculando(ResultadoSimulacao resultado) {
        return resultado.getResultadosDias().stream().flatMap(d -> d.getSerieVeiculosCirculando().values().stream())
                .mapToInt(Integer::intValue).max().orElse(0);
    }
    private int maximoVias(ResultadoSimulacao resultado) {
        return resultado.getResultadosDias().stream().mapToInt(ResultadoDia::getMaximoViasCongestionadas).max().orElse(0);
    }
    private double mediaAmostras(ResultadoSimulacao resultado, ToDoubleFunction<ResultadoDia> medida) {
        long quantidade = resultado.getResultadosDias().stream().mapToLong(ResultadoDia::getQuantidadeAmostrasCongestionamento).sum();
        return quantidade == 0 ? 0 : resultado.getResultadosDias().stream()
                .mapToDouble(d -> medida.applyAsDouble(d) * d.getQuantidadeAmostrasCongestionamento()).sum() / quantidade;
    }
    private void topVias(StringBuilder texto, String cenario, ResultadoSimulacao resultado) {
        texto.append("\nTOP 10 VIAS MAIS UTILIZADAS - ").append(cenario).append('\n')
                .append(String.format("%-12s %14s %14s %12s %14s%n",
                        "Via", "Ocup. média", "Ocup. máxima", "Entradas", "Tempo >100%"));
        for (DiagnosticoVia via : resultado.getTopViasMaisUtilizadas(10)) {
            texto.append(String.format(LOCAL, "%-12s %13.2f%% %13.2f%% %12d %13.2f%%%n", via.viaId(),
                    via.ocupacaoMediaPercentual(), via.ocupacaoMaximaPercentual(), via.totalEntradas(),
                    via.percentualTempoAcimaCapacidade()));
        }
    }
}
