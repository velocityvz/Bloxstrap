pluginManagement {
    repositories {
        google {
            content {
                // all the android and google stuff
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // only use these repos lul
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// main project name i should stop doing random comments
rootProject.name = "Bloxstrap"
include(":app")
