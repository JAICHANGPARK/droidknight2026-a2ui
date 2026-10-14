pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    val snapshotBuildId = providers.gradleProperty("androidx.a2ui.snapshotBuildId")
        .orElse("latest")
        .get()
        .trim()
    val a2uiSnapshotRepoUrl = if (snapshotBuildId.isEmpty() || snapshotBuildId == "latest") {
        "https://androidx.dev/snapshots/latest/artifacts/repository"
    } else {
        "https://androidx.dev/snapshots/builds/$snapshotBuildId/artifacts/repository"
    }
    repositories {
        // Official AndroidX CI Snapshot repository for tracking the latest `androidx-main` A2UI commits
        maven {
            name = "AndroidXSnapshot"
            url = uri(a2uiSnapshotRepoUrl)
            content {
                includeGroupByRegex("androidx\\.a2ui.*")
                includeModule("androidx.compose.material3", "material3-a2ui")
            }
        }
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "DroidKnights2026-A2UI-Samples"
include(":app")
