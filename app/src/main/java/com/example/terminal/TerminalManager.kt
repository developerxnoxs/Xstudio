package com.example.terminal

import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity

data class TerminalCommandResult(
    val output: String,
    val isError: Boolean = false,
    val triggerBuild: Boolean = false,
    val triggerInstallToVirtualDevice: Boolean = false
)

object TerminalManager {

    fun executeCommand(
        commandLine: String,
        project: ProjectEntity?,
        files: List<ProjectFileEntity>
    ): TerminalCommandResult {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) return TerminalCommandResult("")

        val parts = trimmed.split(Regex("\\s+"))
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        return when (cmd) {
            "help" -> TerminalCommandResult(
                """Android Studio Mobile Terminal Commands:
  gradle assembleDebug     - Compile and package debug APK
  gradle clean             - Clean build outputs and caches
  gradle tasks             - List available Gradle build tasks
  adb devices              - List connected virtual/physical devices
  adb install [apk]        - Stream & install APK onto virtual device
  adb shell am start       - Launch MainActivity on virtual device
  install                  - Build and install app to virtual device
  run                      - Build, install & open device emulator
  ls [path]                - List project files and directories
  pwd                      - Print working directory
  cat <file>               - Display file content
  find <name>              - Search files matching name
  java -version            - Show Java runtime version
  kotlinc -version         - Show Kotlin compiler version
  sdkmanager --list        - List installed Android SDK platforms
  clear                    - Clear terminal screen"""
            )

            "adb" -> {
                if (args.isEmpty()) {
                    TerminalCommandResult("Android Debug Bridge version 1.0.41\nVersion 35.0.2-12146422\nUse 'adb devices' or 'adb install'.")
                } else when (args[0].lowercase()) {
                    "devices" -> TerminalCommandResult(
                        """List of devices attached
emulator-5554	device (Pixel 9 Pro, Android 15.0 - API 35)"""
                    )
                    "install" -> {
                        val apkName = if (args.size > 1 && !args[1].startsWith("-")) args[1] else "${project?.name ?: "app"}-debug.apk"
                        TerminalCommandResult(
                            """Performing Streamed Install
Success: $apkName installed on emulator-5554 (Pixel 9 Pro - Android 15)
Launching ${project?.packageName ?: "com.example.app"}/.MainActivity...""",
                            triggerInstallToVirtualDevice = true
                        )
                    }
                    "shell" -> {
                        if (args.size > 1 && args[1] == "am" && args.getOrNull(2) == "start") {
                            TerminalCommandResult(
                                """Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] cmp=${project?.packageName ?: "com.example.app"}/.MainActivity }
Status: ok
Activity: ${project?.packageName ?: "com.example.app"}/.MainActivity""",
                                triggerInstallToVirtualDevice = true
                            )
                        } else {
                            TerminalCommandResult("emulator-5554:/ $ ${args.drop(1).joinToString(" ")}")
                        }
                    }
                    "logcat" -> TerminalCommandResult(
                        """--------- beginning of system
08-31 12:30:01.102  1520  1520 I ActivityManager: Start proc ${project?.packageName ?: "com.example.app"} for activity .MainActivity
08-31 12:30:01.189  1520  1520 D JetpackCompose: Initialized Compose ViewHierarchy
08-31 12:30:01.215  1520  1520 I ${project?.name ?: "App"}: MainActivity.onCreate() completed"""
                    )
                    else -> TerminalCommandResult("adb: unknown command '${args.joinToString(" ")}'. Try 'adb devices' or 'adb install'.")
                }
            }

            "install", "run" -> TerminalCommandResult(
                """Connecting to emulator-5554 (Pixel 9 Pro)...
Performing Streamed Install of ${project?.name ?: "app"}-debug.apk
Success (Installed in 320ms)
Launching MainActivity...""",
                triggerInstallToVirtualDevice = true
            )

            "pwd" -> TerminalCommandResult("/data/user/0/com.aistudio.ide/projects/${project?.name ?: "workspace"}")

            "ls" -> {
                val list = files.map { it.path }.sorted()
                TerminalCommandResult(list.joinToString("\n"))
            }

            "cat" -> {
                if (args.isEmpty()) {
                    TerminalCommandResult("Usage: cat <file_path>", isError = true)
                } else {
                    val targetName = args[0]
                    val found = files.find { it.path.endsWith(targetName) || it.name == targetName }
                    if (found != null) {
                        TerminalCommandResult(found.content)
                    } else {
                        TerminalCommandResult("cat: $targetName: No such file in project", isError = true)
                    }
                }
            }

            "find" -> {
                if (args.isEmpty()) {
                    TerminalCommandResult("Usage: find <pattern>", isError = true)
                } else {
                    val query = args[0]
                    val matched = files.filter { it.path.contains(query, ignoreCase = true) }.map { it.path }
                    TerminalCommandResult(if (matched.isEmpty()) "No matches found." else matched.joinToString("\n"))
                }
            }

            "java" -> {
                if (args.contains("-version") || args.contains("--version")) {
                    TerminalCommandResult(
                        """openjdk version "17.0.12" 2024-07-16
OpenJDK Runtime Environment (build 17.0.12+7-Ubuntu-1ubuntu2)
OpenJDK 64-Bit Server VM (build 17.0.12+7-Ubuntu-1ubuntu2, mixed mode, sharing)"""
                    )
                } else {
                    TerminalCommandResult("java: option required e.g. java -version")
                }
            }

            "kotlinc" -> {
                TerminalCommandResult("info: kotlinc-jvm 2.2.10 (JRE 17.0.12)")
            }

            "sdkmanager" -> {
                TerminalCommandResult(
                    """Installed platforms:
  platforms;android-36          | 1 | Android SDK Platform 36 (Android 16 Baklava)
  platforms;android-35          | 1 | Android SDK Platform 35 (Android 15 VanillaIceCream)
  platforms;android-34          | 1 | Android SDK Platform 34 (Android 14 UpsideDownCake)
  build-tools;36.0.0            | 1 | Android SDK Build-Tools 36.0.0
  platform-tools                | 35.0.2 | Android SDK Platform-Tools"""
                )
            }

            "gradle" -> {
                if (args.contains("assembleDebug") || args.contains("build")) {
                    TerminalCommandResult("Starting Gradle build...\n> Task :app:preBuild\n> Task :app:compileDebugKotlin", triggerBuild = true)
                } else if (args.contains("clean")) {
                    TerminalCommandResult("BUILD SUCCESSFUL in 45ms\nCleaned build/ and .gradle/ cache.")
                } else if (args.contains("tasks")) {
                    TerminalCommandResult(
                        """Android tasks
-------------
androidDependencies - Displays the Android dependencies of the project.
assemble - Assemble main outputs for all the variants.
assembleDebug - Assembles main output for variant Debug.
assembleRelease - Assembles main output for variant Release.
build - Assembles and tests this project.
check - Runs all checks.
connectedCheck - Runs all device checks on currently connected devices."""
                    )
                } else {
                    TerminalCommandResult("gradle: task '${args.joinToString(" ")}' not recognized. Try 'gradle assembleDebug'.")
                }
            }

            "clear" -> TerminalCommandResult("__CLEAR__")

            else -> TerminalCommandResult("sh: $cmd: command not found. Type 'help' for available commands.", isError = true)
        }
    }
}
