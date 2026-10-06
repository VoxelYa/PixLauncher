#!/usr/bin/env bash
# Compiles the Forge headless-install helper into the launcher runtime dir.
set -e
cd "$(dirname "$0")"
mkdir -p ../pixlauncher/runtime/tools-classes
javac -encoding UTF-8 \
  -cp "../pixlauncher/runtime/forge-1.8.9-11.15.1.2318-1.8.9-installer.jar" \
  -d ../pixlauncher/runtime/tools-classes \
  ForgeInstallHelper.java
echo HELPER_COMPILED
