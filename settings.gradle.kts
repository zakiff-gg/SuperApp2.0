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
        // Cadangan: mirror Google untuk Maven Central. Kadang repo.maven.apache.org
        // membalas 403 sesaat karena traffic padat dari IP shared runner CI;
        // dengan mirror ini Gradle otomatis coba sumber lain kalau yang utama gagal.
        maven { url = uri("https://maven-central.storage-download.googleapis.com/maven2/") }
    }
}

rootProject.name = "AbsenSSP"
include(":app")
