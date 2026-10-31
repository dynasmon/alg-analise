# Análise de algoritmos de ordenação: serial × paralelo

Trabalho de Computação Paralela (Unifor). Compara as versões serial e paralela de quatro algoritmos de
ordenação em Java, variando o tamanho e a natureza da entrada e o número de threads.

| Algoritmo | Versão paralela |
|---|---|
| Bubble Sort | transposição par-ímpar com threads dedicadas e barreira de espera ativa |
| Quick Sort | Fork/Join (Hoare + mediana de três) |
| Merge Sort | Fork/Join com intercalação serial |
| Counting Sort | histograma por bloco, soma de prefixos e espalhamento paralelos |

## Requisitos

- Java 17 ou mais recente
- Maven 3.9 (opcional, só para os testes JUnit e para gerar o jar)

## Como compilar e executar

Sem Maven:

```bash
./compilar.sh                 # gera out/
./executar.sh validar         # confere se tudo ordena corretamente
./executar.sh gui             # interface gráfica com gráficos dinâmicos
./executar.sh benchmark       # experimentos completos (20 a 30 min)
./executar.sh benchmark --rapido --saida teste   # versão reduzida (menos de 1 min)
./executar.sh analisar        # refaz resumo, crescimento e gráficos a partir de medicoes.csv
./executar.sh ajuda           # lista todas as opções
```

Com Maven:

```bash
mvn test
mvn package
java -Xms4g -Xmx4g -jar target/alg-analise.jar benchmark
```

Para medições confiáveis, rode o benchmark com o computador na tomada, sem economia de energia e com o
mínimo de programas abertos.

## Opções do benchmark

| Opção | Padrão |
|---|---|
| `--algoritmos` | `bubble,quick,merge,counting` |
| `--distribuicoes` | `aleatorio,ordenado,inverso,quase_ordenado,repetidos` |
| `--tamanhos` | 10k, 30k, 100k, 300k, 1M, 3M e 10M |
| `--tamanhos-bubble` | 1k, 2,5k, 5k, 10k, 20k e 40k |
| `--threads` | 1 até o número de processadores |
| `--amostras` | 5 |
| `--valor-maximo` | 1.000.000 (valores em [0, valor-maximo)) |
| `--intervalo-n`, `--intervalos` | n = 1M e k = 10², 10³, …, 10⁷ (experimento de intervalo, sem o Bubble) |
| `--sem-intervalo` | desliga o experimento de intervalo |
| `--semente` | 20251008 |
| `--saida` | `resultados` |

## Arquivos gerados

Todos ficam na pasta de saída (`resultados/` por padrão):

- `medicoes.csv`: uma linha por medição
  (`experimento,algoritmo,modo,threads,distribuicao,tamanho,valor_maximo,amostra,tempo_ms`)
- `resumo.csv`: uma linha por execução, com média, desvio, CV, mediana, mínimo, máximo, IC 95% (t de Student),
  speedup, speedup relativo (contra 1 thread), eficiência e métrica de Karp-Flatt
- `crescimento.csv`: expoente `a` do ajuste log-log `tempo ~ n^a` e o R² do ajuste
- `ambiente.csv`: sistema operacional, CPU, JVM, heap, threads e semente
- `graficos/*.png`: as figuras usadas no relatório
