package br.com.minhavez.model;

import br.com.minhavez.enums.*;
import br.com.minhavez.service.FabricaCidade;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GrafoTest {
    @Test void todasAsLigacoesTiposEDistanciasCorrespondemAEspecificacao() {
        Cidade cidade = new FabricaCidade().criar();
        verificar(cidade, "N", TipoVia.PRINCIPAL, 3, "5-6 6-7 7-8 2-6 6-9");
        verificar(cidade, "N", TipoVia.SECUNDARIA, 1.5, "1-2 2-3 3-4 1-5 4-8 9-10 3-7");
        verificar(cidade, "N", TipoVia.SECUNDARIA, 3, "5-9 7-10");
        verificar(cidade, "L", TipoVia.PRINCIPAL, 3, "1-2 2-3 3-4 3-7 7-10");
        verificar(cidade, "L", TipoVia.SECUNDARIA, 1.5, "1-5 5-6 6-7 4-8 9-10 2-6 7-8");
        verificar(cidade, "L", TipoVia.SECUNDARIA, 3, "8-9 5-9");
        verificar(cidade, "C", TipoVia.PRINCIPAL, 3, "3-4 4-5 5-6 4-8 8-10");
        verificar(cidade, "C", TipoVia.SECUNDARIA, 1.5, "1-2 2-3 7-8 5-9 9-10 6-10 8-9");
        verificar(cidade, "C", TipoVia.SECUNDARIA, 3, "1-7 2-7");
        verificar(cidade, "S", TipoVia.PRINCIPAL, 3, "1-2 2-3 3-4 2-6 6-9");
        verificar(cidade, "S", TipoVia.SECUNDARIA, 1.5, "1-5 5-6 6-7 7-8 4-8 9-10 3-7");
        verificar(cidade, "S", TipoVia.SECUNDARIA, 3, "7-10 5-9");
        verificar(cidade, "", TipoVia.PRINCIPAL, 4.5, "N4-L1 N5-C1 C6-S1 L9-S4");
        verificar(cidade, "", TipoVia.SECUNDARIA, 3, "N8-L5 N9-C7 C10-S5 L10-S8");
    }
    private void verificar(Cidade cidade, String prefixo, TipoVia tipo, double distancia, String ligacoes) {
        for (String ligacao : ligacoes.split(" ")) {
            String[] pontos = ligacao.split("-");
            Ponto a = cidade.buscarPonto(prefixo + pontos[0]).orElseThrow();
            Ponto b = cidade.buscarPonto(prefixo + pontos[1]).orElseThrow();
            Via via = cidade.buscarVia(a, b).orElseThrow();
            assertEquals(tipo, via.getTipo(), ligacao);
            assertEquals(distancia, via.getDistanciaKm(), ligacao);
            assertSame(via, cidade.buscarVia(b, a).orElseThrow());
        }
    }
    @Test void topologiaTem40Pontos64ViasETodosAlcancaveis() {
        Cidade cidade = new FabricaCidade().criar();
        assertEquals(40, cidade.getPontos().size());
        assertEquals(64, cidade.getVias().size());
        Set<Ponto> visitados = new HashSet<>();
        Deque<Ponto> fila = new ArrayDeque<>();
        fila.add(cidade.buscarPonto("N1").orElseThrow());
        while (!fila.isEmpty()) {
            Ponto ponto = fila.remove();
            if (visitados.add(ponto)) fila.addAll(cidade.getPontosVizinhos(ponto));
        }
        assertEquals(40, visitados.size());
        for (Via via : cidade.getVias()) {
            Set<Regiao> regioes = new HashSet<>(List.of(via.getPontoA().getRegiao(), via.getPontoB().getRegiao()));
            assertNotEquals(Set.of(Regiao.NORTE, Regiao.SUL), regioes);
            assertNotEquals(Set.of(Regiao.CENTRO, Regiao.LESTE), regioes);
            assertTrue(cidade.getViasDe(via.getPontoB()).contains(via));
        }
    }
    @Test void colecoesProtegidasEIdentidadeDosPontos() {
        Cidade cidade = new FabricaCidade().criar();
        Ponto n1 = new Ponto("N1", Regiao.NORTE);
        assertEquals(cidade.buscarPonto("N1").orElseThrow(), n1);
        assertEquals(cidade.buscarPonto("N1").orElseThrow().hashCode(), n1.hashCode());
        assertFalse(cidade.getViasDe(n1).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> cidade.getVias().clear());
        assertThrows(UnsupportedOperationException.class, () -> cidade.getPontos().clear());
        assertThrows(UnsupportedOperationException.class, () -> cidade.getViasDe(n1).clear());
        assertThrows(IllegalArgumentException.class, () -> cidade.adicionarPonto(n1));
    }
}
