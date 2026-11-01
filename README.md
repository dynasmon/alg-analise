# Análise de algoritmos de ordenação: serial × paralelo

| Algoritmo | Versão paralela |
|---|---|
| Bubble Sort | transposição par-ímpar com threads dedicadas e barreira de espera ativa |
| Quick Sort | Fork/Join (Hoare + mediana de três) |
| Merge Sort | Fork/Join com intercalação serial |
| Counting Sort | histograma por bloco, soma de prefixos e espalhamento paralelos |

## Requisitos

- JDK 17 ou mais recente
- Maven 3.9 (opcional, só para os testes JUnit e para gerar o jar)

## Como executar

### Windows

1. Instale um [JDK 17 ou mais recente](https://adoptium.net/temurin/releases/), se ainda não tiver.
2. Baixe o projeto em **Code > Download ZIP** e extraia a pasta inteira.
3. Dê dois cliques em `executar.bat`. Na primeira vez ele compila o código; depois abre a interface gráfica com
   os dados de `resultados/`. Se o Windows mostrar um aviso de segurança, clique em **Mais informações** e
   **Executar assim mesmo**.
4. Se quiser usar o terminal, abra o cmd ou o PowerShell na pasta do projeto e rode um dos comandos abaixo:

   ```bat
   .\executar.bat validar
   ```

### Comandos

| Comando | O que faz |
|---|---|
| `gui` | abre a interface gráfica; é o padrão quando nenhum comando é informado |
| `validar` | confere se todos os algoritmos ordenam corretamente |
| `benchmark --saida novos-resultados` | executa os experimentos completos, de 5 a 30 min conforme o número de núcleos |
| `benchmark --rapido --saida teste` | executa uma versão reduzida, em menos de 1 min |
| `analisar --saida novos-resultados` | refaz resumo, crescimento e gráficos a partir de `medicoes.csv` |
| `ajuda` | lista todas as opções |

Na interface, escolha os algoritmos, a natureza dos dados e os parâmetros e clique em **Executar**. O `validar`
deve terminar com a mensagem `Todos os algoritmos ordenaram corretamente.`

Sem `--saida`, o benchmark substitui os arquivos de `resultados/`, que são os dados do relatório. Os tempos
variam conforme a máquina.

No PowerShell, listas de valores precisam de aspas, como em `--threads "1,2,4"`. Se não houver 4 GB de memória
disponíveis, o `executar.bat` usa o padrão da JVM. Depois de alterar o código, rode `compilar.bat` ou
`./compilar.sh`, porque o executar só compila quando `out/` não existe.

Para medições confiáveis, rodar o benchmark com o computador na tomada, sem economia de energia e com o
mínimo de programas abertos.

### Testes e jar com Maven

```bash
mvn test
mvn package
java -Xms4g -Xmx4g -jar target/alg-analise.jar benchmark
```

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
