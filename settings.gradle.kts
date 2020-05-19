pluginManagement {
    repositories {
        if (System.getenv("GITHUB_WORKFLOW").orEmpty().trim().isBlank()) {
            maven("https://maven.aliyun.com/repository/public")
            println("add aliyun repos")
        } else {
            println("detected Github Action, not add aliyun repos.")
        }

        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        println("Hello world=============")
        mavenCentral()

        maven("https://plugins.gradle.org/m2/")
    }
}
rootProject.name = "webf"

