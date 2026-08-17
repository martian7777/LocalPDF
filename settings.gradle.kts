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

rootProject.name = "LocalPDF"

include(":app")
include(":core:model")
include(":core:designsystem")
include(":core:database")
include(":core:data")
include(":core:pdf")
include(":feature:library")
include(":core:cv-scanner")
include(":feature:scanner")
include(":core:ai-ocr")
include(":core:work")
include(":feature:search")
