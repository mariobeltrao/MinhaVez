package br.com.minhavez.ui.runner;

import br.com.minhavez.model.Cenario;
import br.com.minhavez.service.GeradorCenario;
import br.com.minhavez.ui.ConfiguracaoUi;
import br.com.minhavez.ui.ResultadoExecucao;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SimulacaoVisualRunnerTest {
    @Test void pausaContinuaEParaSemExecutarNaThreadChamadora() throws Exception {
        SimulacaoVisualRunner runner = new SimulacaoVisualRunner();
        runner.definirVelocidade(VelocidadeReproducao.X1);
        Cenario base = new GeradorCenario(4).gerarCenario(7, 1);
        CountDownLatch primeiroSnapshot = new CountDownLatch(1), snapshotDepoisDeContinuar = new CountDownLatch(1);
        AtomicBoolean aguardandoDepois = new AtomicBoolean();
        AtomicInteger quantidade = new AtomicInteger();
        AtomicReference<String> threadSnapshot = new AtomicReference<>();
        AtomicReference<Throwable> erro = new AtomicReference<>();
        try {
            runner.iniciar(base, new ConfiguracaoUi(4, 1, 7), snapshots -> {
                threadSnapshot.set(Thread.currentThread().getName());
                assertEquals(snapshots.semRodizio().diaAtual(), snapshots.comRodizio().diaAtual());
                assertEquals(snapshots.semRodizio().tempoAtualSegundos(), snapshots.comRodizio().tempoAtualSegundos());
                assertFalse(snapshots.semRodizio().rodizioAtivo());
                assertTrue(snapshots.comRodizio().rodizioAtivo());
                quantidade.incrementAndGet();
                primeiroSnapshot.countDown();
                if (aguardandoDepois.get()) snapshotDepoisDeContinuar.countDown();
            }, mensagem -> { }, estado -> { }, resultado -> { }, erro::set);
            assertTrue(primeiroSnapshot.await(2, TimeUnit.SECONDS));
            runner.pausar(estado -> { });
            int durantePausa = quantidade.get();
            Thread.sleep(650);
            assertEquals(durantePausa, quantidade.get());
            assertEquals(EstadoExecucao.PAUSADO, runner.getEstado());
            aguardandoDepois.set(true);
            runner.continuar(estado -> { });
            assertTrue(snapshotDepoisDeContinuar.await(2, TimeUnit.SECONDS));
            runner.parar(estado -> { });
            assertEquals(EstadoExecucao.PARADO, runner.getEstado());
            assertTrue(threadSnapshot.get().startsWith("minhavez-simulacao-"));
            assertNull(erro.get());
        } finally {
            runner.close();
        }
    }

    @Test void rotulosDeVelocidadeSaoEstaveis() {
        assertEquals(VelocidadeReproducao.X16, VelocidadeReproducao.porRotulo("16x"));
        assertEquals(0, VelocidadeReproducao.MAX.getIntervaloMilissegundos());
        assertThrows(IllegalArgumentException.class, () -> VelocidadeReproducao.porRotulo("32x"));
    }

    @Test void concluiOsDoisCenariosConcorrentesComAMesmaConfiguracao() throws Exception {
        SimulacaoVisualRunner runner = new SimulacaoVisualRunner();
        Cenario base = new GeradorCenario(8).gerarCenario(42, 1);
        CountDownLatch concluido = new CountDownLatch(1);
        AtomicReference<ResultadoExecucao> resultado = new AtomicReference<>();
        AtomicReference<Throwable> erro = new AtomicReference<>();
        try {
            runner.iniciar(base, new ConfiguracaoUi(8, 1, 42), snapshot -> { }, mensagem -> { },
                    estado -> { }, recebido -> {
                        resultado.set(recebido);
                        concluido.countDown();
                    }, erro::set);

            assertTrue(concluido.await(5, TimeUnit.SECONDS));
            assertNull(erro.get());
            assertNotNull(resultado.get());
            assertFalse(resultado.get().semRodizio().isRodizioAtivo());
            assertTrue(resultado.get().comRodizio().isRodizioAtivo());
            assertEquals(EstadoExecucao.FINALIZADO, runner.getEstado());
        } finally {
            runner.close();
        }
    }
}
