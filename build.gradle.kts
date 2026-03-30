import org.apache.commons.lang3.StringUtils

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("incrementVersion") {
    description = "Auto incremental vesion when merge to develop is triggered"
    group = "github actions"
    doLast {
        logger.lifecycle("[incrementVersion] Reading 'gradle.properties'…")
        val file = file("gradle.properties")
        val props = java.util.Properties()
        file.inputStream().use { props.load(it) }

        // versionCode +1
        val currentVersionCode = props.getProperty("VERSION_CODE")?.toInt() ?: 1
        val newVersionCode = currentVersionCode + 1
        props.setProperty("VERSION_CODE", newVersionCode.toString())
        logger.lifecycle("[incrementVersion] updating 'VERSION_CODE' from $currentVersionCode to $newVersionCode")

        // current version in major.minor.path format
        val currentVersionName = props.getProperty("VERSION_NAME", "2.3.6")

        val currentVersionParts = currentVersionName
            .split('.')
            .map { it.toIntOrNull() ?: 0 }
            .toMutableList()

        // some checks:
        // a) In case major is in the old format of year
        if (currentVersionParts[0] >= 2025) currentVersionParts[0] = 2
        // b) List size is not as expected
        while (currentVersionParts.size < 3) {
            currentVersionParts.add(0)
        }

        // what to apply
        val scope = project.findProperty("scope") as String?

        if (scope != null) {
            when (scope) {
                "major" -> {
                    currentVersionParts[0]++
                    currentVersionParts[1] = 0
                    currentVersionParts[2] = 0
                }
                "minor" -> {
                    currentVersionParts[1]++
                    currentVersionParts[2] = 0
                }
                "patch" -> {
                    currentVersionParts[2]++
                }
                else -> error("Invalid scope: $scope")
            }
        } else {
            logger.lifecycle("[incrementVersion] No scope provided → VERSION_NAME unchanged")
        }


        var newVersionName = StringUtils.join(currentVersionParts.toTypedArray(), ".", 0, 3)
        logger.lifecycle("[incrementVersion] updating 'VERSION_NAME' from $currentVersionName to $newVersionName")
        props.setProperty("VERSION_NAME", newVersionName)

        // Save
        file.outputStream().use { props.store(it, null) }
        logger.lifecycle("[incrementVersion] Updated version → versionCode=$newVersionCode, versionName=$newVersionName")
    }
}

