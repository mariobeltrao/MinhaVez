package br.com.minhavez.ui.runner;

import br.com.minhavez.model.Cenario;
import br.com.minhavez.result.ResultadoSimulacao;
import br.com.minhavez.simulation.Simulacao;
import br.com.minhavez.snapshot.SnapshotSimulacao;
import br.com.minhavez.ui.*;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.function.Consumer;

/** Executa o motor fora da JavaFX Application Thread e controla somente o ritmo de apresentação. */
public final class SimulacaoVisualRunner implements AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "minhavez-simulacao");
        thread.setDaemon(true);
        return thread;
    });
    private final Lock lock = new ReentrantLock();
    private final Condition alteracao = lock.newCondition();
    private volatile VelocidadeReproducao velocidade = VelocidadeReproducao.MAX;
    private volatile EstadoExecucao estado = EstadoExecucao.PRONTO;
    private volatile boolean parar;
    private Future<?> tarefa;
    private long ultimaEmissaoNanos;

    public void iniciar(Cenario base, ConfiguracaoUi configuracao, Consumer<SnapshotSimulacao> snapshots,
                        Consumer<String> mensagens, Consumer<EstadoExecucao> estados,
                        Consumer<ResultadoExecucao> conclusao, Consumer<Throwable> erros) {
        Objects.requireNonNull(base);
        if (tarefa != null && !tarefa.isDone()) throw new IllegalStateException("Já existe uma execução ativa");
        parar = false;
        mudarEstado(EstadoExecucao.EXECUTANDO, estados);
        tarefa = executor.submit(() -> {
            try {
                mensagens.accept("Executando cenário SEM RODÍZIO");
                ResultadoSimulacao sem = new Simulacao(base, false).executar(s -> apresentar(s, snapshots));
                mensagens.accept("Cenário SEM RODÍZIO concluído");
                aguardarTransicao();
                verificarParada();
                mensagens.accept("Executando cenário COM RODÍZIO");
                ResultadoSimulacao com = new Simulacao(base, true).executar(s -> apresentar(s, snapshots));
                verificarParada();
                mudarEstado(EstadoExecucao.CONCLUIDO, estados);
                conclusao.accept(new ResultadoExecucao(configuracao, sem, com));
            } catch (CancellationException ignorada) {
                mudarEstado(EstadoExecucao.PARADO, estados);
            } catch (Throwable erro) {
                mudarEstado(EstadoExecucao.ERRO, estados);
                erros.accept(erro);
            }
        });
    }

    private void apresentar(SnapshotSimulacao snapshot, Consumer<SnapshotSimulacao> destino) {
        aguardarControle();
        if (velocidade == VelocidadeReproducao.MAX) {
            long agora = System.nanoTime();
            if (agora - ultimaEmissaoNanos < 50_000_000L) return;
            ultimaEmissaoNanos = agora;
        }
        destino.accept(snapshot);
    }

    private void aguardarControle() {
        lock.lock();
        try {
            while (estado == EstadoExecucao.PAUSADO && !parar) alteracao.await();
            verificarParada();
            long nanos = TimeUnit.MILLISECONDS.toNanos(velocidade.getIntervaloMilissegundos());
            while (nanos > 0 && !parar && estado != EstadoExecucao.PAUSADO) nanos = alteracao.awaitNanos(nanos);
            while (estado == EstadoExecucao.PAUSADO && !parar) alteracao.await();
            verificarParada();
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Execução interrompida");
        } finally {
            lock.unlock();
        }
    }

    private void aguardarTransicao() {
        lock.lock();
        try { alteracao.await(700, TimeUnit.MILLISECONDS); }
        catch (InterruptedException erro) { Thread.currentThread().interrupt(); throw new CancellationException(); }
        finally { lock.unlock(); }
    }

    private void verificarParada() { if (parar || Thread.currentThread().isInterrupted()) throw new CancellationException(); }

    public void pausar(Consumer<EstadoExecucao> estados) {
        if (estado == EstadoExecucao.EXECUTANDO) {
            mudarEstado(EstadoExecucao.PAUSADO, estados);
            sinalizar();
        }
    }
    public void continuar(Consumer<EstadoExecucao> estados) {
        if (estado == EstadoExecucao.PAUSADO) {
            mudarEstado(EstadoExecucao.EXECUTANDO, estados);
            sinalizar();
        }
    }
    public void parar(Consumer<EstadoExecucao> estados) {
        parar = true;
        sinalizar();
        if (tarefa != null) tarefa.cancel(true);
        mudarEstado(EstadoExecucao.PARADO, estados);
    }
    public void definirVelocidade(VelocidadeReproducao velocidade) {
        this.velocidade = Objects.requireNonNull(velocidade);
        sinalizar();
    }
    public EstadoExecucao getEstado() { return estado; }
    public VelocidadeReproducao getVelocidade() { return velocidade; }

    private void mudarEstado(EstadoExecucao novo, Consumer<EstadoExecucao> consumidor) {
        estado = novo;
        consumidor.accept(novo);
    }
    private void sinalizar() {
        lock.lock();
        try { alteracao.signalAll(); }
        finally { lock.unlock(); }
    }
    @Override public void close() {
        parar(estado -> { });
        executor.shutdownNow();
    }
}
