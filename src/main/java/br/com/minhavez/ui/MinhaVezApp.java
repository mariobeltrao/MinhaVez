package br.com.minhavez.ui;

import br.com.minhavez.ui.controller.InicioController;
import br.com.minhavez.ui.controller.SimulacaoController;
import br.com.minhavez.ui.controller.ResultadoController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** Entrada gráfica separada da CLI preservada em {@code br.com.minhavez.Main}. */
public final class MinhaVezApp extends Application {
    private Stage janela;
    private SimulacaoController simulacaoController;

    @Override public void start(Stage stage) throws IOException {
        janela = stage;
        janela.setTitle("Minha Vez — Central de Mobilidade Urbana");
        janela.setMinWidth(1024);
        janela.setMinHeight(700);
        janela.setOnCloseRequest(evento -> encerrarSimulacao());
        mostrarInicio();
        janela.show();
    }

    private void mostrarInicio() throws IOException {
        encerrarSimulacao();
        FXMLLoader loader = carregar("inicio.fxml");
        Parent raiz = loader.load();
        InicioController controller = loader.getController();
        controller.configurar(this::iniciarSimulacao);
        trocarCena(raiz);
    }

    private void iniciarSimulacao(ConfiguracaoUi configuracao) {
        try {
            FXMLLoader loader = carregar("simulacao.fxml");
            Parent raiz = loader.load();
            simulacaoController = loader.getController();
            trocarCena(raiz);
            simulacaoController.configurar(configuracao, this::simulacaoConcluida);
        } catch (IOException erro) {
            throw new IllegalStateException("Não foi possível abrir a simulação", erro);
        }
    }

    private void simulacaoConcluida(ResultadoExecucao resultado) {
        try {
            encerrarSimulacao();
            FXMLLoader loader = carregar("resultado.fxml");
            Parent raiz = loader.load();
            ResultadoController controller = loader.getController();
            controller.configurar(resultado, () -> {
                try { mostrarInicio(); }
                catch (IOException erro) { throw new IllegalStateException(erro); }
            });
            janela.setTitle("Minha Vez — comparação concluída");
            trocarCena(raiz);
        } catch (IOException erro) {
            throw new IllegalStateException("Não foi possível abrir os resultados", erro);
        }
    }
    private void encerrarSimulacao() {
        if (simulacaoController != null) {
            simulacaoController.encerrar();
            simulacaoController = null;
        }
    }

    private FXMLLoader carregar(String arquivo) {
        return new FXMLLoader(MinhaVezApp.class.getResource("/fxml/" + arquivo));
    }

    private void trocarCena(Parent raiz) {
        Scene cena = new Scene(raiz, 1440, 900);
        cena.getStylesheets().add(MinhaVezApp.class.getResource("/css/minhavez.css").toExternalForm());
        janela.setScene(cena);
    }

    public static void main(String[] args) { launch(args); }
}
