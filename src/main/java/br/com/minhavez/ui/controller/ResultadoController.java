package br.com.minhavez.ui.controller;

import br.com.minhavez.enums.Regiao;
import br.com.minhavez.result.*;
import br.com.minhavez.service.ComparadorResultados;
import br.com.minhavez.ui.ResultadoExecucao;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

public final class ResultadoController {
    @FXML private Label periodoLabel;
    @FXML private GridPane metricasGrid, regioesGrid;
    @FXML private LineChart<String, Number> circulacaoChart, velocidadeChart, ocupacaoChart;
    @FXML private TableView<ViaLinha> viasTable;
    @FXML private TableColumn<ViaLinha, String> cenarioColumn, viaColumn;
    @FXML private TableColumn<ViaLinha, Number> ocupacaoMediaColumn, ocupacaoMaximaColumn, entradasColumn, tempoAcimaColumn;
    private Runnable aoNovaSimulacao;
    private final ComparadorResultados comparador = new ComparadorResultados();

    public void configurar(ResultadoExecucao execucao, Runnable aoNovaSimulacao) {
        this.aoNovaSimulacao = Objects.requireNonNull(aoNovaSimulacao);
        periodoLabel.setText("%d veículos • %d dia(s) • seed %d".formatted(
                execucao.configuracao().quantidadeVeiculos(), execucao.configuracao().quantidadeDias(),
                execucao.configuracao().semente()));
        preencherMetricas(execucao.semRodizio(), execucao.comRodizio());
        preencherGraficos(execucao.semRodizio(), execucao.comRodizio());
        preencherRegioes(execucao.semRodizio(), execucao.comRodizio());
        preencherVias(execucao.semRodizio(), execucao.comRodizio());
    }

    @FXML private void novaSimulacao() { aoNovaSimulacao.run(); }

    private void preencherMetricas(ResultadoSimulacao sem, ResultadoSimulacao com) {
        List<Metrica> metricas = List.of(
                new Metrica("TEMPO MÉDIO", "min", sem.calcularTempoMedioGeral(), com.calcularTempoMedioGeral()),
                new Metrica("TEMPO DE IDA", "min", sem.calcularTempoMedioIda(), com.calcularTempoMedioIda()),
                new Metrica("TEMPO DE VOLTA", "min", sem.calcularTempoMedioVolta(), com.calcularTempoMedioVolta()),
                new Metrica("VELOCIDADE MÉDIA", "km/h", sem.calcularVelocidadeMediaGeral(), com.calcularVelocidadeMediaGeral()),
                new Metrica("OCUPAÇÃO MÉDIA", "%", sem.calcularCongestionamentoMedioGeral(), com.calcularCongestionamentoMedioGeral()),
                new Metrica("PICO 17H–19H", "%", sem.getMaiorCongestionamentoPico(), com.getMaiorCongestionamentoPico()),
                new Metrica("MÁXIMO SIMULTÂNEO", "veículos", maximoCirculando(sem), maximoCirculando(com)),
                new Metrica("MÁX. VIAS > 100%", "vias", maximoVias(sem), maximoVias(com)),
                new Metrica("VEÍCULOS-DIA", "", soma(sem, ResultadoDia::getVeiculosQueCircularam), soma(com, ResultadoDia::getVeiculosQueCircularam)),
                new Metrica("VIAGENS CONCLUÍDAS", "", soma(sem, ResultadoDia::getQuantidadeViagensConcluidas), soma(com, ResultadoDia::getQuantidadeViagensConcluidas)));
        for (int i = 0; i < metricas.size(); i++) metricasGrid.add(criarCard(metricas.get(i)), i % 5, i / 5);
    }

