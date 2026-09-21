package br.com.minhavez.model;

import br.com.minhavez.enums.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ViaTest {
    private Via via(TipoVia tipo) { return new Via("v", new Ponto("a", Regiao.NORTE), new Ponto("b", Regiao.NORTE), tipo, 3); }

    @Test void caracteristicasEOcupacaoAcimaDaCapacidade() {
        Via principal = via(TipoVia.PRINCIPAL);
        assertEquals(15, principal.getCapacidade());
        assertEquals(60, principal.getVelocidadeBaseKmh());
        assertEquals(7, via(TipoVia.SECUNDARIA).getCapacidade());
        assertEquals(40, via(TipoVia.SECUNDARIA).getVelocidadeBaseKmh());
        for (int i = 0; i < 23; i++) principal.entrarVeiculo();
        assertEquals(153.3333333333, principal.calcularOcupacaoPercentual(), 1e-9);
        assertEquals(5, principal.calcularVelocidadeAtualKmh(), 1e-9);
        assertEquals(36, principal.calcularTempoEstimadoMinutos(), 1e-9);
    }
    @ParameterizedTest @CsvSource({"0,LIVRE", "7,LIVRE", "8,MODERADO", "12,MODERADO", "13,INTENSO", "15,INTENSO", "16,CONGESTIONADO", "18,CONGESTIONADO", "19,SEVERO", "22,SEVERO", "23,COLAPSO"})
    void limitesDasFaixas(int quantidade, EstadoTransito estado) {
        Via via = via(TipoVia.PRINCIPAL);
        for (int i = 0; i < quantidade; i++) via.entrarVeiculo();
        assertEquals(estado, via.getEstadoTransito());
        assertEquals(Math.max(5, 60 * estado.getFatorVelocidade()), via.calcularVelocidadeAtualKmh(), 1e-9);
    }
    @Test void ocupacoesSolicitadasNaCalibracao() {
        Via principal = via(TipoVia.PRINCIPAL);
        for (int i = 0; i < 15; i++) principal.entrarVeiculo();
        assertEquals(100, principal.calcularOcupacaoPercentual(), 1e-9);
        principal.entrarVeiculo();
        assertTrue(principal.calcularOcupacaoPercentual() > 100);
        for (int i = 16; i < 23; i++) principal.entrarVeiculo();
        assertEquals(153.3333333333, principal.calcularOcupacaoPercentual(), 1e-9);
        assertEquals(EstadoTransito.COLAPSO, principal.getEstadoTransito());
        assertEquals(0.08, principal.calcularFatorVelocidade(), 1e-9);
        assertEquals(5, principal.calcularVelocidadeAtualKmh(), 1e-9);

        assertEquals(EstadoTransito.CONGESTIONADO, EstadoTransito.paraOcupacao(1.10));
        assertEquals(0.35, EstadoTransito.paraOcupacao(1.10).getFatorVelocidade(), 1e-9);
        assertEquals(21, 60 * EstadoTransito.paraOcupacao(1.10).getFatorVelocidade(), 1e-9);
    }
    @Test void pisoDeVelocidadeEProtecaoDeOcupacao() {
        Via via = via(TipoVia.SECUNDARIA);
        for (int i = 0; i < 100; i++) via.entrarVeiculo();
        assertEquals(5, via.calcularVelocidadeAtualKmh());
        via.resetarOcupacao();
        assertThrows(IllegalStateException.class, via::sairVeiculo);
        assertEquals(0, via.getQuantidadeVeiculos());
        assertThrows(IllegalArgumentException.class, () -> via.getOutroPonto(new Ponto("x", Regiao.SUL)));
    }
}
