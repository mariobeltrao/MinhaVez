# Cobertura dos requisitos obrigatórios

Os testes usam JUnit 5 e cenários pequenos quando é necessário conferir um resultado físico exato. Testes parametrizados contam cada entrada como uma execução. Os relatórios Maven completos ficam em `target/surefire-reports/`.

Validação desta entrega: **63 testes executados, zero falhas, zero erros e zero testes ignorados**. A suíte cobre população configurável, polos de atração, métricas diagnósticas, snapshots, cenários sincronizados e replay visual, com JDK 21 e Maven 3.9.9.

| Requisito do pedido | Teste responsável |
|---|---|
| 1–5: 40 pontos, 64 vias, alcance e ausência de ligações proibidas | `GrafoTest.topologiaTem40Pontos64ViasETodosAlcancaveis` |
| Topologia, distâncias e tipos exatos | `GrafoTest.todasAsLigacoesTiposEDistanciasCorrespondemAEspecificacao` |
| Capacidades 15/7, ocupações 100%, >100% e 153,33% | `ViaTest.caracteristicasEOcupacaoAcimaDaCapacidade`, `ocupacoesSolicitadasNaCalibracao` |
| Nova curva, 110% a 21 km/h e piso de 5 km/h no colapso | `ViaTest.limitesDasFaixas`, `ocupacoesSolicitadasNaCalibracao`, `pisoDeVelocidadeEProtecaoDeOcupacao` |
| 11: menor tempo, não distância | `CalculadorRotasTest.escolheMenorTempoEmVezDeMenorDistanciaEReageAOcupacao` |
| 800/1000/1200/1500 veículos, grupos e residências equilibrados | `GeradorCenarioTest.quantidadeConfiguravelMantemGruposERegioesEquilibrados` |
| Seed, placas, proporção 20/80, atração regional, polos e novos horários | `GeradorCenarioTest.populacaoEquilibradaReproduzivelSemPlacasRepetidas` (quatro seeds) |
| 18: sete restrições por grupo em 28 dias | `GeradorCenarioTest.cicloCompletoRestringeCadaGrupoSeteVezes` |
| 19: restrito não realiza viagens | `SimulacaoTest.restritoNaoIniciaNenhumaViagem` |
| 20: cenário sem rodízio não restringe | `SimulacaoTest.comparaCenariosSemContaminarBaseERespeitaCiclo` (1, 7 e 28 dias) |
| 21: nova rota na volta, que pode diferir da ida | `SimulacaoTest.voltaPodeEscolherOutroCaminhoComTransitoAlterado` |
| 22–23: sem recálculo em movimento e mesma ocupação nas partidas simultâneas | `SimulacaoTest.rotasSimultaneasUsamMesmaOcupacaoESoSaoCalculadasNaPartida` |
| 24: tempo restante entre vias | `SimulacaoTest.tempoRestanteAtravessaViasComVelocidadesDiferentes` |
| 25–26: não iniciar às 23h, concluir trajetos em andamento | `SimulacaoTest.naoIniciaAs23MasConcluiViagemEmAndamento`, `naoComecaVoltaQuandoIdaSoTerminaDepoisDas23` |
| 27: preservar cenário-base | `GeradorCenarioTest.copiaLimpaNaoCompartilhaObjetosMutaveis`, `SimulacaoTest.comparaCenariosSemContaminarBaseERespeitaCiclo` |
| Snapshot imutável e fiel ao estado do motor | `FabricaSnapshotTest` |
| Observação por tick sem alterar o fluxo da simulação | `SimulacaoObserverTest` |
| Pausar, continuar e encerrar fora da thread JavaFX | `SimulacaoVisualRunnerTest` |
| SEM e COM publicados no mesmo dia e horário | `SimulacaoVisualRunnerTest` |
| Checkpoints, busca anterior e limite do tempo conhecido | `HistoricoSnapshotsTest` |

Também são verificados estados e posições do veículo, proteção das coleções, grafos desconectados, médias ponderadas (viagens, movimento e amostras), categorias regionais, séries imutáveis, base zero e interpretação textual nas variações, seção de configuração e terminal interativo/por argumentos.

Os testes `ordemDosVeiculosNaoAlteraMovimentoOuResultados` e `movimentoCongestionadoTambemIndependeDaOrdemDaPopulacao` invertem a ordem da população e comparam os resultados e as séries temporais. O segundo força sobrecarga e encontros em sentidos opostos. `mesmaInstanciaPodeSerExecutadaNovamenteSemEstadoResidual` verifica uma nova execução sem resíduos.

## Validação manual

```shell
mvn clean test package
mvn javafx:run
java -jar target/minha-vez-0.0.jar 1 12345 --resumo
java -jar target/minha-vez-0.0.jar 28 12345 --resumo
```

Na configuração padrão de 1000 veículos, cada dia sem rodízio deve concluir 2000 viagens e não restringir veículos. Cada dia com rodízio deve concluir 1500 viagens, com 250 restritos. Em 28 dias, isso corresponde a 56000 e 42000 viagens, com 7000 veículos-dia restritos. Tempos, velocidades, ocupações e saturação são calculados pela simulação; não há resultados de desempenho fixados no programa.
