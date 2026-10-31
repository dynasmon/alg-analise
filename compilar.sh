#!/bin/sh
# compila sem precisar do Maven; as classes vão para out/
set -e
cd "$(dirname "$0")"
rm -rf out
javac --release 17 -encoding UTF-8 -d out $(find src/main/java -name '*.java')
echo "Compilado em out/"
