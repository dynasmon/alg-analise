#!/bin/sh
# uso: ./executar.sh [gui|benchmark|analisar|validar|ajuda] [opções]
set -e
cd "$(dirname "$0")"
[ -d out ] || ./compilar.sh
exec java -Xms4g -Xmx4g -cp out ordenacao.Main "$@"
