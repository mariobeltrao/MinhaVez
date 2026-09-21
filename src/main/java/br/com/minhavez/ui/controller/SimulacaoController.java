package br.com.minhavez.ui.controller;

import br.com.minhavez.enums.Regiao;
import br.com.minhavez.model.Cenario;
import br.com.minhavez.service.GeradorCenario;
import br.com.minhavez.snapshot.SnapshotSimulacao;
import br.com.minhavez.ui.*;
import br.com.minhavez.ui.component.DialogoConfiguracao;
import br.com.minhavez.ui.component.MapaCidade;
import br.com.minhavez.ui.runner.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

public final class SimulacaoController {
    @FXML private StackPane mapaHost, mapaSemHost, mapaComHost;
    @FXML private HBox individualPane, comparacaoPane;
    @FXML private Label diaLabel, horarioLabel, cenarioLabel, mensagemLabel, estadoLabel;
    @FXML private Label circulandoLabel, velocidadeLabel, ocupacaoLabel, viasAcimaLabel;
    @FXML private Label rodizioLabel, grupoLabel, restritosLabel;
    @FXML private Label norteLabel, lesteLabel, centroLabel, sulLabel;
    @FXML private Label semCirculandoLabel, semVelocidadeLabel, semOcupacaoLabel;
    @FXML private Label comCirculandoLabel, comVelocidadeLabel, comOcupacaoLabel;
    @FXML private Label timelineSelecionadaLabel;
    @FXML private ProgressBar norteBar, lesteBar, centroBar, sulBar, processadoBar;
    @FXML private Slider timelineSlider;
    @FXML private Button configuracoesButton, iniciarButton, pausarButton, continuarButton, aoVivoButton, pararButton;
    @FXML private ToggleGroup velocidadeGroup, modoGroup;

    private final MapaCidade mapaIndividual = new MapaCidade();
    private final MapaCidade mapaSem = new MapaCidade();
    private final MapaCidade mapaCom = new MapaCidade();
    private final HistoricoSnapshots historico = new HistoricoSnapshots();
    private final AtomicReference<SnapshotComparacao> snapshotPendente = new AtomicReference<>();
    private final AtomicBoolean atualizacaoAgendada = new AtomicBoolean();
    private SimulacaoVisualRunner runner = new SimulacaoVisualRunner();
    private ConfiguracaoUi configuracao = new ConfiguracaoUi(1000, 28, 12345);
    private Consumer<ResultadoExecucao> aoConcluir;
    private SnapshotComparacao snapshotExibido;
    private ModoVisualizacao modo = ModoVisualizacao.COMPARACAO;
    private boolean ajustandoTimeline;

    @FXML private void initialize() {
        mapaHost.getChildren().add(mapaIndividual);
        mapaSemHost.getChildren().add(mapaSem);
        mapaComHost.getChildren().add(mapaCom);
        velocidadeGroup.selectedToggleProperty().addListener((obs, anterior, atual) -> {
            if (atual != null) runner.definirVelocidade(
                    VelocidadeReproducao.porRotulo(atual.getUserData().toString()));
        });
        modoGroup.selectedToggleProperty().addListener((obs, anterior, atual) -> {
            if (atual != null) {
                modo = ModoVisualizacao.valueOf(atual.getUserData().toString());
                atualizarModo();
            }
        });
        timelineSlider.setOnMousePressed(evento -> entrarReplay());
        timelineSlider.setOnMouseReleased(evento -> selecionarTempo());
        timelineSlider.valueChangingProperty().addListener((obs, anterior, alterando) -> {
            if (alterando) entrarReplay();
            else if (anterior) selecionarTempo();
        });
        timelineSlider.valueProperty().addListener((obs, anterior, atual) -> {
            if (!ajustandoTimeline) timelineSelecionadaLabel.setText(formatarPosicao(atual.longValue()));
        });
        prepararTimeline();
        atualizarControles(EstadoExecucao.PRONTO);
    }

    public void configurar(Consumer<ResultadoExecucao> aoConcluir) {
        this.aoConcluir = Objects.requireNonNull(aoConcluir);
        mensagemLabel.setText(resumoConfiguracao(configuracao));
    }

