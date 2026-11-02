pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://alphacephei.com/maven/") } // Vosk repository
    }
}

rootProject.name = "SeventyFivePerc"
include(":app")
