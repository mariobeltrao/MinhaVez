package br.com.minhavez.ui.component;

import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public final class PontoView extends Group {
    private final String codigo;

    public PontoView(String codigo) {
        this.codigo = codigo;
        getStyleClass().add("ponto-view");
        Circle circulo = new Circle(4.5);
        circulo.getStyleClass().add("ponto-circle");
        Text texto = new Text(codigo);
        texto.getStyleClass().add("ponto-label");
        texto.setLayoutX(7);
        texto.setLayoutY(-6);
        getChildren().addAll(circulo, texto);
        Tooltip.install(this, new Tooltip("Ponto " + codigo));
    }
    public String getCodigo() { return codigo; }
    public void posicionar(double x, double y) { setLayoutX(x); setLayoutY(y); }
}
