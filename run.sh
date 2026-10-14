#!/bin/sh
cd "$(dirname "$0")/java" && mkdir -p out && javac -cp ".:../mysql-connector.jar" -d out *.java && java -cp "out:../mysql-connector.jar" Main
