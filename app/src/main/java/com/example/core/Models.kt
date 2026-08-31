package com.example.core

enum class LanguageType(val displayName: String, val extension: String, val defaultFileName: String) {
    KOTLIN("Kotlin", ".kt", "MainActivity.kt"),
    JAVA("Java", ".java", "MainActivity.java")
}

enum class FileCategory(val extension: String, val iconLabel: String) {
    KOTLIN(".kt", "KT"),
    JAVA(".java", "J"),
    XML(".xml", "XML"),
    GRADLE(".gradle.kts", "G"),
    GROOVY(".gradle", "G"),
    JSON(".json", "JSON"),
    MANIFEST(".xml", "M"),
    PROPERTIES(".properties", "P"),
    TOML(".toml", "T"),
    OTHER("", "TXT")
}

data class SdkConfiguration(
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val compileSdk: Int = 36,
    val composeVersion: String = "1.7.0",
    val agpVersion: String = "8.9.0",
    val kotlinVersion: String = "2.2.10"
)

enum class DiagnosticType(val label: String, val badgeColorHex: Long) {
    KOTLIN_COMPILATION("Kotlin", 0xFF81C784),
    JAVA_COMPILATION("Java", 0xFFFFB74D),
    AAPT2_RESOURCE("AAPT2 / Res", 0xFF4DD0E1),
    MANIFEST_MERGER("Manifest", 0xFFBA68C8),
    GRADLE_SCRIPT("Gradle DSL", 0xFFFF8A65),
    KSP_PROCESSOR("KSP / Kapt", 0xFF9575CD),
    LINT_WARNING("Lint", 0xFFFFD54F),
    XML_SYNTAX("XML Syntax", 0xFF4DD0E1)
}

data class BuildDiagnostic(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,
    val line: Int,
    val column: Int = 1,
    val message: String,
    val rawLogLine: String = "",
    val isWarning: Boolean = false,
    val errorType: DiagnosticType = DiagnosticType.KOTLIN_COMPILATION,
    val codeSnippet: String? = null,
    val suggestion: String? = null,
    val generatedFileOrigin: String? = null
)

data class ApkMetadata(
    val appName: String,
    val packageName: String,
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val minSdk: Int = 24,
    val targetSdk: Int = 36,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val apkFilePath: String,
    val isSigned: Boolean = true,
    val signatureScheme: String = "APK Signature Scheme v2/v3",
    val certSha256: String = "4C:91:2A:7E:18:9B:44:81:CC:2F:33:DE:55:01:A8",
    val permissions: List<String> = listOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE"
    )
)
