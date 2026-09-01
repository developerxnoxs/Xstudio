package com.example.terminal

import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity

data class TerminalFileAction(
    val action: String, // "CREATE", "UPDATE", "DELETE"
    val path: String,
    val fileName: String,
    val fileType: String,
    val content: String = "",
    val isDirectory: Boolean = false
)

data class TerminalCommandResult(
    val output: String,
    val isError: Boolean = false,
    val triggerBuild: Boolean = false,
    val triggerInstallToVirtualDevice: Boolean = false,
    val fileAction: TerminalFileAction? = null
)

object TerminalManager {

    fun executeCommand(
        commandLine: String,
        project: ProjectEntity?,
        files: List<ProjectFileEntity>
    ): TerminalCommandResult {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) return TerminalCommandResult("")

        // Check for echo with redirect: echo "content" > filepath
        if (trimmed.startsWith("echo ") && trimmed.contains(">")) {
            val parts = trimmed.substringAfter("echo ").split(">", limit = 2)
            val rawContent = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
            val targetPath = parts[1].trim()
            val fileName = targetPath.substringAfterLast('/')
            val fileType = when {
                fileName.endsWith(".kt") -> "KOTLIN"
                fileName.endsWith(".xml") -> "XML"
                fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts") -> "GRADLE"
                fileName.endsWith(".json") -> "JSON"
                else -> "TEXT"
            }
            return TerminalCommandResult(
                output = "Wrote ${rawContent.length} bytes to $targetPath",
                fileAction = TerminalFileAction(
                    action = "UPDATE",
                    path = targetPath,
                    fileName = fileName,
                    fileType = fileType,
                    content = rawContent
                )
            )
        }

