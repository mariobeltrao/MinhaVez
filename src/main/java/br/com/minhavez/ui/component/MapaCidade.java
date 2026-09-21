package br.com.minhavez.ui.component;

import br.com.minhavez.snapshot.*;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.util.*;

/** Visualização responsiva do grafo fictício; os nós são criados uma vez e apenas atualizados. */
public final class MapaCidade extends Pane {
    private final Map<String, ViaView> vias = new LinkedHashMap<>();
    private final Map<String, SnapshotVia> dadosVias = new LinkedHashMap<>();
    private final Map<String, PontoView> pontos = new LinkedHashMap<>();
    private final List<Circle> marcadores = new ArrayList<>();
    private List<SnapshotVeiculo> veiculos = List.of();

    public MapaCidade() {
        getStyleClass().add("mapa-cidade");
        setMinSize(520, 500);
    }

    public void aplicar(SnapshotSimulacao snapshot) {
        if (vias.isEmpty()) construir(snapshot.vias());
        snapshot.vias().forEach(v -> { dadosVias.put(v.id(), v); vias.get(v.id()).atualizar(v); });
        veiculos = snapshot.veiculosVisiveis();
        prepararMarcadores(veiculos.size());
        for (int i = 0; i < marcadores.size(); i++) marcadores.get(i).setVisible(i < veiculos.size());
        requestLayout();
    }

    private void construir(List<SnapshotVia> dados) {
        Set<String> codigos = new TreeSet<>(Comparator.comparingInt((String s) -> s.charAt(0)).thenComparingInt(MapaCidade::numero));
        dados.forEach(v -> { codigos.add(v.pontoA()); codigos.add(v.pontoB()); });
        for (SnapshotVia via : dados) {
            ViaView view = new ViaView(via);
            vias.put(via.id(), view);
            dadosVias.put(via.id(), via);
            getChildren().add(view);
        }
        for (String codigo : codigos) {
            PontoView view = new PontoView(codigo);
            pontos.put(codigo, view);
            getChildren().add(view);
        }
        for (String regiao : List.of("NORTE", "LESTE", "CENTRO", "SUL")) {
            Label label = new Label(regiao);
            label.getStyleClass().add("mapa-regiao");
            label.setUserData(regiao);
            getChildren().add(label);
        }
    }

    private static int numero(String codigo) { return Integer.parseInt(codigo.substring(1)); }
    private void prepararMarcadores(int quantidade) {
        while (marcadores.size() < quantidade) {
            Circle marcador = new Circle(2.7);
            marcador.getStyleClass().add("veiculo-marker");
            marcadores.add(marcador);
            getChildren().add(marcador);
        }
    }

    @Override protected void layoutChildren() {
        double largura = getWidth(), altura = getHeight();
        for (PontoView ponto : pontos.values()) {
            Point2D posicao = coordenada(ponto.getCodigo(), largura, altura);
            ponto.posicionar(posicao.getX(), posicao.getY());
        }
        for (SnapshotVia dado : dadosVias.values()) {
            Point2D a = coordenada(dado.pontoA(), largura, altura), b = coordenada(dado.pontoB(), largura, altura);
            vias.get(dado.id()).posicionar(a.getX(), a.getY(), b.getX(), b.getY());
        }
        for (int i = 0; i < veiculos.size(); i++) {
            SnapshotVeiculo veiculo = veiculos.get(i);
            Point2D a = coordenada(veiculo.pontoOrigem(), largura, altura);
            Point2D b = coordenada(veiculo.pontoDestino(), largura, altura);
            marcadores.get(i).setCenterX(a.getX() + veiculo.progresso() * (b.getX() - a.getX()));
            marcadores.get(i).setCenterY(a.getY() + veiculo.progresso() * (b.getY() - a.getY()));
        }
        for (javafx.scene.Node node : getChildren()) if (node instanceof Label label) posicionarRegiao(label, largura, altura);
    }

    private Point2D coordenada(String codigo, double largura, double altura) {
        char prefixo = codigo.charAt(0);
        int numero = numero(codigo);
        double origemX = (prefixo == 'L' || prefixo == 'S') ? 0.57 : 0.07;
        double origemY = (prefixo == 'C' || prefixo == 'S') ? 0.57 : 0.10;
        int coluna;
        double linha;
        if (numero <= 4) { coluna = numero - 1; linha = 0; }
        else if (numero <= 8) { coluna = numero - 5; linha = 1; }
        else { coluna = numero == 9 ? 1 : 2; linha = 2; }
        return new Point2D(largura * (origemX + coluna * 0.105), altura * (origemY + linha * 0.13));
    }
    private void posicionarRegiao(Label label, double largura, double altura) {
        String regiao = label.getUserData().toString();
        boolean direita = regiao.equals("LESTE") || regiao.equals("SUL");
        boolean baixo = regiao.equals("CENTRO") || regiao.equals("SUL");
        label.relocate(largura * (direita ? 0.57 : 0.07), altura * (baixo ? 0.52 : 0.05));
    }
}
