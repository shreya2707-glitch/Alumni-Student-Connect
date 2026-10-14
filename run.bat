@echo off
cd /d "%~dp0java"
if not exist out mkdir out
javac -cp ".;..\mysql-connector.jar" -d out *.java || (echo Compile failed & pause & exit /b 1)
java -cp "out;..\mysql-connector.jar" Main
pause
