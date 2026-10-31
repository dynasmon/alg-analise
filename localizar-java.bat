@echo off
rem coloca no PATH o primeiro JDK 17 ou mais recente que encontrar
set "PATH_ORIGINAL=%PATH%"
call :testar && exit /b 0
if defined JAVA_HOME call :tentar "%JAVA_HOME%" && exit /b 0
for /d %%j in ("%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Microsoft\*" "%ProgramFiles%\Amazon Corretto\*" "%ProgramFiles%\Zulu\*" "%ProgramFiles%\BellSoft\*" "%LOCALAPPDATA%\Programs\Eclipse Adoptium\*" "%USERPROFILE%\.jdks\*") do (
    call :tentar "%%~j" && exit /b 0
)
echo.
echo Nao encontrei um JDK 17 ou mais recente neste computador.
echo Instale o JDK em https://adoptium.net/temurin/releases/ e execute de novo.
echo.
exit /b 1

:tentar
if not exist "%~1\bin\javac.exe" exit /b 1
set "PATH=%~1\bin;%PATH_ORIGINAL%"

:testar
rem no cmd, um pipe cujo primeiro comando nao existe encerra o script inteiro
where javac >nul 2>nul || exit /b 1
where java >nul 2>nul || exit /b 1
javac -version 2>&1 | findstr /r /c:"javac 1[7-9]" /c:"javac [2-9][0-9]" >nul || exit /b 1
java -version 2>&1 | findstr /r /c:"version .1[7-9]" /c:"version .[2-9][0-9]" >nul
exit /b %errorlevel%
