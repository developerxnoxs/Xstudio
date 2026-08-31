package com.example.engine

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectFileEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ProjectZipExporter {

    fun generateZipBytes(project: ProjectEntity, files: List<ProjectFileEntity>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for (file in files) {
                val entryName = "${project.name}/${file.path}"
                val zipEntry = ZipEntry(entryName)
                zos.putNextEntry(zipEntry)
                zos.write(file.content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    fun shareProjectSource(context: Context, project: ProjectEntity, files: List<ProjectFileEntity>) {
        try {
            val zipBytes = generateZipBytes(project, files)
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val zipFile = File(exportDir, "${project.name}-source.zip")
            FileOutputStream(zipFile).use { it.write(zipBytes) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Android Project Source: ${project.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Android Project ZIP"))
        } catch (e: Exception) {
            // Fallback to text sharing
            val textSummary = buildString {
                appendLine("// Project: ${project.name} (${project.packageName})")
                for (f in files) {
                    appendLine("// --- ${f.path} ---")
                    appendLine(f.content)
                    appendLine()
                }
            }
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, textSummary)
                putExtra(Intent.EXTRA_SUBJECT, "${project.name} Source Code")
            }
            context.startActivity(Intent.createChooser(textIntent, "Share Project Code"))
        }
    }
}
