#!/usr/bin/env bash
# Builds the PixLauncher client pack into the game's pixclient directory.
# The launcher puts every jar in that folder on the game classpath and
# prepends our tweak class, so nothing ever touches the mods folder.
set -e
cd "$(dirname "$0")"
mkdir -p build/pack
cp -r src/main/resources/* build/pack/ 2>/dev/null || true
cp -r build/classes/* build/pack/
cd build/pack
jar cf ../pixlauncher-client.jar .
cd ..
cp devlibs/mixin-0.7.11.jar ../launcher/.minecraft/pixclient/ 2>/dev/null || {
  mkdir -p ../launcher/.minecraft/pixclient
  cp devlibs/mixin-0.7.11.jar ../launcher/.minecraft/pixclient/
}
cp pixlauncher-client.jar ../launcher/.minecraft/pixclient/
echo "PACK_OK"
ls -la ../launcher/.minecraft/pixclient/
