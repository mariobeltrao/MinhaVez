package br.com.minhavez.snapshot;

import br.com.minhavez.enums.*;
import br.com.minhavez.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FabricaSnapshotTest {
    @Test void copiaValoresDirecaoEProgressoSemExporColecoesMutaveis() {
        Ponto a = new Ponto("A", Regiao.NORTE), b = new Ponto("B", Regiao.NORTE);
        Cidade cidade = new Cidade();
        cidade.adicionarPonto(a);
        cidade.adicionarPonto(b);
        Via via = new Via("A-B", a, b, TipoVia.PRINCIPAL, 2);
        cidade.adicionarVia(via);
        Veiculo veiculo = veiculo(1, a, b);
        veiculo.iniciarIda(new Rota(a, b, List.of(via)), 18_000);
        veiculo.avancar(0.5);
        via.entrarVeiculo();

        SnapshotSimulacao snapshot = new FabricaSnapshot().criar(0, 1, 18_030, true,
                cidade, List.of(veiculo), 0.5, 30);

        assertEquals(0.25, snapshot.veiculosVisiveis().getFirst().progresso(), 1e-9);
        assertEquals("A", snapshot.veiculosVisiveis().getFirst().pontoOrigem());
        assertEquals("B", snapshot.veiculosVisiveis().getFirst().pontoDestino());
        assertEquals(1, snapshot.quantidadeVeiculosCirculando());
        assertEquals(GrupoRodizio.A, snapshot.grupoRestrito());
        assertEquals(6.6666666667, snapshot.vias().getFirst().ocupacaoPercentual(), 1e-9);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.vias().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.ocupacaoPorRegiao().clear());
    }

    @Test void selecaoVisualEhDeterministicaELimitada() {
        Ponto a = new Ponto("A", Regiao.NORTE), b = new Ponto("B", Regiao.NORTE);
        Via via = new Via("A-B", a, b, TipoVia.PRINCIPAL, 1);
        List<Veiculo> veiculos = new ArrayList<>();
        for (int id : List.of(8, 2, 5)) {
            Veiculo veiculo = veiculo(id, a, b);
            veiculo.iniciarIda(new Rota(a, b, List.of(via)), 18_000);
            veiculos.add(veiculo);
        }
        assertEquals(List.of(2, 5), new SeletorVeiculosVisuais().selecionar(veiculos, 2)
                .stream().map(Veiculo::getId).toList());
    }

    private Veiculo veiculo(int id, Ponto a, Ponto b) {
        return new Veiculo(id, "AAA-%04d".formatted(id), GrupoRodizio.values()[(id - 1) % 4],
                a, b, LocalTime.of(5, 0), LocalTime.of(17, 0));
    }
}
