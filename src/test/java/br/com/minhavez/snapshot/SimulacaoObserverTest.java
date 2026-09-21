package br.com.minhavez.snapshot;

import br.com.minhavez.model.Cenario;
import br.com.minhavez.result.ResultadoDia;
import br.com.minhavez.service.GeradorCenario;
import br.com.minhavez.simulation.Simulacao;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimulacaoObserverTest {
    @Test void observarCadaTickNaoAlteraResultados() {
        Cenario base = new GeradorCenario(20).gerarCenario(12345, 1);
        ResultadoDia esperado = new Simulacao(base, false).executar().getResultadosDias().getFirst();
        List<SnapshotSimulacao> snapshots = new ArrayList<>();
        ResultadoDia observado = new Simulacao(base, false).executar(snapshots::add).getResultadosDias().getFirst();

        assertFalse(snapshots.isEmpty());
        assertEquals(18_030, snapshots.getFirst().tempoAtualSegundos());
        assertEquals(30, snapshots.get(1).tempoAtualSegundos() - snapshots.getFirst().tempoAtualSegundos());
        assertEquals(esperado.getSerieCongestionamentoCidade(), observado.getSerieCongestionamentoCidade());
        assertEquals(esperado.getTempoTotalViagensMinutos(), observado.getTempoTotalViagensMinutos(), 1e-9);
    }
}
