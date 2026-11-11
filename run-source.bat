@echo off
chcp 65001 >nul
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 goto missing
if not exist build mkdir build
javac -encoding UTF-8 -d build *.java
if errorlevel 1 goto failed
java -Dfile.encoding=UTF-8 -cp build SimplenoteApp
if errorlevel 1 goto failed
exit /b 0
:missing
echo Install a JDK or download the Windows package from GitHub Releases.
:failed
pause