        val parts = trimmed.split(Regex("\\s+"))
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        return when (cmd) {
            "help" -> TerminalCommandResult(
                """Android Studio Mobile Terminal:
  --- Build & ADB ---
  gradle assembleDebug     - Compile and package debug APK
  gradle clean             - Clean build outputs and caches
  gradle tasks             - List available Gradle build tasks
  gradle test              - Run local JVM unit tests
  adb devices              - List connected virtual/physical devices
  adb install [apk]        - Stream & install APK onto virtual device
  adb shell am start       - Launch MainActivity on virtual device
  adb logcat               - View system and app runtime logs
  install / run            - Build, install & boot Pixel 9 Pro emulator

  --- File Operations ---
  ls [path]                - List project files and directories
  pwd                      - Print working directory
  cat <file>               - Display file content
  find <pattern>           - Search files matching name
  grep <pattern> [file]    - Search text in file or all project files
  touch <path>             - Create new file
  mkdir <path>             - Create new directory
  rm <path>                - Delete file or directory
  wc -l <file>             - Count lines in file
  head -n <N> <file>       - Show first N lines
  tail -n <N> <file>       - Show last N lines

  --- Git VCS & Environment ---
  git status               - Check working tree status
  git branch               - List branches
  git log                  - View recent commit history
  java -version            - Show Java runtime version
  kotlinc -version         - Show Kotlin compiler version
  sdkmanager --list        - List installed Android SDK platforms
  clear                    - Clear terminal screen"""
            )

            "touch" -> {
                if (args.isEmpty()) {
                    TerminalCommandResult("Usage: touch <path/to/File.kt>", isError = true)
                } else {
                    val path = args[0]
                    val name = path.substringAfterLast('/')
                    val ext = name.substringAfterLast('.', "")
                    val fileType = when (ext.lowercase()) {
                        "kt" -> "KOTLIN"
                        "xml" -> "XML"
                        "gradle", "kts" -> "GRADLE"
                        "json" -> "JSON"
                        else -> "TEXT"
                    }
                    val defaultContent = when (fileType) {
                        "KOTLIN" -> "package ${project?.packageName ?: "com.example"}\n\n// New Kotlin file\n"
                        "XML" -> "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n</resources>\n"
                        else -> ""
                    }
                    TerminalCommandResult(
                        output = "Created file $path",
                        fileAction = TerminalFileAction(
                            action = "CREATE",
                            path = path,
                            fileName = name,
                            fileType = fileType,
                            content = defaultContent
                        )
                    )
                }
            }

            "mkdir" -> {
                if (args.isEmpty()) {
                    TerminalCommandResult("Usage: mkdir <path/to/directory>", isError = true)
                } else {
                    val path = args[0]
                    val name = path.substringAfterLast('/')
                    TerminalCommandResult(
                        output = "Created directory $path",
                        fileAction = TerminalFileAction(
                            action = "CREATE",
                            path = path,
                            fileName = name,
                            fileType = "DIRECTORY",
                            content = "",
                            isDirectory = true
                        )
                    )
                }
            }

            "rm" -> {
                if (args.isEmpty()) {
                    TerminalCommandResult("Usage: rm <path/to/file>", isError = true)
                } else {
                    val path = args[0]
                    val found = files.find { it.path == path || it.name == path || it.path.endsWith("/$path") }
                    if (found != null) {
                        TerminalCommandResult(
                            output = "Removed ${found.path}",
                            fileAction = TerminalFileAction(
                                action = "DELETE",
                                path = found.path,
                                fileName = found.name,
                                fileType = found.fileType
                            )
                        )
                    } else {
                        TerminalCommandResult("rm: cannot remove '$path': No such file or directory", isError = true)
                    }
                }
            }

            "grep" -> {
                if (args.isEmpty()) {
                    TerminalCommandResult("Usage: grep <pattern> [file_path]", isError = true)
                } else {
                    val pattern = args[0]
                    val targetFile = args.getOrNull(1)
                    if (targetFile != null) {
                        val file = files.find { it.path.endsWith(targetFile) || it.name == targetFile }
                        if (file != null) {
                            val matches = file.content.lines().mapIndexedNotNull { idx, line ->
                                if (line.contains(pattern, ignoreCase = true)) "${file.name}:${idx + 1}: $line" else null
                            }
                            TerminalCommandResult(if (matches.isEmpty()) "Pattern not found in ${file.name}" else matches.joinToString("\n"))
                        } else {
                            TerminalCommandResult("grep: $targetFile: No such file in project", isError = true)
                        }
                    } else {
                        val allMatches = mutableListOf<String>()
                        for (file in files) {
                            if (!file.isDirectory) {
                                file.content.lines().forEachIndexed { idx, line ->
                                    if (line.contains(pattern, ignoreCase = true)) {
                                        allMatches.add("${file.name}:${idx + 1}: $line")
                                    }
                                }
                            }
                        }
                        TerminalCommandResult(if (allMatches.isEmpty()) "No matches found across project files." else allMatches.take(50).joinToString("\n"))
                    }
                }
            }

            "wc" -> {
                if (args.isEmpty()) {
                    TerminalCommandResult("Usage: wc -l <file>", isError = true)
                } else {
                    val target = if (args[0] == "-l" && args.size > 1) args[1] else args[0]
                    val file = files.find { it.path.endsWith(target) || it.name == target }
                    if (file != null) {
                        val lines = file.content.lines().size
                        val words = file.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                        val bytes = file.content.toByteArray().size
                        TerminalCommandResult("$lines lines  $words words  $bytes bytes  ${file.name}")
                    } else {
                        TerminalCommandResult("wc: $target: No such file", isError = true)
                    }
                }
            }

            "head" -> {
                val n = if (args.size >= 2 && args[0] == "-n") args[1].toIntOrNull() ?: 10 else 10
                val target = if (args.size >= 3 && args[0] == "-n") args[2] else args.lastOrNull()
                if (target == null) {
                    TerminalCommandResult("Usage: head -n <lines> <file>", isError = true)
                } else {
                    val file = files.find { it.path.endsWith(target) || it.name == target }
                    if (file != null) {
                        val headLines = file.content.lines().take(n).joinToString("\n")
                        TerminalCommandResult(headLines)
                    } else {
                        TerminalCommandResult("head: $target: No such file", isError = true)
                    }
                }
            }

            "tail" -> {
                val n = if (args.size >= 2 && args[0] == "-n") args[1].toIntOrNull() ?: 10 else 10
                val target = if (args.size >= 3 && args[0] == "-n") args[2] else args.lastOrNull()
                if (target == null) {
                    TerminalCommandResult("Usage: tail -n <lines> <file>", isError = true)
                } else {
                    val file = files.find { it.path.endsWith(target) || it.name == target }
                    if (file != null) {
                        val tailLines = file.content.lines().takeLast(n).joinToString("\n")
                        TerminalCommandResult(tailLines)
                    } else {
                        TerminalCommandResult("tail: $target: No such file", isError = true)
                    }
                }
            }

            "git" -> {
                if (args.isEmpty()) {
                    TerminalCommandResult("git version 2.43.0\nCommands: status, branch, log, diff")
                } else when (args[0].lowercase()) {
                    "status" -> TerminalCommandResult(
                        """On branch main
Your branch is up to date with 'origin/main'.

Changes to be committed:
  (use "git restore --staged <file>..." to unstage)
	modified:   ${files.firstOrNull { it.fileType == "KOTLIN" }?.path ?: "app/src/main/java/MainActivity.kt"}

Total project files: ${files.size} (Indexed in SQLite Room DB)"""
                    )
                    "branch" -> TerminalCommandResult("* main\n  feature/visual-designer\n  release/v1.0")
                    "log" -> TerminalCommandResult(
                        """commit a9f83c1d (HEAD -> main)
Author: Mobile Developer <developer@aistudio.google.com>
Date:   ${java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", java.util.Locale.US).format(java.util.Date())}

    feat: Integrated Real Gemini AI Assistant and Visual Layout Designer

commit 3b8e72c4
Author: AI Studio Initializer <bot@aistudio.google.com>
Date:   Wed Aug 27 10:00:00 2025

    chore: Initial Android Studio Mobile Project Template"""
                    )
                    "diff" -> TerminalCommandResult("Use Git Visual Diff tab for side-by-side graphical diff view.")
                    else -> TerminalCommandResult("git: '${args.joinToString(" ")}' is not a git command. See 'help'.")
                }
            }

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
                } else if (args.contains("test")) {
                    TerminalCommandResult(
                        """> Task :app:testDebugUnitTest
Test com.example.ExampleUnitTest PASSED (12ms)
BUILD SUCCESSFUL (1 test passed)"""
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
