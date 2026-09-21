# Minha Vez

Simulador acadêmico de rodízio de veículos, desenvolvido em **Java 21**, **JavaFX**, **Maven** e **JUnit 5**. A versão 0.0 oferece uma interface gráfica para acompanhar e comparar os cenários, mantendo também a execução pelo terminal.

O experimento compara a mesma população **SEM rodízio** e **COM rodízio**, para investigar o impacto de restringir grupos de veículos sobre o fluxo de uma cidade fictícia. João Pessoa/PB é apenas uma inspiração conceitual e visual: não são usadas ruas, GPS, mapas, dados de frota ou informações reais de trânsito e infraestrutura.

## Executar

Pré-requisitos: JDK 21 e Maven 3.9 ou posterior no `PATH`. Confira `java -version` e `mvn -version` (o Maven também deve apontar para o JDK 21).

```shell
mvn test
mvn package
java -jar target/minha-vez-0.0.jar
```

Para abrir a interface gráfica:

```shell
mvn javafx:run
```

Na tela inicial, escolha a população, o período de 1, 7 ou 28 dias e a seed. A simulação executa primeiro o cenário **SEM rodízio** e depois o cenário **COM rodízio**, sempre com a mesma população-base. O painel permite pausar, continuar ou encerrar a execução e selecionar velocidades de 1×, 2×, 4×, 8×, 16× ou máxima. Ao final, a tela de resultados apresenta métricas comparativas, séries temporais reais, ocupação regional e as vias mais utilizadas.

O mapa da interface representa o grafo fictício de 40 pontos e 64 vias. As cores das vias indicam `LIVRE`, `MODERADO`, `INTENSO`, `CONGESTIONADO`, `SEVERO` e `COLAPSO`; os veículos mostrados são uma amostra visual determinística de até 125 unidades, enquanto todas as unidades continuam sendo processadas pelo motor.

Sem argumentos, o programa oferece um menu para **1, 7 ou 28 dias** e pede a seed. Enter usa 28 dias e seed 12345. Também é possível executar diretamente:

```shell
java -jar target/minha-vez-0.0.jar 1 12345 --resumo
java -jar target/minha-vez-0.0.jar 7 12345
java -jar target/minha-vez-0.0.jar 28 12345
java -jar target/minha-vez-0.0.jar 28 12345 --resumo --veiculos=800
```

Em argumentos, o primeiro número é a quantidade de dias; no menu, é a opção do menu. A seed é um `long`, podendo ser negativa. Argumentos inválidos geram uma mensagem de uso e código de saída 2.

Um dia exibe, por padrão, o relógio a cada dois minutos simulados na mesma linha. Para 7 e 28 dias, são exibidos apenas resumos diários e o relatório agregado. `--detalhado` força o relógio; `--resumo` o desativa e é recomendado para redirecionar a saída. A execução não espera tempo real.

### Ambiente local preparado nesta entrega (Windows)

