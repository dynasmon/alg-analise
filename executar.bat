@echo off
rem uso: executar.bat [comando] [opcoes]; sem comando, abre a interface grafica
setlocal
pushd "%~dp0"
set "CODIGO=1"
if exist "out\ordenacao\Main.class" goto :java
call compilar.bat || goto :sair

:java
call localizar-java.bat || goto :pausa
rem usa 4 GB como nas medicoes do relatorio; sem essa memoria, fica com o padrao da JVM
set "MEMORIA=-Xms4g -Xmx4g"
java %MEMORIA% -version >nul 2>nul || set "MEMORIA="
java %MEMORIA% -cp out ordenacao.Main %*
set "CODIGO=%errorlevel%"
if "%CODIGO%"=="0" goto :sair

:pausa
pause

:sair
popd
exit /b %CODIGO%
