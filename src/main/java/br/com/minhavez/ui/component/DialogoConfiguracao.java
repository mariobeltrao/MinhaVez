package br.com.minhavez.ui.component;

import br.com.minhavez.ui.ConfiguracaoUi;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Formulário modal dos três parâmetros já existentes na antiga tela inicial. */
public final class DialogoConfiguracao {
    private DialogoConfiguracao() { }

    public static Optional<ConfiguracaoUi> mostrar(Window owner, ConfiguracaoUi atual) {
        Dialog<ConfiguracaoUi> dialogo = new Dialog<>();
        dialogo.setTitle("Configurações da simulação");
        dialogo.setHeaderText("Parâmetros usados igualmente nos cenários SEM e COM rodízio");
        if (owner != null) dialogo.initOwner(owner);

        TextField veiculos = new TextField(Integer.toString(atual.quantidadeVeiculos()));
        TextField seed = new TextField(Long.toString(atual.semente()));
        ToggleGroup diasGroup = new ToggleGroup();
        HBox periodos = new HBox(10);
        for (int dias : new int[]{1, 7, 28}) {
            RadioButton opcao = new RadioButton(dias + (dias == 1 ? " dia" : " dias"));
            opcao.setUserData(dias);
            opcao.setToggleGroup(diasGroup);
            opcao.setSelected(dias == atual.quantidadeDias());
            periodos.getChildren().add(opcao);
        }

        Label erro = new Label();
        erro.getStyleClass().add("error-label");
        GridPane campos = new GridPane();
        campos.setHgap(14);
        campos.setVgap(12);
        campos.setPadding(new Insets(8));
        campos.addRow(0, new Label("Quantidade de veículos"), veiculos);
        campos.addRow(1, new Label("Período"), periodos);
        campos.addRow(2, new Label("Seed"), seed);
        campos.add(erro, 0, 3, 2, 1);
        dialogo.getDialogPane().setContent(campos);
        dialogo.getDialogPane().getStylesheets().add(Objects.requireNonNull(
                DialogoConfiguracao.class.getResource("/css/minhavez.css")).toExternalForm());

        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType salvar = new ButtonType("Salvar configurações", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(cancelar, salvar);
        AtomicReference<ConfiguracaoUi> validada = new AtomicReference<>();
        Button botaoSalvar = (Button) dialogo.getDialogPane().lookupButton(salvar);
        botaoSalvar.addEventFilter(ActionEvent.ACTION, evento -> validar(
                veiculos, seed, diasGroup, erro, validada, evento));
        dialogo.setResultConverter(botao -> botao == salvar ? validada.get() : null);
        return dialogo.showAndWait();
    }

    private static void validar(TextField veiculos, TextField seed, ToggleGroup diasGroup, Label erro,
                                AtomicReference<ConfiguracaoUi> destino, ActionEvent evento) {
        try {
            int dias = (int) diasGroup.getSelectedToggle().getUserData();
            destino.set(new ConfiguracaoUi(Integer.parseInt(veiculos.getText().trim()), dias,
                    Long.parseLong(seed.getText().trim())));
            erro.setText("");
        } catch (NumberFormatException excecao) {
            erro.setText("Veículos deve ser inteiro positivo e seed deve ser um long válido.");
            evento.consume();
        } catch (IllegalArgumentException excecao) {
            erro.setText(excecao.getMessage());
            evento.consume();
        }
    }
}
