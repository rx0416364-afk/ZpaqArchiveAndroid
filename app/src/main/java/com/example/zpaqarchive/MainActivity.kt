Warning: ff was declared with const; use let for reassignable variables.
package com.example.zpaqarchive

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZpaqArchiveApp() }
    }
}

private enum class ScreenMode { COMPRESS, EXTRACT }

private object AppColors {
    val canvas = Color(0xFFF5F6F4)
    val card = Color(0xFFFFFFFF)
    val ink = Color(0xFF18211F)
    val muted = Color(0xFF78827E)
    val accent = Color(0xFF176B5B)
    val accentSoft = Color(0xFFE6F1ED)
    val border = Color(0xFFE5E9E6)
    val warning = Color(0xFF8B5B16)
    val warningSoft = Color(0xFFFFF4DE)
}

private data class OperationResult(val success: Boolean, val message: String, val detail: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZpaqArchiveApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(ScreenMode.COMPRESS) }
    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var folderToCompress by remember { mutableStateOf<Uri?>(null) }
    var archiveUri by remember { mutableStateOf<Uri?>(null) }
    var destinationTree by remember { mutableStateOf<Uri?>(null) }
    var archiveName by remember { mutableStateOf("ArsipBaru.zpaq") }
    var compressionLevel by remember { mutableStateOf(1f) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pilih file untuk mulai membuat arsip.") }
    var lastResult by remember { mutableStateOf<OperationResult?>(null) }

    val filesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        selectedFiles = uris
        uris.forEach { runCatching { context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        if (uris.isNotEmpty()) {
            status = "${uris.size} file dipilih. Tentukan nama arsip, lalu buat arsip."
            lastResult = null
        }
    }

    val compressFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            folderToCompress = uri
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            status = "Folder dipilih. File dan folder dapat dikompres bersama."
            lastResult = null
        }
    }

    val createArchivePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { outputUri ->
        if (outputUri != null && (selectedFiles.isNotEmpty() || folderToCompress != null)) {
            scope.launch {
                busy = true
                lastResult = null
                status = "Menyiapkan file dan mengompres dengan ZPAQ…"
                val result = withContext(Dispatchers.IO) {
                    createArchive(context, selectedFiles, folderToCompress, outputUri, compressionLevel.toInt())
                }
                lastResult = result
                status = result.message
                busy = false
            }
        }
    }

    val archivePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            archiveUri = uri
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            status = "Arsip dipilih. Pilih folder tujuan untuk mengekstrak."
            lastResult = null
        }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            destinationTree = uri
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            status = "Folder tujuan siap. Tekan tombol Ekstrak arsip."
            lastResult = null
        }
    }

    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = AppColors.accent,
            onPrimary = Color.White,
            background = AppColors.canvas,
            surface = AppColors.card,
            onSurface = AppColors.ink,
            onBackground = AppColors.ink
        )
    ) {
        Scaffold(
            containerColor = AppColors.canvas,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AppColors.accentSoft, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = null, tint = AppColors.accent, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Zpaq Archive", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AppColors.ink)
                                Text("Pengelola arsip sederhana", fontSize = 11.sp, color = AppColors.muted)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.canvas)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column {
                        Text("Arsipkan file.\nLebih ringkas.", fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = AppColors.ink)
                        Spacer(Modifier.height(7.dp))
                        Text(
                            "Kompres dan ekstrak file menggunakan format ZPAQ langsung di perangkat.",
                            color = AppColors.muted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE9EDEA), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ModeTab(
                            title = "Buat arsip",
                            icon = Icons.Default.Inventory2,
                            selected = mode == ScreenMode.COMPRESS,
                            modifier = Modifier.weight(1f)
                        ) { mode = ScreenMode.COMPRESS; lastResult = null }
                        ModeTab(
                            title = "Ekstrak",
                            icon = Icons.Default.Unarchive,
                            selected = mode == ScreenMode.EXTRACT,
                            modifier = Modifier.weight(1f)
                        ) { mode = ScreenMode.EXTRACT; lastResult = null }
                    }
                }

                if (mode == ScreenMode.COMPRESS) {
                    item {
                        SectionCard {
                            SectionHeading("File yang akan dikompres", "Pilih satu atau beberapa file dari penyimpanan.")
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { filesPicker.launch(arrayOf("*/*")) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(13.dp),
                                    border = BorderStroke(1.dp, AppColors.border)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(if (selectedFiles.isEmpty()) "Pilih file" else "Ubah file")
                                }
                                OutlinedButton(
                                    onClick = { compressFolderPicker.launch(null) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(13.dp),
                                    border = BorderStroke(1.dp, AppColors.border)
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(if (folderToCompress == null) "Pilih folder" else "Ubah folder")
                                }
                            }
                            if (selectedFiles.isNotEmpty() || folderToCompress != null) {
                                Spacer(Modifier.height(10.dp))
                                if (selectedFiles.isNotEmpty()) {
                                    Text("${selectedFiles.size} file dipilih", color = AppColors.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Spacer(Modifier.height(5.dp))
                                    selectedFiles.take(4).forEach { uri ->
                                        Text("• ${displayName(context.contentResolver, uri)}", color = AppColors.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (selectedFiles.size > 4) Text("dan ${selectedFiles.size - 4} file lainnya", color = AppColors.muted, fontSize = 12.sp)
                                }
                                folderToCompress?.let { folderUri ->
                                    Spacer(Modifier.height(5.dp))
                                    Text("Folder: ${displayName(context.contentResolver, folderUri)}", color = AppColors.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Divider(color = AppColors.border)
                            Spacer(Modifier.height(14.dp))
                            SectionHeading("Nama arsip", "Arsip akan disimpan dengan ekstensi .zpaq.")
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = archiveName,
                                onValueChange = { archiveName = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(13.dp),
                                placeholder = { Text("contoh: Dokumen.zpaq") }
                            )
                            Spacer(Modifier.height(16.dp))
                            SectionHeading("Tingkat kompresi", "Semakin tinggi, umumnya semakin kecil tetapi lebih lambat.")
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${compressionLevel.toInt()}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppColors.accent)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Slider(value = compressionLevel, onValueChange = { compressionLevel = it }, valueRange = 0f..5f, steps = 4)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cepat", fontSize = 10.sp, color = AppColors.muted)
                                        Text("Maksimal", fontSize = 10.sp, color = AppColors.muted)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(levelDescription(compressionLevel.toInt()), color = AppColors.muted, fontSize = 11.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (selectedFiles.isEmpty() && folderToCompress == null) {
                                        status = "Pilih minimal satu file atau satu folder terlebih dahulu."
                                    } else {
                                        val cleanName = archiveName.trim().ifBlank { "ArsipBaru.zpaq" }
                                        val finalName = if (cleanName.lowercase(Locale.ROOT).endsWith(".zpaq")) cleanName else "$cleanName.zpaq"
                                        createArchivePicker.launch(finalName)
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(51.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Buat arsip .zpaq", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(5.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else {
                    item {
                        SectionCard {
                            SectionHeading("Arsip sumber", "Pilih file arsip ZPAQ (.zpaq) yang ingin dibuka.")
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { archivePicker.launch(arrayOf("*/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(13.dp),
                                border = BorderStroke(1.dp, AppColors.border)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (archiveUri == null) "Pilih file .zpaq" else "Ganti arsip")
                            }
                            archiveUri?.let {
                                Spacer(Modifier.height(9.dp))
                                Text(displayName(context.contentResolver, it), color = AppColors.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.height(17.dp))
                            Divider(color = AppColors.border)
                            Spacer(Modifier.height(14.dp))
                            SectionHeading("Folder tujuan", "File hasil ekstrak akan disalin ke folder yang kamu pilih.")
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { folderPicker.launch(null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(13.dp),
                                border = BorderStroke(1.dp, AppColors.border)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (destinationTree == null) "Pilih folder tujuan" else "Ganti folder tujuan")
                            }
                            if (destinationTree != null) {
                                Spacer(Modifier.height(8.dp))
                                Text("Folder tujuan sudah dipilih", color = AppColors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val a = archiveUri
                                    val d = destinationTree
                                    if (a == null || d == null) {
                                        status = "Pilih arsip dan folder tujuan terlebih dahulu."
                                    } else {
                                        scope.launch {
                                            busy = true
                                            lastResult = null
                                            status = "Mengekstrak arsip ZPAQ…"
                                            val result = withContext(Dispatchers.IO) { extractArchive(context, a, d) }
                                            lastResult = result
                                            status = result.message
                                            busy = false
                                        }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(51.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ekstrak arsip", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(5.dp))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                item {
                    StatusCard(busy = busy, status = status, result = lastResult)
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppColors.warningSoft),
                        shape = RoundedCornerShape(15.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AppColors.warning, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(9.dp))
                            Column {
                                Text("Catatan format", fontWeight = FontWeight.SemiBold, color = AppColors.warning, fontSize = 12.sp)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "Aplikasi ini menggunakan ZPAQ 7.15. File .zpaq tidak dibuka langsung oleh pengelola file Android biasa; gunakan aplikasi ini untuk mengekstraknya.",
                                    color = Color(0xFF775A2D), fontSize = 11.sp, lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("ZPAQ engine 7.15", fontSize = 10.sp, color = AppColors.muted)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(3.dp).background(AppColors.muted, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Pemrosesan lokal · Tanpa unggah", fontSize = 10.sp, color = AppColors.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(45.dp).clickable(onClick = onClick),
        color = if (selected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (selected) AppColors.accent else AppColors.muted, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, color = if (selected) AppColors.ink else AppColors.muted, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.card),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AppColors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(17.dp)) { content() }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.ink)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, fontSize = 11.sp, lineHeight = 16.sp, color = AppColors.muted)
    }
}

@Composable
private fun StatusCard(busy: Boolean, status: String, result: OperationResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (result?.success == true) AppColors.accentSoft else Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (result?.success == true) Color(0xFFD7E9E1) else AppColors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(19.dp), strokeWidth = 2.dp, color = AppColors.accent)
            } else if (result?.success == true) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppColors.accent, modifier = Modifier.size(19.dp))
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = AppColors.muted, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (busy) "Sedang diproses" else if (result?.success == true) "Selesai" else if (result != null) "Belum berhasil" else "Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppColors.ink)
                Spacer(Modifier.height(3.dp))
                Text(status, fontSize = 11.sp, lineHeight = 16.sp, color = AppColors.muted)
                if (!result?.detail.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(result!!.detail, fontSize = 10.sp, lineHeight = 14.sp, color = AppColors.muted, maxLines = 5, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun levelDescription(level: Int): String = when (level) {
    0 -> "Level 0 · Prioritas kecepatan, rasio kompresi lebih rendah."
    1 -> "Level 1 · Pengaturan cepat dan seimbang (default ZPAQ)."
    2 -> "Level 2 · Kompresi lebih rapat dengan waktu lebih lama."
    3 -> "Level 3 · Mengutamakan ukuran arsip yang lebih kecil."
    4 -> "Level 4 · Kompresi tinggi, membutuhkan lebih banyak waktu dan memori."
    else -> "Level 5 · Kompresi tertinggi yang disediakan ZPAQ; bisa sangat lambat dan boros memori."
}

private fun displayName(resolver: ContentResolver, uri: Uri): String {
    return runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "File"
}

private fun safeFileName(input: String): String {
    val cleaned = input.substringAfterLast('/').substringAfterLast('\\')
        .replace(Regex("[\\u0000-\\u001F]"), "_")
        .replace("..", "_")
        .trim()
    return cleaned.ifBlank { "file.bin" }
}

private fun uniqueFile(parent: File, requestedName: String): File {
    val safe = safeFileName(requestedName)
    var candidate = File(parent, safe)
    if (!candidate.exists()) return candidate
    val base = safe.substringBeforeLast('.', safe)
    val ext = if (safe.contains('.')) ".${safe.substringAfterLast('.')}" else ""
    var index = 2
    while (candidate.exists()) {
        candidate = File(parent, "$base ($index)$ext")
        index++
    }
    return candidate
}

private fun copyUriToFile(context: Context, uri: Uri, destination: File) {
    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Tidak dapat membaca file yang dipilih.")
    input.use { source -> destination.outputStream().use { target -> source.copyTo(target) } }
}

private fun runZpaq(context: Context, args: List<String>, workingDir: File, log: File): Int {
    return ZpaqNative.runNative(args.toTypedArray(), workingDir.absolutePath, log.absolutePath)
}

private fun logSummary(log: File): String {
    if (!log.exists()) return "Tidak ada detail log dari mesin ZPAQ."
    return runCatching {
        log.readLines().takeLast(8).joinToString("\n").take(700)
    }.getOrDefault("Log tidak dapat dibaca.")
}

private fun copyDocumentFolderToLocal(context: Context, source: DocumentFile, destination: File) {
    source.listFiles().forEach { child ->
        val name = safeFileName(child.name ?: "file.bin")
        if (child.isDirectory) {
            val targetDir = uniqueFile(destination, name)
            if (!targetDir.mkdirs()) throw IOException("Tidak dapat membuat folder $name")
            copyDocumentFolderToLocal(context, child, targetDir)
        } else if (child.isFile) {
            val targetFile = uniqueFile(destination, name)
            copyUriToFile(context, child.uri, targetFile)
        }
    }
}

private fun createArchive(context: Context, uris: List<Uri>, folderUri: Uri?, outputUri: Uri, level: Int): OperationResult {
    val workspace = File(context.cacheDir, "zpaq-work")
    workspace.deleteRecursively()
    val inputDir = File(workspace, "input")
    if (!inputDir.mkdirs()) return OperationResult(false, "Tidak dapat menyiapkan folder kerja sementara.")

    return try {
        val stagedNames = mutableListOf<String>()
        uris.forEachIndexed { index, uri ->
            val requested = displayName(context.contentResolver, uri)
            val target = uniqueFile(inputDir, requested.ifBlank { "file-$index.bin" })
            copyUriToFile(context, uri, target)
            stagedNames += target.name
        }
        folderUri?.let { uri ->
            val sourceFolder = DocumentFile.fromTreeUri(context, uri)
                ?: throw IOException("Folder sumber tidak dapat dibaca.")
            if (!sourceFolder.isDirectory) throw IOException("Sumber yang dipilih bukan folder.")
            val folderRoot = uniqueFile(inputDir, sourceFolder.name ?: "Folder")
            if (!folderRoot.mkdirs()) throw IOException("Tidak dapat menyiapkan folder sumber.")
            copyDocumentFolderToLocal(context, sourceFolder, folderRoot)
            stagedNames += folderRoot.name
        }

        val archive = File(workspace, "created.zpaq")
        val log = File(workspace, "zpaq.log")
        val args = mutableListOf("zpaq", "add", archive.absolutePath)
        args.addAll(stagedNames)
        args += listOf("-method", level.toString(), "-t1")
        val code = runZpaq(context, args, inputDir, log)
        if (code != 0 || !archive.exists() || archive.length() == 0L) {
            OperationResult(false, "Pembuatan arsip gagal (kode $code).", logSummary(log))
        } else {
            val output = context.contentResolver.openOutputStream(outputUri, "w")
                ?: throw IOException("Lokasi penyimpanan tidak dapat ditulis.")
            output.use { target -> archive.inputStream().use { source -> source.copyTo(target) } }
            OperationResult(true, "Arsip berhasil dibuat dan disimpan.", "Ukuran arsip: ${formatBytes(archive.length())}\nLevel kompresi: $level")
        }
    } catch (e: Exception) {
        OperationResult(false, "Gagal membuat arsip: ${e.message ?: "kesalahan tidak diketahui"}")
    } finally {
        workspace.deleteRecursively()
    }
}

private fun extractArchive(context: Context, archiveUri: Uri, treeUri: Uri): OperationResult {
    val workspace = File(context.cacheDir, "zpaq-extract-work")
    workspace.deleteRecursively()
    val outputDir = File(workspace, "output")
    if (!outputDir.mkdirs()) return OperationResult(false, "Tidak dapat menyiapkan folder ekstraksi.")

    return try {
        val archive = File(workspace, "source.zpaq")
        copyUriToFile(context, archiveUri, archive)
        val log = File(workspace, "zpaq.log")
        val args = listOf("zpaq", "extract", archive.absolutePath, "-to", outputDir.absolutePath, "-t1")
        val code = runZpaq(context, args, workspace, log)
        if (code != 0) {
            OperationResult(false, "Ekstraksi gagal (kode $code).", logSummary(log))
        } else {
            val destination = DocumentFile.fromTreeUri(context, treeUri)
                ?: throw IOException("Folder tujuan tidak valid.")
            val copied = copyDirectoryToDocumentTree(context, outputDir, destination)
            OperationResult(true, "Ekstraksi selesai. $copied item disalin ke folder tujuan.", logSummary(log))
        }
    } catch (e: Exception) {
        OperationResult(false, "Gagal mengekstrak arsip: ${e.message ?: "kesalahan tidak diketahui"}")
    } finally {
        workspace.deleteRecursively()
    }
}

private fun copyDirectoryToDocumentTree(context: Context, source: File, destination: DocumentFile): Int {
    var copied = 0
    source.listFiles()?.forEach { child ->
        if (child.isDirectory) {
            val safeName = safeFileName(child.name ?: "Folder")
            val targetDir = destination.findFile(safeName)?.takeIf { it.isDirectory }
                ?: destination.createDirectory(safeName)
                ?: throw IOException("Tidak dapat membuat folder $safeName")
            copied += copyDirectoryToDocumentTree(context, child, targetDir)
        } else if (child.isFile) {
            val name = safeFileName(child.name ?: "file.bin")
            var targetName = name
            var suffix = 2
            while (destination.findFile(targetName) != null) {
                val base = name.substringBeforeLast('.', name)
                val ext = if (name.contains('.')) ".${name.substringAfterLast('.')}" else ""
                targetName = "$base ($suffix)$ext"
                suffix++
            }
            val target = destination.createFile("application/octet-stream", targetName)
                ?: throw IOException("Tidak dapat membuat file $targetName")
            val output = context.contentResolver.openOutputStream(target.uri, "w")
                ?: throw IOException("Tidak dapat menulis file $targetName")
            output.use { sink -> child.inputStream().use { input -> input.copyTo(sink) } }
            copied++
        }
    }
    return copied
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.2f MB", kb / 1024.0)
}
