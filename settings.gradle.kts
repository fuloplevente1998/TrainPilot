pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TrainPilot"
include(":shared")
include(":app")
include(":wear")
project(":wear").projectDir = file("android/wear")
project(":app").projectDir = file("android/app")
project(":shared").projectDir = file("android/shared")
