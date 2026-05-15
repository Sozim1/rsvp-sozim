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

rootProject.name = "Wrist RSVP Reader"

include(":apps:watch")
include(":core:domain")
include(":core:data")
include(":core:reader")
include(":core:parser")
include(":core:designsystem")
include(":core:testing")
