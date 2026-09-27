package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.signing.KeystoreDetails
import com.example.signing.SignedApkResult
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeystoreSignerDialog(
    isOpen: Boolean,
    currentProject: ProjectEntity?,
    isSigning: Boolean,
    signedResult: SignedApkResult?,
    onGenerateKeystoreAndSign: (alias: String, password: String, devName: String, org: String, validity: Int) -> Unit,
    onInstallSignedApk: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var alias by remember { mutableStateOf("release_key") }
    var password by remember { mutableStateOf("android123") }
    var devName by remember { mutableStateOf("Android Developer") }
    var organization by remember { mutableStateOf("Mobile Studio") }
    var validityYears by remember { mutableStateOf("25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioSurface,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Keystore & APK App Signer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Tandatangani rilis APK proyek dengan kunci resmi Anda", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Result Card (if signed)
                    if (signedResult != null && signedResult.isSuccess) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StudioGreen.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, StudioGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("APK Berhasil Ditandatangani!", color = StudioGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Nama File: ${signedResult.signedApkName}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Ukuran: ${signedResult.formattedSize}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("Alias Kunci: ${signedResult.signerAlias}", color = Color.LightGray, fontSize = 11.sp)
                                    Text("Checksum SHA-256: ${signedResult.sha256Checksum.take(16)}...", color = StudioCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { onInstallSignedApk(signedResult.signedApkPath) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.InstallMobile, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Install APK Terverifikasi Sekarang", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Form Fields
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                            border = BorderStroke(0.5.dp, StudioBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Konfigurasi Kunci Rilis Baru:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                OutlinedTextField(
                                    value = alias,
                                    onValueChange = { alias = it },
                                    label = { Text("Key Alias") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Keystore & Key Password") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = devName,
                                        onValueChange = { devName = it },
                                        label = { Text("Developer (CN)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = organization,
                                        onValueChange = { organization = it },
                                        label = { Text("Organisasi (O)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                OutlinedTextField(
                                    value = validityYears,
                                    onValueChange = { validityYears = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Masa Berlaku (Tahun)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                            border = BorderStroke(0.5.dp, StudioBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Standar Android Signature Scheme v2 & v3", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "File APK ditandatangani menggunakan algoritma RSA 2048-bit dengan digest SHA-256. Sertifikat disimpan aman di penyimpanan privat aplikasi.",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val years = validityYears.toIntOrNull() ?: 25
                            onGenerateKeystoreAndSign(alias, password, devName, organization, years)
                        },
                        enabled = !isSigning && alias.isNotBlank() && password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen)
                    ) {
                        if (isSigning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Menandatangani...", color = Color.Black)
                        } else {
                            Icon(Icons.Default.Draw, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tandatangani APK Rilis", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
