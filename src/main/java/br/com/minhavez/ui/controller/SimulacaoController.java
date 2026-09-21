package br.com.minhavez.ui.controller;

import br.com.minhavez.enums.Regiao;
import br.com.minhavez.model.Cenario;
import br.com.minhavez.service.GeradorCenario;
import br.com.minhavez.snapshot.SnapshotSimulacao;
import br.com.minhavez.ui.*;
import br.com.minhavez.ui.component.MapaCidade;
import br.com.minhavez.ui.runner.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

public final class SimulacaoController {
    @FXML private StackPane mapaHost;
    @FXML private Label diaLabel, horarioLabel, cenarioLabel, mensagemLabel, estadoLabel;
    @FXML private Label circulandoLabel, velocidadeLabel, ocupacaoLabel, viasAcimaLabel;
    @FXML private Label rodizioLabel, grupoLabel, restritosLabel;
    @FXML private Label norteLabel, lesteLabel, centroLabel, sulLabel;
    @FXML private ProgressBar norteBar, lesteBar, centroBar, sulBar, timelineBar;
    @FXML private Button iniciarButton, pausarButton, continuarButton, pararButton;
    @FXML private ToggleGroup velocidadeGroup;

    private final MapaCidade mapa = new MapaCidade();
    private final AtomicReference<SnapshotSimulacao> snapshotPendente = new AtomicReference<>();
    private final AtomicBoolean atualizacaoAgendada = new AtomicBoolean();
    private SimulacaoVisualRunner runner = new SimulacaoVisualRunner();
    private ConfiguracaoUi configuracao;
    private Cenario base;
    private Consumer<ResultadoExecucao> aoConcluir;

    @FXML private void initialize() {
        mapaHost.getChildren().add(mapa);
        velocidadeGroup.selectedToggleProperty().addListener((obs, anterior, atual) -> {
            if (atual != null) runner.definirVelocidade(
                    VelocidadeReproducao.porRotulo(atual.getUserData().toString()));
        });
        atualizarControles(EstadoExecucao.PRONTO);
    }

    public void configurar(ConfiguracaoUi configuracao, Consumer<ResultadoExecucao> aoConcluir) {
        this.configuracao = Objects.requireNonNull(configuracao);
        this.aoConcluir = Objects.requireNonNull(aoConcluir);
        base = new GeradorCenario(configuracao.quantidadeVeiculos())
                .gerarCenario(configuracao.semente(), configuracao.quantidadeDias());
        iniciarExecucao();
    }

    @FXML private void iniciarExecucao() {
        if (configuracao == null || runner.getEstado() == EstadoExecucao.EXECUTANDO
                || runner.getEstado() == EstadoExecucao.PAUSADO) return;
        runner.iniciar(base, configuracao, this::receberSnapshot, this::mostrarMensagem,
                this::publicarEstado, resultado -> Platform.runLater(() -> aoConcluir.accept(resultado)),
                this::mostrarErro);
    }
    @FXML private void pausar() { runner.pausar(this::publicarEstado); }
    @FXML private void continuarExecucao() { runner.continuar(this::publicarEstado); }
    @FXML private void parar() { runner.parar(this::publicarEstado); }

    private void receberSnapshot(SnapshotSimulacao snapshot) {
        snapshotPendente.set(snapshot);
        if (atualizacaoAgendada.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                try {
                    SnapshotSimulacao maisRecente = snapshotPendente.getAndSet(null);
                    if (maisRecente != null) aplicar(maisRecente);
                } finally {
                    atualizacaoAgendada.set(false);
                    if (snapshotPendente.get() != null) receberSnapshot(snapshotPendente.get());
                }
            });
        }
    }

    private void aplicar(SnapshotSimulacao snapshot) {
        mapa.aplicar(snapshot);
        diaLabel.setText("Dia %02d / %02d".formatted(snapshot.diaAtual() + 1, snapshot.quantidadeDias()));
        horarioLabel.setText(formatarHorario(snapshot.tempoAtualSegundos()));
        cenarioLabel.setText(snapshot.rodizioAtivo() ? "COM RODÍZIO" : "SEM RODÍZIO");
        circulandoLabel.setText(Integer.toString(snapshot.quantidadeVeiculosCirculando()));
        velocidadeLabel.setText(String.format(Locale.forLanguageTag("pt-BR"), "%.1f km/h", snapshot.velocidadeMediaInstantaneaKmh()));
        ocupacaoLabel.setText(String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", snapshot.ocupacaoMediaInstantaneaPercentual()));
        viasAcimaLabel.setText(Integer.toString(snapshot.quantidadeViasAcimaCapacidade()));
        rodizioLabel.setText(snapshot.rodizioAtivo() ? "ATIVO" : "DESATIVADO");
        grupoLabel.setText(snapshot.grupoRestrito() == null ? "NENHUM" : snapshot.grupoRestrito().name());
        restritosLabel.setText(Integer.toString(snapshot.quantidadeVeiculosRestritos()));
        atualizarRegiao(snapshot, Regiao.NORTE, norteLabel, norteBar);
        atualizarRegiao(snapshot, Regiao.LESTE, lesteLabel, lesteBar);
        atualizarRegiao(snapshot, Regiao.CENTRO, centroLabel, centroBar);
        atualizarRegiao(snapshot, Regiao.SUL, sulLabel, sulBar);
        double progresso = (snapshot.tempoAtualSegundos() - 5 * 3600.0) / (18 * 3600.0);
        timelineBar.setProgress(Math.clamp(progresso, 0, 1));
    }

    private void atualizarRegiao(SnapshotSimulacao snapshot, Regiao regiao, Label label, ProgressBar barra) {
        double valor = snapshot.ocupacaoPorRegiao().getOrDefault(regiao, 0.0);
        label.setText(String.format(Locale.forLanguageTag("pt-BR"), "%.1f%%", valor));
        barra.setProgress(Math.clamp(valor / 100, 0, 1));
    }
    private String formatarHorario(long segundos) { return "%02d:%02d".formatted(segundos / 3600, segundos % 3600 / 60); }
    private void mostrarMensagem(String mensagem) { Platform.runLater(() -> mensagemLabel.setText(mensagem)); }
    private void publicarEstado(EstadoExecucao estado) { Platform.runLater(() -> atualizarControles(estado)); }
    private void mostrarErro(Throwable erro) {
        Platform.runLater(() -> {
            mensagemLabel.setText("Falha na simulação: " + erro.getMessage());
            atualizarControles(EstadoExecucao.ERRO);
        });
    }
    private void atualizarControles(EstadoExecucao estado) {
        estadoLabel.setText(estado.name());
        iniciarButton.setDisable(estado == EstadoExecucao.EXECUTANDO || estado == EstadoExecucao.PAUSADO);
        pausarButton.setDisable(estado != EstadoExecucao.EXECUTANDO);
        continuarButton.setDisable(estado != EstadoExecucao.PAUSADO);
        pararButton.setDisable(estado != EstadoExecucao.EXECUTANDO && estado != EstadoExecucao.PAUSADO);
    }
    public void encerrar() { runner.close(); }
}