    @FXML private void abrirConfiguracoes() {
        EstadoExecucao anterior = runner.getEstado();
        atualizarControles(EstadoExecucao.CONFIGURANDO);
        DialogoConfiguracao.mostrar(mapaHost.getScene() == null ? null : mapaHost.getScene().getWindow(), configuracao)
                .ifPresent(nova -> {
            configuracao = nova;
            prepararTimeline();
            mensagemLabel.setText("Configuração pronta: " + resumoConfiguracao(configuracao));
        });
        atualizarControles(anterior == EstadoExecucao.CONFIGURANDO ? EstadoExecucao.PRONTO : anterior);
    }

    @FXML private void iniciarExecucao() {
        EstadoExecucao estado = runner.getEstado();
        if (estado == EstadoExecucao.EXECUTANDO || estado == EstadoExecucao.PAUSADO || estado == EstadoExecucao.REPLAY) return;
        if (!confirmarInicio()) return;
        if (estado == EstadoExecucao.PARADO || estado == EstadoExecucao.FINALIZADO || estado == EstadoExecucao.ERRO) {
            runner.close();
            runner = new SimulacaoVisualRunner();
            Toggle selecionado = velocidadeGroup.getSelectedToggle();
            if (selecionado != null) runner.definirVelocidade(
                    VelocidadeReproducao.porRotulo(selecionado.getUserData().toString()));
        }
        Cenario base = new GeradorCenario(configuracao.quantidadeVeiculos())
                .gerarCenario(configuracao.semente(), configuracao.quantidadeDias());
        historico.limpar();
        snapshotExibido = null;
        prepararTimeline();
        limparMetricas();
        runner.iniciar(base, configuracao, this::receberSnapshot, this::mostrarMensagem,
                this::publicarEstado, resultado -> Platform.runLater(() -> aoConcluir.accept(resultado)),
                this::mostrarErro);
    }

    private boolean confirmarInicio() {
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar simulação");
        confirmacao.setHeaderText("Iniciar simulação?");
        confirmacao.setContentText("Veículos: %d%nDias: %d%nSeed: %d%n%nSEM e COM rodízio serão processados simultaneamente."
                .formatted(configuracao.quantidadeVeiculos(), configuracao.quantidadeDias(), configuracao.semente()));
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType iniciar = new ButtonType("Confirmar e iniciar", ButtonBar.ButtonData.OK_DONE);
        confirmacao.getButtonTypes().setAll(cancelar, iniciar);
        if (mapaHost.getScene() != null) confirmacao.initOwner(mapaHost.getScene().getWindow());
        return confirmacao.showAndWait().filter(iniciar::equals).isPresent();
    }

    @FXML private void pausar() { runner.pausar(this::publicarEstado); }
    @FXML private void continuarExecucao() {
        if (runner.getEstado() == EstadoExecucao.REPLAY) exibirMaisRecente(false);
        runner.continuar(this::publicarEstado);
    }
    @FXML private void voltarAoMaisRecente() {
        exibirMaisRecente(true);
        runner.voltarAoMaisRecente(this::publicarEstado);
    }
    @FXML private void parar() {
        runner.parar(this::publicarEstado);
        mensagemLabel.setText("Simulação parada. Inicie novamente para gerar uma nova comparação.");
    }

    private void entrarReplay() {
        if (historico.maisRecente().isEmpty()) return;
        runner.entrarReplay(this::publicarEstado);
    }

    private void selecionarTempo() {
        if (ajustandoTimeline || historico.maisRecente().isEmpty()) return;
        long solicitada = Math.min(Math.round(timelineSlider.getValue()), historico.posicaoMaxima());
        historico.buscar(solicitada).ifPresent(snapshot -> {
            runner.entrarReplay(this::publicarEstado);
            aplicar(snapshot, true);
            mensagemLabel.setText("Replay de um estado já processado. Continue ou volte ao ponto mais recente.");
        });
    }

    private void exibirMaisRecente(boolean manterPausado) {
        historico.maisRecente().ifPresent(snapshot -> aplicar(snapshot, false));
        mensagemLabel.setText(manterPausado
                ? "Ponto mais recente exibido. Use CONTINUAR para retomar."
                : "Retomando a partir do ponto mais recente processado.");
    }

