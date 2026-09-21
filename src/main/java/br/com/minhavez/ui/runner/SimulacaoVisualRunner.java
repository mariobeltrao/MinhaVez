package br.com.minhavez.ui.runner;

import br.com.minhavez.model.Cenario;
import br.com.minhavez.result.ResultadoSimulacao;
import br.com.minhavez.simulation.Simulacao;
import br.com.minhavez.snapshot.SnapshotSimulacao;
import br.com.minhavez.ui.ConfiguracaoUi;
import br.com.minhavez.ui.ResultadoExecucao;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;
import java.util.function.Consumer;

/** Executa SEM e COM em paralelo e publica somente pares referentes ao mesmo tick. */
public final class SimulacaoVisualRunner implements AutoCloseable {
    private static final int CAPACIDADE_FILA = 4;
    private final AtomicInteger numeroThread = new AtomicInteger();
    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread thread = new Thread(r, "minhavez-simulacao-" + numeroThread.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });
    private final Lock lock = new ReentrantLock();
    private final Condition alteracao = lock.newCondition();
    private volatile VelocidadeReproducao velocidade = VelocidadeReproducao.MAX;
    private volatile EstadoExecucao estado = EstadoExecucao.PRONTO;
    private volatile boolean parar;
    private Future<?> tarefa;

    public void iniciar(Cenario base, ConfiguracaoUi configuracao, Consumer<SnapshotComparacao> snapshots,
                        Consumer<String> mensagens, Consumer<EstadoExecucao> estados,
                        Consumer<ResultadoExecucao> conclusao, Consumer<Throwable> erros) {
        Objects.requireNonNull(base);
        if (tarefa != null && !tarefa.isDone()) throw new IllegalStateException("Já existe uma execução ativa");
        parar = false;
        mudarEstado(EstadoExecucao.EXECUTANDO, estados);
        tarefa = executor.submit(() -> coordenar(base, configuracao, snapshots, mensagens, estados, conclusao, erros));
    }

    private void coordenar(Cenario base, ConfiguracaoUi configuracao, Consumer<SnapshotComparacao> snapshots,
                           Consumer<String> mensagens, Consumer<EstadoExecucao> estados,
                           Consumer<ResultadoExecucao> conclusao, Consumer<Throwable> erros) {
        BlockingQueue<EventoSnapshot> filaSem = new ArrayBlockingQueue<>(CAPACIDADE_FILA);
        BlockingQueue<EventoSnapshot> filaCom = new ArrayBlockingQueue<>(CAPACIDADE_FILA);
        Future<ResultadoSimulacao> sem = executor.submit(() -> executarCenario(base, false, filaSem));
        Future<ResultadoSimulacao> com = executor.submit(() -> executarCenario(base, true, filaCom));
        mensagens.accept("Executando SEM e COM RODÍZIO no mesmo relógio");
        try {
            boolean fimSem = false, fimCom = false;
            EventoSnapshot eventoSem = null, eventoCom = null;
            while (!fimSem || !fimCom) {
                if (!fimSem && eventoSem == null) eventoSem = filaSem.take();
                if (!fimCom && eventoCom == null) eventoCom = filaCom.take();
                if (eventoSem != null && eventoSem.fim()) { fimSem = true; eventoSem = null; }
                if (eventoCom != null && eventoCom.fim()) { fimCom = true; eventoCom = null; }
                if (eventoSem != null && eventoCom != null) {
                    int ordem = compararInstante(eventoSem.snapshot(), eventoCom.snapshot());
                    if (ordem < 0) { eventoSem = null; continue; }
                    if (ordem > 0) { eventoCom = null; continue; }
                    aguardarControle();
                    snapshots.accept(new SnapshotComparacao(eventoSem.snapshot(), eventoCom.snapshot()));
                    eventoSem = null;
                    eventoCom = null;
                } else if (fimSem) {
                    eventoCom = null;
                } else if (fimCom) {
                    eventoSem = null;
                }
            }
            verificarParada();
            ResultadoSimulacao resultadoSem = sem.get();
            ResultadoSimulacao resultadoCom = com.get();
            mudarEstado(EstadoExecucao.FINALIZADO, estados);
            conclusao.accept(new ResultadoExecucao(configuracao, resultadoSem, resultadoCom));
        } catch (CancellationException | InterruptedException ignorada) {
            Thread.currentThread().interrupt();
            sem.cancel(true);
            com.cancel(true);
            mudarEstado(EstadoExecucao.PARADO, estados);
        } catch (ExecutionException erro) {
            mudarEstado(EstadoExecucao.ERRO, estados);
            erros.accept(erro.getCause());
        } catch (Throwable erro) {
            sem.cancel(true);
            com.cancel(true);
            mudarEstado(EstadoExecucao.ERRO, estados);
            erros.accept(erro);
        }
    }

    private int compararInstante(SnapshotSimulacao a, SnapshotSimulacao b) {
        int dia = Integer.compare(a.diaAtual(), b.diaAtual());
        return dia != 0 ? dia : Long.compare(a.tempoAtualSegundos(), b.tempoAtualSegundos());
    }

    private ResultadoSimulacao executarCenario(Cenario base, boolean rodizio,
                                                BlockingQueue<EventoSnapshot> fila) {
        try {
            return new Simulacao(base, rodizio).executar(snapshot -> publicarNaFila(fila, snapshot));
        } finally {
            publicarFim(fila);
        }
    }

    private void publicarNaFila(BlockingQueue<EventoSnapshot> fila, SnapshotSimulacao snapshot) {
        verificarParada();
        try {
            fila.put(EventoSnapshot.dado(snapshot));
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Execução interrompida");
        }
    }

    private void publicarFim(BlockingQueue<EventoSnapshot> fila) {
        while (!parar) {
            try {
                if (fila.offer(EventoSnapshot.FIM, 100, TimeUnit.MILLISECONDS)) return;
            } catch (InterruptedException erro) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void aguardarControle() {
        lock.lock();
        try {
            while (bloqueado() && !parar) alteracao.await();
            verificarParada();
            long nanos = TimeUnit.MILLISECONDS.toNanos(velocidade.getIntervaloMilissegundos());
            while (nanos > 0 && !parar && !bloqueado()) nanos = alteracao.awaitNanos(nanos);
            while (bloqueado() && !parar) alteracao.await();
            verificarParada();
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Execução interrompida");
        } finally {
            lock.unlock();
        }
    }

    private boolean bloqueado() { return estado == EstadoExecucao.PAUSADO || estado == EstadoExecucao.REPLAY; }
    private void verificarParada() { if (parar || Thread.currentThread().isInterrupted()) throw new CancellationException(); }

    public void pausar(Consumer<EstadoExecucao> estados) {
        if (estado == EstadoExecucao.EXECUTANDO) mudarEstadoSinalizando(EstadoExecucao.PAUSADO, estados);
    }

    public void entrarReplay(Consumer<EstadoExecucao> estados) {
        if (estado == EstadoExecucao.EXECUTANDO || estado == EstadoExecucao.PAUSADO) {
            mudarEstadoSinalizando(EstadoExecucao.REPLAY, estados);
        }
    }

    public void voltarAoMaisRecente(Consumer<EstadoExecucao> estados) {
        if (estado == EstadoExecucao.REPLAY) mudarEstadoSinalizando(EstadoExecucao.PAUSADO, estados);
    }

    public void continuar(Consumer<EstadoExecucao> estados) {
        if (estado == EstadoExecucao.PAUSADO || estado == EstadoExecucao.REPLAY) {
            mudarEstadoSinalizando(EstadoExecucao.EXECUTANDO, estados);
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

    private void mudarEstadoSinalizando(EstadoExecucao novo, Consumer<EstadoExecucao> consumidor) {
        mudarEstado(novo, consumidor);
        sinalizar();
    }
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

    private record EventoSnapshot(SnapshotSimulacao snapshot, boolean fim) {
        private static final EventoSnapshot FIM = new EventoSnapshot(null, true);
        private static EventoSnapshot dado(SnapshotSimulacao snapshot) {
            return new EventoSnapshot(snapshot, false);
        }
    }
}
