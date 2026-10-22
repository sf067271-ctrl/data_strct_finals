@echo off
REM Windows: double-click this file to compile the program into "bin" and start it.
cd /d "%~dp0"
javac -d bin -sourcepath src src\Main.java
if errorlevel 1 (
    echo Compiling failed. Make sure the Java JDK 8 or newer is installed.
    pause
    exit /b 1
)
java -cp bin Main
pause