    private void receberSnapshot(SnapshotComparacao snapshot) {
        historico.registrar(snapshot);
        snapshotPendente.set(snapshot);
        agendarAtualizacao();
    }

    private void agendarAtualizacao() {
        if (atualizacaoAgendada.compareAndSet(false, true)) {
            Platform.runLater(() -> {
                try {
                    SnapshotComparacao maisRecente = snapshotPendente.getAndSet(null);
                    if (maisRecente != null && runner.getEstado() != EstadoExecucao.REPLAY) aplicar(maisRecente, false);
                } finally {
                    atualizacaoAgendada.set(false);
                    if (snapshotPendente.get() != null) agendarAtualizacao();
                }
            });
        }
    }

    private void aplicar(SnapshotComparacao snapshot, boolean replay) {
        snapshotExibido = snapshot;
        SnapshotSimulacao referencia = snapshot.semRodizio();
        diaLabel.setText("Dia %02d / %02d".formatted(referencia.diaAtual() + 1, referencia.quantidadeDias()));
        horarioLabel.setText(formatarHorario(referencia.tempoAtualSegundos()));
        if (modo == ModoVisualizacao.COMPARACAO) aplicarComparacao(snapshot);
        else aplicarIndividual(modo == ModoVisualizacao.SEM ? snapshot.semRodizio() : snapshot.comRodizio());
        long posicao = snapshot.posicaoTimeline();
        definirTimeline(posicao);
        timelineSelecionadaLabel.setText("Dia %02d • %s".formatted(
                referencia.diaAtual() + 1, formatarHorario(referencia.tempoAtualSegundos())));
        processadoBar.setProgress(timelineSlider.getMax() == 0 ? 0
                : Math.clamp(historico.posicaoMaxima() / timelineSlider.getMax(), 0, 1));
        if (replay) estadoLabel.setText(EstadoExecucao.REPLAY.name());
    }

    private void aplicarComparacao(SnapshotComparacao snapshot) {
        mapaSem.aplicar(snapshot.semRodizio());
        mapaCom.aplicar(snapshot.comRodizio());
        preencherResumoComparacao(snapshot.semRodizio(), semCirculandoLabel, semVelocidadeLabel, semOcupacaoLabel);
        preencherResumoComparacao(snapshot.comRodizio(), comCirculandoLabel, comVelocidadeLabel, comOcupacaoLabel);
    }

    private void preencherResumoComparacao(SnapshotSimulacao snapshot, Label circulando, Label velocidade, Label ocupacao) {
        circulando.setText(Integer.toString(snapshot.quantidadeVeiculosCirculando()));
        velocidade.setText(formatarDecimal(snapshot.velocidadeMediaInstantaneaKmh(), " km/h"));
        ocupacao.setText(formatarDecimal(snapshot.ocupacaoMediaInstantaneaPercentual(), "%"));
    }

    private void aplicarIndividual(SnapshotSimulacao snapshot) {
        mapaIndividual.aplicar(snapshot);
        cenarioLabel.setText(snapshot.rodizioAtivo() ? "COM RODÍZIO" : "SEM RODÍZIO");
        circulandoLabel.setText(Integer.toString(snapshot.quantidadeVeiculosCirculando()));
        velocidadeLabel.setText(formatarDecimal(snapshot.velocidadeMediaInstantaneaKmh(), " km/h"));
        ocupacaoLabel.setText(formatarDecimal(snapshot.ocupacaoMediaInstantaneaPercentual(), "%"));
        viasAcimaLabel.setText(Integer.toString(snapshot.quantidadeViasAcimaCapacidade()));
        rodizioLabel.setText(snapshot.rodizioAtivo() ? "ATIVO" : "DESATIVADO");
        grupoLabel.setText(snapshot.grupoRestrito() == null ? "NENHUM" : snapshot.grupoRestrito().name());
        restritosLabel.setText(Integer.toString(snapshot.quantidadeVeiculosRestritos()));
        atualizarRegiao(snapshot, Regiao.NORTE, norteLabel, norteBar);
        atualizarRegiao(snapshot, Regiao.LESTE, lesteLabel, lesteBar);
        atualizarRegiao(snapshot, Regiao.CENTRO, centroLabel, centroBar);
        atualizarRegiao(snapshot, Regiao.SUL, sulLabel, sulBar);
    }

