package br.com.minhavez.ui.controller;

import br.com.minhavez.ui.ConfiguracaoUi;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Objects;
import java.util.function.Consumer;

public final class InicioController {
    @FXML private TextField veiculosField;
    @FXML private TextField seedField;
    @FXML private ToggleGroup periodoGroup;
    @FXML private Label erroLabel;
    private Consumer<ConfiguracaoUi> aoIniciar;

    public void configurar(Consumer<ConfiguracaoUi> aoIniciar) {
        this.aoIniciar = Objects.requireNonNull(aoIniciar);
    }

    @FXML private void iniciar() {
        try {
            int veiculos = Integer.parseInt(veiculosField.getText().trim());
            long seed = Long.parseLong(seedField.getText().trim());
            RadioButton selecionado = (RadioButton) periodoGroup.getSelectedToggle();
            int dias = Integer.parseInt(selecionado.getUserData().toString());
            erroLabel.setText("");
            aoIniciar.accept(new ConfiguracaoUi(veiculos, dias, seed));
        } catch (NumberFormatException erro) {
            erroLabel.setText("Informe uma quantidade inteira positiva e uma seed do tipo long.");
        } catch (IllegalArgumentException erro) {
            erroLabel.setText(erro.getMessage());
        }
    }
}