Como esta máquina não tinha Maven nem JDK 21, foram baixadas cópias locais em `.tools/`, ignoradas pelo Git. Nenhuma instalação global foi alterada. Na pasta do projeto, no PowerShell:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tools\jdk-21.0.12.1+1').Path
$mavenMinhaVez = (Resolve-Path '.tools\apache-maven-3.9.9\bin').Path
$env:PATH = "$env:JAVA_HOME\bin;$mavenMinhaVez;$env:PATH"
mvn '-Dmaven.repo.local=.tools/m2' test
mvn '-Dmaven.repo.local=.tools/m2' package
java -jar target/minha-vez-0.0.jar 1 12345 --resumo
```

As configurações acima valem para a sessão do PowerShell. Em outra máquina, instale JDK 21 e Maven e use os comandos comuns. A primeira execução Maven requer acesso à internet para obter JUnit e os plugins; execuções posteriores podem usar `-o` se todas as dependências estiverem em cache. A saída do programa é UTF-8; em terminais Windows antigos, execute `chcp 65001` antes de iniciar para exibir os acentos.

## Modelo da cidade e rodízio

São **40 pontos** e **64 vias bidirecionais**:

| Região | Pontos | Vias internas principais | Vias internas secundárias |
|---|---|---:|---:|
| NORTE | N1–N10 | 5 | 9 |
| LESTE | L1–L10 | 5 | 9 |
| CENTRO | C1–C10 | 5 | 9 |
| SUL | S1–S10 | 5 | 9 |

As 56 vias internas são complementadas por oito conexões entre regiões. Norte–Leste, Norte–Centro, Centro–Sul e Leste–Sul têm, cada par, uma principal e uma secundária. Não existem ligações diretas Norte–Sul nem Centro–Leste. A lista exata está em [docs/MODELO.md](docs/MODELO.md) e em `FabricaCidade`.

Por padrão, os **1000 veículos** têm residência e destino distintos, placa fictícia única e rotina fixa. A opção `--veiculos=N` permite experimentar outras populações sem editar o código. As residências e os grupos são equilibrados com diferença máxima de um veículo; aproximadamente 20% das viagens são internas e 80% inter-regionais.

Os grupos seguem o ID: 1 → A, 2 → B, 3 → C, 4 → D. Com 1000 veículos, cada grupo possui exatamente 250. A placa não determina o grupo.

Os destinos formam polos fictícios de atração: aproximadamente 50% ficam no CENTRO, 25% no LESTE, 12,5% no NORTE e 12,5% no SUL. No CENTRO, cerca de 65% convergem para C4, C5, C8 e C10; no LESTE, cerca de 60% convergem para L3, L7 e L10. A matriz de origem/destino mantém 20% de viagens internas sem permitir destino igual à residência.

O dia interno 0 restringe A; 1 restringe B; 2 restringe C; 3 restringe D; o ciclo continua. No terminal, o primeiro dia aparece como **Dia 01**. Em 28 dias, cada grupo fica restrito sete vezes. O veículo restrito não inicia ida nem volta.

## Premissas da Simulação

**Capacidades, velocidades, distâncias, congestionamento e população de veículos são parâmetros fictícios definidos para fins acadêmicos.** Não representam medições ou previsões para João Pessoa.

| Tipo de via | Capacidade compartilhada pelos dois sentidos | Velocidade-base |
|---|---:|---:|
| Principal | 15 veículos | 60 km/h |
| Secundária | 7 veículos | 40 km/h |

Capacidade não é limite de entrada: a ocupação pode superar 100%. `ocupação = quantidade / capacidade`.

| Ocupação | Estado | Fator de velocidade |
|---|---|---:|
| 0% a 50% | LIVRE | 1,00 |
| >50% a 80% | MODERADO | 0,80 |
| >80% a 100% | INTENSO | 0,55 |
| >100% a 120% | CONGESTIONADO | 0,35 |
| >120% a 150% | SEVERO | 0,20 |
| >150% | COLAPSO | 0,08 |

`velocidadeAtual = max(5, velocidadeBase × fator)`. Não há veículos externos, bloqueio de entrada, filas em cruzamentos ou separação por faixa/sentido.

As saídas usam quotas embaralhadas pela seed: 3% entre 05h–06h, 7% entre 06h–06h30, 25% entre 06h30–07h, 35% entre 07h–07h30, 22% entre 07h30–08h e 8% entre 08h–09h. Os retornos usam 8% entre 16h–17h, 27% entre 17h–17h30, 35% entre 17h30–18h, 22% entre 18h–18h30 e 8% entre 18h30–19h30. Dentro de cada faixa, o minuto é uniforme. A rotina não muda entre os dias.

## Rotas e movimento

`CalculadorRotas` implementa Dijkstra usando **tempo estimado**, não distância: `distância / velocidadeAtual × 60`. A rota de ida é calculada na partida e a de volta é calculada novamente no retorno; nenhuma é recalculada em movimento. Empates são resolvidos de maneira determinística pelo código dos pontos e pela ordem fixa das adjacências.

O dia começa às 05h e usa ticks de **30 segundos**:

1. Identificar partidas permitidas; o rodízio já foi aplicado na preparação do dia.
2. Calcular todas as rotas com a mesma ocupação inicial, sem inserir veículos durante essa fase.
3. Inserir em conjunto os veículos que partem.
4. Congelar as velocidades após essas entradas, para que novos veículos já contribuam para o congestionamento nesse tick.
5. Movimentar cada veículo com essas velocidades, aproveitando o **tempo** restante ao atravessar outras vias. A distância restante não é transferida cegamente: a próxima via pode ter outra velocidade.
6. Consolidar a ocupação de todas as vias com as posições finais e coletar a amostra do tick.

Essa aproximação mantém o movimento independente da ordem dos veículos. Entradas e saídas ocorridas no meio do tick afetam a velocidade no tick seguinte. A posição entre dois pontos é a distância percorrida na via, e `pontoAtual` é o último ponto alcançado.

As partidas são verificadas nas fronteiras dos ticks. Um horário fora da grade de 30 segundos parte na próxima fronteira. Se a ida terminar depois do retorno programado, a volta começa na primeira fronteira seguinte, desde que anterior às 23h. As rotinas geradas normalmente não encontram esse caso.

Às **23h**, a quantidade de veículos ainda em movimento é registrada, e novas viagens deixam de ser iniciadas. As viagens em andamento continuam até o destino da viagem atual. Um veículo que só conclui a ida após 23h fica no destino; não inicia uma volta nova fora do horário. O dia seguinte começa com todos em casa e as vias vazias.

## Métricas e interpretação


Cada ida e volta é uma viagem independente. O período estacionado no destino é excluído.

| Métrica | Cálculo |
|---|---|
| Tempo médio geral, de ida e de volta | Soma das durações / quantidade das respectivas viagens concluídas |
| Velocidade média | Distância total / soma das horas efetivamente em movimento |
| Ocupação média da cidade | Média das taxas percentuais de todas as vias, depois média das amostras |
| Ocupação por região | Apenas vias com os dois pontos nessa região |
| Ocupação inter-regional | Categoria separada para as oito conexões |
| Pico 17h–19h | Maior ocupação média da cidade nas amostras de 17h inclusive a 19h exclusive |
| Máximo de vias acima da capacidade | Maior contagem amostrada de vias com ocupação estritamente >100% |
| Circulação | Série de veículos simultaneamente nas vias e quantidade de IDs que circularam no dia |
| Restritos e circulação às 23h | Contagens diárias; no agregado, soma em veículos-dia |
| Amostras com via acima de 100% | Percentual de ticks em que ao menos uma via excedeu a capacidade |
| Amostras em SEVERO/COLAPSO | Percentual de ticks em que ao menos uma via esteve exatamente no estado indicado |
| Média de vias acima da capacidade no pico | Soma das vias acima de 100% / amostras entre 17h e 19h |
| Ocupação média das vias ativas | Soma das ocupações das vias com veículos / quantidade de observações dessas vias |
| Número médio de vias ativas | Soma das vias com veículos / quantidade de amostras |
| Percentual das vias ativas congestionadas | Observações de vias acima de 100% / observações de vias ativas |
| Tempo médio acima de 100% por via | Tempo acumulado acima da capacidade, dividido pelas 64 vias |
| Top 10 vias mais utilizadas | Ordenação por entradas reais, com ocupações média/máxima e tempo acima de 100% |

As séries temporais guardam segundos desde 00h (podem exceder 86400 em cenários extremos), contagem em circulação, ocupação da cidade e velocidade calculada pela distância/tempo de movimento **do tick**. As amostras são feitas ao final de cada tick, de 05:00:30 a 23:00, incluindo a extensão necessária para finalizar viagens. A média temporal inclui os intervalos sem trânsito e essa extensão. Há 2160 amostras em um dia sem extensão.

As métricas agregadas ponderam tempos por viagens, velocidade por tempo em movimento e ocupação por quantidade de amostras de duração igual. Não é feita uma média simples de médias diárias com pesos diferentes. `ResultadoDia` preserva cópias imutáveis dos dados; `ResultadoSimulacao` protege sua lista de resultados.

A comparação usa `((COM - SEM) / SEM) × 100`. Se ambos são zero, a variação é 0%. Base zero com valor novo não zero resulta em `Double.NaN` no método numérico e **N/D (base zero)** no relatório.

A calibração aumenta a pressão de forma geral, sem multiplicadores condicionados ao rodízio. Em períodos curtos, o rodízio também muda a composição das viagens observadas; o tempo médio pode subir por terem sido restringidas viagens mais curtas, mesmo sem piora do trânsito. O ciclo de 28 dias equilibra a restrição entre grupos, mas não substitui uma análise causal de viagens equivalentes.

## Arquitetura para estudo

| Pacote | Responsabilidade |
|---|---|
| `enums` | Regiões, tipos de via, grupos e estados |
| `model` | Grafo, rota fixa, rotina e estado individual do veículo, cópia do cenário |
| `service` | Topologia (`FabricaCidade`), geração por seed, Dijkstra, rodízio e relatório |
| `simulation` | Motor em fases (`Simulacao`) e acumulação de métricas (`ColetorMetricas`) |
| `result` | Dados diários e agregações do período |
| `snapshot` | Retrato imutável e desacoplado de cada tick para observadores externos |
| `ui` | Aplicação JavaFX, controladores, mapa e executor assíncrono da simulação |
| `Main` | Entrada, progresso e apresentação pelo terminal |

`FabricaCidade` separa as 64 ligações fixas do sorteio da população. A simulação copia automaticamente o cenário recebido, preservando a base e permitindo executar a mesma instância novamente com o estado diário reiniciado. `Cenario.copiarParaExecucao()` também fica disponível para uso explícito. A interface recebe somente snapshots imutáveis; ela não altera o modelo nem executa o motor na thread visual. O modo terminal continua usando a mesma API anterior.

Sequência de leitura sugerida: `Ponto`/`Via` → `Cidade`/`FabricaCidade` → `Rota`/`CalculadorRotas` → `Veiculo` → `GeradorCenario`/`Cenario`/`SistemaRodizio` → `Simulacao`/`ColetorMetricas` → resultados/relatório → `Main`.

## Testes

```shell
mvn test
```

Os 59 testes cobrem topologia, limites das faixas de ocupação, Dijkstra, reprodutibilidade, cópias independentes, população, ciclo do rodízio, estados, métricas, terminal, snapshots, controle assíncrono e integração para 1/7/28 dias. Há cenários controlados com sobrecarga para validar velocidades, partidas simultâneas, alteração de rota na volta, ausência de recálculo no percurso, movimento entre vias e encerramento às 23h. Veja [docs/TESTES.md](docs/TESTES.md) para o mapeamento dos requisitos.

## Limitações atuais e evoluções possíveis

A versão 0.0 é determinística para uma seed e usa rotinas fixas. Não modela cruzamentos, semáforos, aceleração, veículos especiais, alternativas de transporte ou mudanças de comportamento causadas pelo rodízio. A amostragem a cada 30 segundos pode não capturar picos que ocorram inteiramente dentro de um tick. A média das vias dá o mesmo peso a cada via, independentemente do comprimento e da capacidade, conforme a especificação.

Não há mapas reais, banco de dados, persistência, servidor, API, Spring ou aprendizado de máquina. A interface JavaFX é uma visualização local do grafo fictício; séries temporais e resultados ficam em memória e são descartados quando o programa termina.

Evoluções futuras possíveis incluem exportação de métricas, análise de sensibilidade dos parâmetros e múltiplas seeds, comparação pareada das mesmas viagens e persistência de resultados.
