package br.com.minhavez.model;

import br.com.minhavez.enums.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VeiculoTest {
    @Test void transicoesPosicaoEResetPreservamRotina() {
        Ponto a = new Ponto("a", Regiao.NORTE), b = new Ponto("b", Regiao.NORTE);
        Via via = new Via("ab", a, b, TipoVia.PRINCIPAL, 3);
        Veiculo v = new Veiculo(1, "AAA-0001", GrupoRodizio.A, a, b, LocalTime.of(6, 0), LocalTime.of(17, 0));
        v.iniciarIda(new Rota(a, b, List.of(via)), 21600);
        assertTrue(v.estaEmMovimento());
        assertThrows(IllegalStateException.class, v::concluirIda);
        assertThrows(IllegalArgumentException.class, () -> v.avancar(-1));
        v.avancar(3);
        v.concluirViaAtual();
        v.concluirIda();
        assertEquals(b, v.getPontoAtual());
        v.iniciarVolta(new Rota(b, a, List.of(via)), 61200);
        v.avancar(3);
        v.concluirViaAtual();
        v.concluirVolta();
        assertEquals(EstadoVeiculo.FINALIZADO, v.getEstado());
        assertEquals(a, v.getPontoAtual());
        assertEquals(6, v.getDistanciaTotalPercorridaKm());
        v.resetarParaNovoDia();
        assertEquals(0, v.getDistanciaTotalPercorridaKm());
        assertNull(v.getRotaAtual());
        assertEquals(LocalTime.of(6, 0), v.getHorarioSaida());
        v.marcarRestrito();
        assertThrows(IllegalStateException.class, () -> v.iniciarIda(new Rota(a, b, List.of(via)), 21600));
    }
}
