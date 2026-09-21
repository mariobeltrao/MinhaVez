package br.com.minhavez.simulation;

import br.com.minhavez.enums.*;
import br.com.minhavez.model.*;
import br.com.minhavez.result.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MetricasTest {
    @Test void ocupacaoAgregadaPonderaPelaQuantidadeDeAmostras() {
        Cidade cidade = new Cidade();
        Ponto a = new Ponto("a", Regiao.NORTE), b = new Ponto("b", Regiao.NORTE);
        cidade.adicionarPonto(a);
        cidade.adicionarPonto(b);
        Via via = new Via("ab", a, b, TipoVia.SECUNDARIA, 1);
        cidade.adicionarVia(via);
        ColetorMetricas primeiro = new ColetorMetricas(), segundo = new ColetorMetricas();
        primeiro.registrarAmostra(30, cidade, List.of(), 0, 0);
        for (int i = 0; i < 10; i++) via.entrarVeiculo();
        segundo.registrarAmostra(30, cidade, List.of(), 0, 0);
        segundo.registrarAmostra(60, cidade, List.of(), 0, 0);
        segundo.registrarAmostra(90, cidade, List.of(), 0, 0);
        ResultadoSimulacao resultado = new ResultadoSimulacao(false);
        resultado.adicionarResultadoDia(primeiro.gerarResultadoDia(0, null));
        resultado.adicionarResultadoDia(segundo.gerarResultadoDia(1, null));
        assertEquals(750.0 / 7, resultado.calcularCongestionamentoMedioGeral(), 1e-9);
        assertEquals(75, resultado.calcularPercentualAmostrasComViaAcimaCapacidade(), 1e-9);
        assertEquals(75, resultado.calcularPercentualAmostrasComViaSevera(), 1e-9);
        assertEquals(0, resultado.calcularPercentualAmostrasComViaEmColapso(), 1e-9);
    }
    @Test void separaRegioesInterregionaisEPicoEMantemResultadoImutavel() {
        Cidade cidade = new Cidade();
        Ponto a = new Ponto("a", Regiao.NORTE), b = new Ponto("b", Regiao.NORTE), c = new Ponto("c", Regiao.LESTE);
        List.of(a, b, c).forEach(cidade::adicionarPonto);
        Via interna = new Via("ab", a, b, TipoVia.PRINCIPAL, 1);
        Via inter = new Via("bc", b, c, TipoVia.SECUNDARIA, 1);
        cidade.adicionarVia(interna);
        cidade.adicionarVia(inter);
        for (int i = 0; i < 20; i++) interna.entrarVeiculo();
        for (int i = 0; i < 15; i++) inter.entrarVeiculo();
        ColetorMetricas coletor = new ColetorMetricas();
        coletor.registrarAmostra(17 * 3600, cidade, List.of(), 1, 120);
        ResultadoDia resultado = coletor.gerarResultadoDia(0, null);
        assertEquals(3650.0 / 21, resultado.getCongestionamentoMedioPercentual(), 1e-9);
        assertEquals(400.0 / 3, resultado.getCongestionamentoMedioPorRegiao().get(Regiao.NORTE), 1e-9);
        assertEquals(0, resultado.getCongestionamentoMedioPorRegiao().get(Regiao.LESTE));
        assertEquals(1500.0 / 7, resultado.getCongestionamentoMedioInterregional(), 1e-9);
        assertEquals(3650.0 / 21, resultado.getCongestionamentoPicoPercentual(), 1e-9);
        assertEquals(2, resultado.getMaximoViasCongestionadas());
        assertEquals(1, resultado.getAmostrasComViaAcimaCapacidade());
        assertEquals(1, resultado.getAmostrasComViaSevera());
        assertEquals(1, resultado.getAmostrasComViaEmColapso());
        assertEquals(2, resultado.getSomaViasAcimaCapacidadeNoPico());
        assertEquals(1, resultado.getQuantidadeAmostrasNoPico());
        assertEquals(30, resultado.getSerieVelocidadeMedia().get(17L * 3600));
        cidade.resetarOcupacaoDasVias();
        coletor.registrarAmostra(19 * 3600, cidade, List.of(), 0, 0);
        assertEquals(1, resultado.getQuantidadeAmostrasCongestionamento());
        assertThrows(UnsupportedOperationException.class, () -> resultado.getSerieVeiculosCirculando().clear());
        assertThrows(UnsupportedOperationException.class, () -> resultado.getCongestionamentoMedioPorRegiao().clear());
    }
    @Test void agregaAnaliseDeSaturacaoNoPeriodo() {
        Cidade cidade = new Cidade();
        Ponto a = new Ponto("a", Regiao.NORTE), b = new Ponto("b", Regiao.NORTE);
        cidade.adicionarPonto(a);
        cidade.adicionarPonto(b);
        Via via = new Via("ab", a, b, TipoVia.PRINCIPAL, 1);
        cidade.adicionarVia(via);
        for (int i = 0; i < 23; i++) via.entrarVeiculo();
        ColetorMetricas coletor = new ColetorMetricas();
        coletor.registrarEntradaVia(via);
        coletor.registrarEntradaVia(via);
        coletor.registrarEntradaVia(via);
        coletor.registrarAmostra(17 * 3600, cidade, List.of(), 0, 0);
        cidade.resetarOcupacaoDasVias();
        coletor.registrarAmostra(17 * 3600 + 30, cidade, List.of(), 0, 0);
        ResultadoSimulacao resultado = new ResultadoSimulacao(false);
        resultado.adicionarResultadoDia(coletor.gerarResultadoDia(0, null));
        assertEquals(50, resultado.calcularPercentualAmostrasComViaAcimaCapacidade(), 1e-9);
        assertEquals(0, resultado.calcularPercentualAmostrasComViaSevera(), 1e-9);
        assertEquals(50, resultado.calcularPercentualAmostrasComViaEmColapso(), 1e-9);
        assertEquals(0.5, resultado.calcularMediaViasAcimaCapacidadeNoPico(), 1e-9);
        assertEquals(153.3333333333, resultado.calcularOcupacaoMediaViasAtivas(), 1e-9);
        assertEquals(0.5, resultado.calcularNumeroMedioViasAtivas(), 1e-9);
        assertEquals(100, resultado.calcularPercentualViasAtivasCongestionadas(), 1e-9);
        assertEquals(0.5, resultado.calcularTempoMedioAcimaCapacidadePorViaMinutos(), 1e-9);
        DiagnosticoVia diagnostico = resultado.getTopViasMaisUtilizadas(10).getFirst();
        assertEquals("ab", diagnostico.viaId());
        assertEquals(3, diagnostico.totalEntradas());
        assertEquals(76.6666666667, diagnostico.ocupacaoMediaPercentual(), 1e-9);
        assertEquals(153.3333333333, diagnostico.ocupacaoMaximaPercentual(), 1e-9);
        assertEquals(50, diagnostico.percentualTempoAcimaCapacidade(), 1e-9);
    }
    @Test void agregaComPesosDeViagensTempoEAmostras() {
        ColetorMetricas a = new ColetorMetricas(), b = new ColetorMetricas();
        a.registrarConclusaoIda(10);
        a.registrarMovimento(10, 3600);
        b.registrarConclusaoIda(20);
        b.registrarConclusaoIda(30);
        b.registrarConclusaoVolta(60);
        b.registrarMovimento(90, 7200);
        ResultadoSimulacao resultado = new ResultadoSimulacao(false);
        resultado.adicionarResultadoDia(a.gerarResultadoDia(0, null));
        resultado.adicionarResultadoDia(b.gerarResultadoDia(1, null));
        assertEquals(30, resultado.calcularTempoMedioGeral());
        assertEquals(20, resultado.calcularTempoMedioIda());
        assertEquals(60, resultado.calcularTempoMedioVolta());
        assertEquals(100.0 / 3, resultado.calcularVelocidadeMediaGeral(), 1e-9);
        assertEquals(0, resultado.calcularCongestionamentoMedioGeral());
        assertThrows(UnsupportedOperationException.class, () -> resultado.getResultadosDias().clear());
    }
    @Test void nenhumaViagemProduzZerosFinitos() {
        ResultadoDia dia = new ColetorMetricas().gerarResultadoDia(0, null);
        assertEquals(0, dia.getTempoMedioViagemMinutos());
        assertEquals(0, dia.getVelocidadeMediaKmh());
        ResultadoSimulacao resultado = new ResultadoSimulacao(false);
        assertEquals(0, resultado.calcularTempoMedioGeral());
        assertEquals(0, resultado.calcularVelocidadeMediaGeral());
    }
}