    private void atualizarModo() {
        boolean comparando = modo == ModoVisualizacao.COMPARACAO;
        comparacaoPane.setVisible(comparando); comparacaoPane.setManaged(comparando);
        individualPane.setVisible(!comparando); individualPane.setManaged(!comparando);
        if (snapshotExibido != null) aplicar(snapshotExibido, runner.getEstado() == EstadoExecucao.REPLAY);
    }

    private void atualizarRegiao(SnapshotSimulacao snapshot, Regiao regiao, Label label, ProgressBar barra) {
        double valor = snapshot.ocupacaoPorRegiao().getOrDefault(regiao, 0.0);
        label.setText(formatarDecimal(valor, "%"));
        barra.setProgress(Math.clamp(valor / 100, 0, 1));
    }

    private void prepararTimeline() {
        ajustandoTimeline = true;
        timelineSlider.setMax(configuracao.quantidadeDias() * HistoricoSnapshots.TICKS_POR_DIA);
        timelineSlider.setValue(0);
        processadoBar.setProgress(0);
        timelineSelecionadaLabel.setText("Dia 01 • 05:00");
        ajustandoTimeline = false;
    }

    private void definirTimeline(long posicao) {
        ajustandoTimeline = true;
        timelineSlider.setValue(Math.min(posicao, timelineSlider.getMax()));
        ajustandoTimeline = false;
    }

    private String formatarPosicao(long posicao) {
        if (posicao <= 0) return "Dia 01 • 05:00";
        long dia = Math.min(configuracao.quantidadeDias() - 1, (posicao - 1) / HistoricoSnapshots.TICKS_POR_DIA);
        long tickNoDia = (posicao - 1) % HistoricoSnapshots.TICKS_POR_DIA + 1;
        long tempo = HistoricoSnapshots.INICIO_DIA_SEGUNDOS
                + tickNoDia * HistoricoSnapshots.PASSO_SIMULACAO_SEGUNDOS;
        return "Dia %02d • %s".formatted(dia + 1, formatarHorario(tempo));
    }

    private String formatarHorario(long segundos) { return "%02d:%02d".formatted(segundos / 3600, segundos % 3600 / 60); }
    private String formatarDecimal(double valor, String unidade) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f%s", valor, unidade);
    }
    private String resumoConfiguracao(ConfiguracaoUi valor) {
        return "%d veículos • %d dia(s) • seed %d".formatted(
                valor.quantidadeVeiculos(), valor.quantidadeDias(), valor.semente());
    }
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
        boolean ativa = estado == EstadoExecucao.EXECUTANDO || estado == EstadoExecucao.PAUSADO || estado == EstadoExecucao.REPLAY;
        configuracoesButton.setDisable(ativa);
        iniciarButton.setDisable(ativa || estado == EstadoExecucao.CONFIGURANDO);
        pausarButton.setDisable(estado != EstadoExecucao.EXECUTANDO);
        continuarButton.setDisable(estado != EstadoExecucao.PAUSADO && estado != EstadoExecucao.REPLAY);
        aoVivoButton.setDisable(estado != EstadoExecucao.REPLAY);
        pararButton.setDisable(!ativa);
        timelineSlider.setDisable(!ativa);
    }
    private void limparMetricas() {
        for (Label label : new Label[]{circulandoLabel, viasAcimaLabel, semCirculandoLabel, comCirculandoLabel}) label.setText("0");
        for (Label label : new Label[]{velocidadeLabel, semVelocidadeLabel, comVelocidadeLabel}) label.setText("0 km/h");
        for (Label label : new Label[]{ocupacaoLabel, semOcupacaoLabel, comOcupacaoLabel}) label.setText("0%");
    }
    public void encerrar() { runner.close(); }

    private enum ModoVisualizacao { SEM, COM, COMPARACAO }
}
