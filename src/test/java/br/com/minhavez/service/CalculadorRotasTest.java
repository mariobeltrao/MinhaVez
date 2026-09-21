package br.com.minhavez.service;

import br.com.minhavez.enums.*;
import br.com.minhavez.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CalculadorRotasTest {
    @Test void escolheMenorTempoEmVezDeMenorDistanciaEReageAOcupacao() {
        Cidade cidade = new Cidade();
        Ponto a = new Ponto("a", Regiao.NORTE), b = new Ponto("b", Regiao.NORTE), c = new Ponto("c", Regiao.NORTE);
        List.of(a, b, c).forEach(cidade::adicionarPonto);
        Via curta = new Via("ab", a, b, TipoVia.SECUNDARIA, 3);
        Via ac = new Via("ac", a, c, TipoVia.PRINCIPAL, 2);
        Via cb = new Via("cb", c, b, TipoVia.PRINCIPAL, 2);
        List.of(curta, ac, cb).forEach(cidade::adicionarVia);
        CalculadorRotas calculador = new CalculadorRotas();
        Rota rota = calculador.calcularMelhorRota(cidade, a, b);
        assertEquals(List.of(ac, cb), rota.getVias());
        assertEquals(4, rota.getDistanciaTotalKm());
        assertEquals(4, rota.getTempoEstimadoMinutos());
        for (int i = 0; i < 40; i++) ac.entrarVeiculo();
        assertEquals(List.of(curta), calculador.calcularMelhorRota(cidade, a, b).getVias());
        assertEquals(4, rota.getTempoEstimadoMinutos());
        assertThrows(UnsupportedOperationException.class, () -> rota.getVias().clear());
    }
    @Test void todasAsRegioesSaoAlcancaveisEmAmbosOsSentidos() {
        Cidade cidade = new FabricaCidade().criar();
        Ponto origem = cidade.buscarPonto("N1").orElseThrow();
        CalculadorRotas calculador = new CalculadorRotas();
        for (Ponto destino : cidade.getPontos()) {
            Rota ida = calculador.calcularMelhorRota(cidade, origem, destino);
            Rota volta = calculador.calcularMelhorRota(cidade, destino, origem);
            assertEquals(ida.getTempoEstimadoMinutos(), volta.getTempoEstimadoMinutos(), 1e-9);
        }
        assertTrue(calculador.calcularMelhorRota(cidade, origem, origem).isVazia());
    }
    @Test void rejeitaDestinoDesconectadoEPontoExterno() {
        Cidade cidade = new Cidade();
        Ponto a = new Ponto("a", Regiao.NORTE), b = new Ponto("b", Regiao.NORTE);
        cidade.adicionarPonto(a);
        cidade.adicionarPonto(b);
        CalculadorRotas calculador = new CalculadorRotas();
        assertThrows(IllegalStateException.class, () -> calculador.calcularMelhorRota(cidade, a, b));
        assertThrows(IllegalArgumentException.class, () -> calculador.calcularMelhorRota(cidade, a, new Ponto("x", Regiao.SUL)));
    }
}
