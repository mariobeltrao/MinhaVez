package br.com.minhavez.ui.component;

import br.com.minhavez.enums.EstadoTransito;
import br.com.minhavez.enums.TipoVia;
import br.com.minhavez.snapshot.SnapshotVia;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.Locale;

public final class ViaView extends Group {
    private final Line linha = new Line();
    private final Line areaHover = new Line();
    private final Tooltip tooltip = new Tooltip();
    private EstadoTransito estado;
    private FadeTransition pulsacao;

    public ViaView(SnapshotVia via) {
        getStyleClass().add("via-view");
        linha.getStyleClass().addAll("via-line", via.tipo() == TipoVia.PRINCIPAL ? "via-principal" : "via-secundaria");
        areaHover.getStyleClass().add("via-hover");
        Tooltip.install(areaHover, tooltip);
        getChildren().addAll(linha, areaHover);
        atualizar(via);
    }

    public void atualizar(SnapshotVia via) {
        tooltip.setText(String.format(Locale.forLanguageTag("pt-BR"),
                "Via %s%nTipo: %s%nVeículos: %d%nCapacidade: %d%nOcupação: %.1f%%%nVelocidade: %.1f km/h%nEstado: %s",
                via.id(), formatar(via.tipo()), via.quantidadeVeiculos(), via.capacidade(),
                via.ocupacaoPercentual(), via.velocidadeAtualKmh(), via.estado()));
        if (estado != via.estado()) {
            if (estado != null) getStyleClass().remove("estado-" + estado.name().toLowerCase(Locale.ROOT));
            estado = via.estado();
            getStyleClass().add("estado-" + estado.name().toLowerCase(Locale.ROOT));
            transicionar();
        }
    }

    private String formatar(TipoVia tipo) {
        String texto = tipo.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
    private void transicionar() {
        if (pulsacao != null) { pulsacao.stop(); pulsacao = null; setOpacity(1); }
        FadeTransition entrada = new FadeTransition(Duration.millis(220), this);
        entrada.setFromValue(0.55);
        entrada.setToValue(1);
        entrada.play();
        if (estado == EstadoTransito.COLAPSO) {
            pulsacao = new FadeTransition(Duration.millis(850), this);
            pulsacao.setFromValue(1);
            pulsacao.setToValue(0.62);
            pulsacao.setAutoReverse(true);
            pulsacao.setCycleCount(FadeTransition.INDEFINITE);
            pulsacao.play();
        }
    }
    public void posicionar(double x1, double y1, double x2, double y2) {
        linha.setStartX(x1); linha.setStartY(y1); linha.setEndX(x2); linha.setEndY(y2);
        areaHover.setStartX(x1); areaHover.setStartY(y1); areaHover.setEndX(x2); areaHover.setEndY(y2);
    }
}
