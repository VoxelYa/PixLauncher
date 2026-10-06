plugins {
    java
}

group = "top.pixlauncher"
version = "0.1.0"

/**
 * PixLauncher client (Lunar-style): compiled against deobfuscated 1.8.9 (MCP),
 * applied at launch through our own bootstrap tweaker + Mixin — NOT a Forge
 * mod, never placed in the mods folder. The launcher injects the built jars
 * via the tweak chain (see launcher/src/main/game/launch.ts).
 *
 * Dev setup (one-time): generate a deobf 1.8.9 workspace from MCP mappings
 * (stable_22 for 1.8.9) and publish the mapped jar to mavenLocal as
 * `top.pixlauncher:minecraft-deobf:1.8.9`, then `gradlew build` works offline.
 */
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven")
    maven("https://libraries.minecraft.net")
    mavenLocal()
}

dependencies {
    compileOnly("top.pixlauncher:minecraft-deobf:1.8.9") // mapped 1.8.9, dev-only
    implementation("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    implementation("net.minecraft:launchwrapper:1.12")
    implementation("org.ow2.asm:asm-debug-all:5.2")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "PixLauncher-Client",
            "Implementation-Version" to version,
            "MixinConfigs" to "mixins.pixlauncher.json",
            "TweakClass" to "top.pixlauncher.bootstrap.PixBootstrap",
            "TweakOrder" to "-1000"
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
