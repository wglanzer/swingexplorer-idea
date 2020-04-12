plugins {
    id("org.jetbrains.intellij") version "0.4.18"
    java
}

group = "com.github.wglanzer.swingexplorer"
version = "2020.1-1.6.0"

repositories {
    mavenCentral()
}

dependencies {
    compile(files("libs/swag.jar", "libs/swexpl.jar"))
    testImplementation("junit:junit:4.12")
}

intellij {
    version = "2020.1"
    pluginName = "SwingExplorer Integration v2"
    setPlugins("java")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
