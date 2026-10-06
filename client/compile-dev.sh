#!/usr/bin/env bash
# Dev compile of the PixLauncher client against the locally produced
# SRG-named 1.8.9 jar (see devlibs/). Output: build/classes
set -e
cd "$(dirname "$0")"
mkdir -p build/classes
CP="devlibs/minecraft-srg-1.8.9.jar;devlibs/launchwrapper-1.12.jar;devlibs/mixin-0.7.11.jar;devlibs/asm-all-5.0.3.jar;devlibs/gson-2.2.4.jar;devlibs/lwjgl-2.9.4-nightly-20150209.jar"
find src/main/java -name '*.java' > build/sources.txt
javac -encoding UTF-8 -proc:none -cp "$CP" -d build/classes @build/sources.txt
echo CLIENT_COMPILE_OK
