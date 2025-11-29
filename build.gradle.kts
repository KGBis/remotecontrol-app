// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("incrementVersion") {
    doLast {
        val file = file("gradle.properties")
        val props = java.util.Properties()
        file.inputStream().use { props.load(it) }

        // --- versionCode +1 ---
        val currentCode = props.getProperty("VERSION_CODE")?.toInt() ?: 1
        val newCode = currentCode + 1
        props.setProperty("VERSION_CODE", newCode.toString())

        // --- versionName: incrementa solo el patch ---
        val currentName = props.getProperty("VERSION_NAME") ?: "1.0.0"
        val parts = currentName.split(".").map { it.toInt() }.toMutableList()
        parts[2] += 1
        val newName = parts.joinToString(".")
        props.setProperty("VERSION_NAME", newName)

        // Guardar
        file.outputStream().use { props.store(it, null) }

        println("Updated version → versionCode=$newCode, versionName=$newName")
    }
}
