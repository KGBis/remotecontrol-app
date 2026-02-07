import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}


tasks.register("incrementVersion") {
    description = "Auto incremental vesion hook when commit is triggered"
    group = "git hooks"
    doLast {
        logger.lifecycle("[Version Hook] Reading 'gradle.properties'…")
        val file = file("gradle.properties")
        val props = java.util.Properties()
        file.inputStream().use { props.load(it) }

        // --- versionCode +1 ---
        val currentCode = props.getProperty("VERSION_CODE")?.toInt() ?: 1
        val newCode = currentCode + 1
        props.setProperty("VERSION_CODE", newCode.toString())
        logger.lifecycle("[Version Hook] updating 'VERSION_CODE' from $currentCode to $newCode")

        // --- versionName: in yyyy.MM.commit_number ---
        val currentDateStr = DateTimeFormatter.ofPattern("yyyy.MM").format(LocalDate.now())
        val currentName = props.getProperty("VERSION_NAME") ?: "$currentDateStr.1"

        val previousDateStr = currentName.substringBeforeLast(".") // just the date (YYYY.MM)
        val previousCommit = currentName.substringAfterLast(".").toInt() // the revision

        var newName: String
        if(previousDateStr == currentDateStr) { // when in the same month commit + 1
            val currentCommit = previousCommit + 1
            newName = "$currentDateStr.$currentCommit"
        } else {
            newName = "$currentDateStr.1"
        }
        props.setProperty("VERSION_NAME", newName)

        // Guardar
        file.outputStream().use { props.store(it, null) }

        logger.lifecycle("[Version Hook] Updated version → versionCode=$newCode, versionName=$newName")
    }
}
