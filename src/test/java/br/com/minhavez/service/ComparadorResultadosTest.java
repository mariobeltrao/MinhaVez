package br.com.minhavez.service;

import br.com.minhavez.result.ResultadoSimulacao;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComparadorResultadosTest {
    @Test void variacaoPercentualEMedidasComBaseZero() {
        ComparadorResultados comparador = new ComparadorResultados();
        assertEquals(-25, comparador.calcularVariacaoPercentual(100, 75));
        assertEquals(25, comparador.calcularVariacaoPercentual(100, 125));
        assertEquals(0, comparador.calcularVariacaoPercentual(0, 0));
        assertTrue(Double.isNaN(comparador.calcularVariacaoPercentual(0, 10)));
        assertThrows(IllegalArgumentException.class, () -> comparador.calcularVariacaoPercentual(Double.NaN, 1));
    }
    @Test void relatorioInterpretaReducaoAumentoEEstabilidade() {
        ComparadorResultados comparador = new ComparadorResultados();
        ResultadoSimulacao sem = new ResultadoSimulacao(false);
        ResultadoSimulacao com = new ResultadoSimulacao(true);
        var coletorSem = new br.com.minhavez.simulation.ColetorMetricas();
        coletorSem.registrarConclusaoIda(100);
        coletorSem.registrarMovimento(100, 3600);
        var coletorCom = new br.com.minhavez.simulation.ColetorMetricas();
        coletorCom.registrarConclusaoIda(75);
        coletorCom.registrarMovimento(125, 3600);
        var diaSem = coletorSem.gerarResultadoDia(0, null);
        var diaCom = coletorCom.gerarResultadoDia(0, br.com.minhavez.enums.GrupoRodizio.A);
        sem.adicionarResultadoDia(diaSem);
        com.adicionarResultadoDia(diaCom);
        String relatorio = comparador.gerarRelatorioComparativo(sem, com);
        assertTrue(relatorio.contains("Resultado: redução de 25,00%"));
        assertTrue(relatorio.contains("Resultado: aumento de 25,00%"));
        assertTrue(relatorio.contains("Resultado: estabilidade (0,00%)"));
        assertTrue(relatorio.contains("SEM: 100,00 min"));
        assertTrue(relatorio.contains("COM: 75,00 min"));
    }
}
