plugins {
    id("org.jetbrains.intellij") version "0.4.18"
    java
}

group = "com.github.wglanzer.swingexplorer"
version = "2020.1-1.7.0"

repositories {
    mavenCentral()
}

dependencies {
    compile(files("libs/swingexplorer-agent-1.7.0.jar", "libs/swingexplorer-core-1.7.0.jar"))
    testImplementation("junit:junit:4.12")
}

intellij {
    version = "2020.1"
    pluginName = "SwingExplorer Integration v2"
    updateSinceUntilBuild = false
    setPlugins("java")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
