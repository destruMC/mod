plugins {
    alias(libs.plugins.fabric.loom)
}

repositories {
    maven { url = uri("https://maven.shedaniel.me/") }
    maven { url = uri("https://maven.terraformersmc.com/") }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    implementation(project(":destru-api"))
    modApi(libs.cloth.fabric) {
        exclude(group = "net.fabricmc.fabric-api")
    }
    modImplementation(libs.modmenu)
    include(project(":destru-api"))
}

loom {
    mixin {
        defaultRefmapName = "destru-refmap.json"
    }
    accessWidenerPath.set(file("src/main/resources/destru.accesswidener"))
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.version,
                "minecraft_version" to libs.versions.minecraft.get(),
                "loader_version" to libs.versions.fabric.loader.get(),
            )
        )
    }
}