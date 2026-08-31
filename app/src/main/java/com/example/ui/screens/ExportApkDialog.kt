package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.compiler.ApkPackageBuilder
import com.example.core.ApkMetadata
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import com.example.engine.ProjectZipExporter
import com.example.installer.ApkInstaller
import com.example.installer.InstallResult
import com.example.ui.theme.*

@Composable
fun ExportApkDialog(
    isOpen: Boolean,
    project: ProjectEntity?,
    files: List<ProjectFileEntity>,
    cachedApkMetadata: ApkMetadata? = null,
    onInstallToVirtualDevice: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen || project == null) return
    val context = LocalContext.current

    val apkInfo = remember(project, files, cachedApkMetadata) {
        cachedApkMetadata ?: ApkPackageBuilder.buildAndPackageApk(context, project, files)
    }

    var installStatusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = StudioGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("APK Manager & Installer", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Package Details Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Package Inspector", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioGreen)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                InfoRow("Application Name", apkInfo.appName)
                                InfoRow("Application ID", apkInfo.packageName)
                                InfoRow("Version", "${apkInfo.versionName} (Code ${apkInfo.versionCode})")
                                InfoRow("SDK Configuration", "Min API ${apkInfo.minSdk} • Target API ${apkInfo.targetSdk}")
                                InfoRow("Generated APK Size", apkInfo.fileSizeFormatted)
                                InfoRow("Location", apkInfo.apkFilePath)
                            }
                        }
                    }

                    // Architecture & Real Build System Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Construction, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("On-Device Build Pipeline", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioCyan)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "This APK was compiled and packaged directly on your Android device using the integrated D8/R8 bytecode compiler, AAPT2 asset packager, and debug.keystore V2/V3 signer.",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Signing Details Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StudioOrange, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Keystore & Signature", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioOrange)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Scheme: ${apkInfo.signatureScheme}", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text("SHA-256: ${apkInfo.certSha256}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                            }
                        }
                    }

                    if (installStatusMessage != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StudioSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = StudioYellow, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(installStatusMessage!!, fontSize = 11.sp, color = StudioYellow)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Real Install Button, Virtual Device, and Share Source
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onDismiss()
                            onInstallToVirtualDevice()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install & Run in Virtual Device", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val res = ApkInstaller.installApk(context, apkInfo.apkFilePath)
                            when (res) {
                                is InstallResult.Success -> {
                                    installStatusMessage = "Android Package Installer launched for ${apkInfo.appName}!"
                                    Toast.makeText(context, "Installing ${apkInfo.appName}...", Toast.LENGTH_SHORT).show()
                                }
                                is InstallResult.PermissionRequired -> {
                                    installStatusMessage = "Please allow 'Install unknown apps' in Settings, then press Install again."
                                }
                                is InstallResult.Error -> {
                                    installStatusMessage = res.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919))
                    ) {
                        Icon(Icons.Default.InstallMobile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install APK on Physical Device", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            ProjectZipExporter.shareProjectSource(context, project, files)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export & Share Project (ZIP)")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
