package com.example.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed class InstallResult {
    object Success : InstallResult()
    object PermissionRequired : InstallResult()
    data class Error(val message: String) : InstallResult()
}

object ApkInstaller {

    fun installApk(context: Context, apkPath: String): InstallResult {
        try {
            val apkFile = File(apkPath)
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return InstallResult.Error("APK file not found or is empty at $apkPath")
            }

            // Check Unknown Sources Permission for Android 8.0 (API 26) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return InstallResult.PermissionRequired
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
            return InstallResult.Success
        } catch (e: Exception) {
            return InstallResult.Error("Failed to launch package installer: ${e.localizedMessage}")
        }
    }
}
