// AGP 9.4 trae Kotlin integrado; no se aplica org.jetbrains.kotlin.android.
buildscript {
    dependencies {
        // La versión de Kotlin coincide con Compose Compiler.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    }
}
plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
}
