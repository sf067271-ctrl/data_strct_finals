#!/bin/sh
# macOS / Linux: compiles the program into "bin" and starts it.
# Run it from a terminal with:  sh run.sh
set -e
cd "$(dirname "$0")"
javac -d bin -sourcepath src src/Main.java
java -cp bin Main