    private VBox criarCard(Metrica metrica) {
        Label titulo = new Label(metrica.nome()); titulo.getStyleClass().add("metric-name");
        Label sem = new Label("SEM  " + formatar(metrica.sem(), metrica.unidade())); sem.getStyleClass().add("result-sem");
        Label com = new Label("COM  " + formatar(metrica.com(), metrica.unidade())); com.getStyleClass().add("result-com");
        double variacao = comparador.calcularVariacaoPercentual(metrica.sem(), metrica.com());
        String texto = Double.isNaN(variacao) ? "N/D" : String.format(Locale.forLanguageTag("pt-BR"), "%s %.1f%%",
                variacao < 0 ? "↓" : variacao > 0 ? "↑" : "→", Math.abs(variacao));
        Label diferenca = new Label(texto); diferenca.getStyleClass().add(variacao <= 0 ? "variation-good" : "variation-alert");
        VBox card = new VBox(7, titulo, sem, com, diferenca);
        card.getStyleClass().add("result-card");
        card.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);
        return card;
    }
    private String formatar(double valor, String unidade) {
        String numero = Math.abs(valor - Math.rint(valor)) < 1e-9
                ? String.format(Locale.forLanguageTag("pt-BR"), "%.0f", valor)
                : String.format(Locale.forLanguageTag("pt-BR"), "%.2f", valor);
        return unidade.isBlank() ? numero : numero + " " + unidade;
    }

    private void preencherGraficos(ResultadoSimulacao sem, ResultadoSimulacao com) {
        adicionarSeries(circulacaoChart, sem, com, ResultadoDia::getSerieVeiculosCirculando);
        adicionarSeries(velocidadeChart, sem, com, ResultadoDia::getSerieVelocidadeMedia);
        adicionarSeries(ocupacaoChart, sem, com, ResultadoDia::getSerieCongestionamentoCidade);
    }
    private <T extends Number> void adicionarSeries(LineChart<String, Number> grafico,
                                                     ResultadoSimulacao sem, ResultadoSimulacao com,
                                                     java.util.function.Function<ResultadoDia, Map<Long, T>> extrator) {
        grafico.setCreateSymbols(false);
        grafico.setAnimated(false);
        grafico.getData().add(serie("SEM", sem, extrator));
        grafico.getData().add(serie("COM", com, extrator));
    }
    private <T extends Number> XYChart.Series<String, Number> serie(String nome, ResultadoSimulacao resultado,
                                                                   java.util.function.Function<ResultadoDia, Map<Long, T>> extrator) {
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName(nome);
        long total = resultado.getResultadosDias().stream().mapToLong(d -> extrator.apply(d).size()).sum();
        int passo = (int) Math.max(1, Math.ceil(total / 550.0));
        int indice = 0;
        for (ResultadoDia dia : resultado.getResultadosDias()) {
            for (Map.Entry<Long, T> ponto : extrator.apply(dia).entrySet()) {
                if (indice++ % passo == 0) serie.getData().add(new XYChart.Data<>(
                        "D%02d %02d:%02d".formatted(dia.getDia() + 1, ponto.getKey() / 3600, ponto.getKey() % 3600 / 60),
                        ponto.getValue()));
            }
        }
        return serie;
    }

    private void preencherRegioes(ResultadoSimulacao sem, ResultadoSimulacao com) {
        int linha = 1;
        for (Regiao regiao : Regiao.values()) {
            adicionarRegiao(linha++, regiao.name(), media(sem, d -> d.getCongestionamentoMedioPorRegiao().getOrDefault(regiao, 0.0)),
                    media(com, d -> d.getCongestionamentoMedioPorRegiao().getOrDefault(regiao, 0.0)));
        }
        adicionarRegiao(linha, "INTERREGIONAIS", media(sem, ResultadoDia::getCongestionamentoMedioInterregional),
                media(com, ResultadoDia::getCongestionamentoMedioInterregional));
    }
    private void adicionarRegiao(int linha, String nome, double sem, double com) {
        regioesGrid.add(new Label(nome), 0, linha);
        regioesGrid.add(new Label(String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", sem)), 1, linha);
        regioesGrid.add(new Label(String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", com)), 2, linha);
    }

    private void preencherVias(ResultadoSimulacao sem, ResultadoSimulacao com) {
        cenarioColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().cenario()));
        viaColumn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().via()));
        ocupacaoMediaColumn.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().ocupacaoMedia()));
        ocupacaoMaximaColumn.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().ocupacaoMaxima()));
        entradasColumn.setCellValueFactory(d -> new SimpleLongProperty(d.getValue().entradas()));
        tempoAcimaColumn.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().tempoAcima()));
        sem.getTopViasMaisUtilizadas(10).forEach(v -> viasTable.getItems().add(ViaLinha.de("SEM", v)));
        com.getTopViasMaisUtilizadas(10).forEach(v -> viasTable.getItems().add(ViaLinha.de("COM", v)));
    }

    private int maximoCirculando(ResultadoSimulacao r) { return r.getResultadosDias().stream()
            .flatMap(d -> d.getSerieVeiculosCirculando().values().stream()).mapToInt(Integer::intValue).max().orElse(0); }
    private int maximoVias(ResultadoSimulacao r) { return r.getResultadosDias().stream()
            .mapToInt(ResultadoDia::getMaximoViasCongestionadas).max().orElse(0); }
    private long soma(ResultadoSimulacao r, ToIntFunction<ResultadoDia> f) {
        return r.getResultadosDias().stream().mapToLong(d -> f.applyAsInt(d)).sum();
    }
    private double media(ResultadoSimulacao r, ToDoubleFunction<ResultadoDia> f) {
        long n = r.getResultadosDias().stream().mapToLong(ResultadoDia::getQuantidadeAmostrasCongestionamento).sum();
        return n == 0 ? 0 : r.getResultadosDias().stream()
                .mapToDouble(d -> f.applyAsDouble(d) * d.getQuantidadeAmostrasCongestionamento()).sum() / n;
    }

    private record Metrica(String nome, String unidade, double sem, double com) { }
    public record ViaLinha(String cenario, String via, double ocupacaoMedia, double ocupacaoMaxima,
                           long entradas, double tempoAcima) {
        static ViaLinha de(String cenario, DiagnosticoVia via) {
            return new ViaLinha(cenario, via.viaId(), via.ocupacaoMediaPercentual(),
                    via.ocupacaoMaximaPercentual(), via.totalEntradas(), via.percentualTempoAcimaCapacidade());
        }
    }
}
