@echo off
rem compila sem precisar do Maven; as classes vao para out\
setlocal
pushd "%~dp0"
if exist "src\main\java\ordenacao\Main.java" goto :compilar
echo Codigo-fonte nao encontrado. Extraia o ZIP inteiro e execute de novo.
goto :erro

:compilar
call localizar-java.bat || goto :erro
if exist out rmdir /s /q out
javac --release 17 -encoding UTF-8 -d out -sourcepath src\main\java src\main\java\ordenacao\Main.java || goto :erro
echo Compilado em out\
popd
exit /b 0

:erro
pause
popd
exit /b 1
