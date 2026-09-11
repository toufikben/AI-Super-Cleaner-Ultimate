package com.aisupercleaner.ultimate

import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.data.AppDatabase
import com.aisupercleaner.ultimate.data.RecommendationEngine
import com.aisupercleaner.ultimate.data.DuplicateEngine
import com.aisupercleaner.ultimate.data.MediaAnalysisReport
import com.aisupercleaner.ultimate.data.CleanupManager
import com.aisupercleaner.ultimate.data.SmartCleanupReport
import com.aisupercleaner.ultimate.data.StorageScanner
import com.aisupercleaner.ultimate.data.ScanProgress
import com.aisupercleaner.ultimate.data.VideoPreset
import com.aisupercleaner.ultimate.data.CompressionManager
import com.aisupercleaner.ultimate.data.CompressionResult
import com.aisupercleaner.ultimate.data.ImagePreset
import com.aisupercleaner.ultimate.ads.AdManager
import com.aisupercleaner.ultimate.billing.BillingManager
import com.aisupercleaner.ultimate.privacy.AppPreferences
import com.aisupercleaner.ultimate.privacy.StoragePermissionManager
import com.aisupercleaner.ultimate.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { AISuperCleanerTheme { CleanerApp() } } }
}

data class NavItem(val label: String, val icon: ImageVector)
data class Finding(val title: String, val detail: String, val size: String, val icon: ImageVector, val tint: Color)

@Composable
fun CleanerApp() {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }
    val database = remember { AppDatabase.get(context) }
    val scanner = remember { StorageScanner(context.contentResolver, database.storageDao()) }
    val recommendationEngine = remember { RecommendationEngine(database.storageDao()) }
    val duplicateEngine = remember { DuplicateEngine(context.contentResolver, database.storageDao()) }
    val cleanupManager = remember { CleanupManager(context.contentResolver, database.storageDao()) }
    val compressionManager = remember { CompressionManager(context, context.contentResolver, database.storageDao()) }
    val adManager = remember { AdManager(context) }
    LaunchedEffect(Unit) { adManager.initialize() }
    val billingManager = remember { BillingManager(context) }
    val isPremium by billingManager.isPremium.collectAsStateWithLifecycle()
    val billingMessage by billingManager.message.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val fileCount by database.storageDao().observeFileCount().collectAsStateWithLifecycle(initialValue = 0)
    val totalBytes by database.storageDao().observeTotalBytes().collectAsStateWithLifecycle(initialValue = 0L)
    val imageCount by database.storageDao().observeCountByType("image").collectAsStateWithLifecycle(initialValue = 0)
    val videoCount by database.storageDao().observeCountByType("video").collectAsStateWithLifecycle(initialValue = 0)
    val audioCount by database.storageDao().observeCountByType("audio").collectAsStateWithLifecycle(initialValue = 0)
    val statFs = remember { StatFs(Environment.getDataDirectory().path) }
    val freeBytes = remember { statFs.availableBytes }
    val totalStorageBytes = remember { statFs.totalBytes }
    var selected by remember { mutableIntStateOf(0) }
    var showPermissionEducation by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var scanProgress by remember { mutableStateOf<ScanProgress?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<SmartCleanupReport?>(null) }
    var mediaReport by remember { mutableStateOf<MediaAnalysisReport?>(null) }
    LaunchedEffect(fileCount, totalBytes, isScanning) {
        if (!isScanning && fileCount > 0) {
            report = recommendationEngine.analyze(totalStorageBytes - freeBytes, freeBytes)
        }
    }
    val startScan: () -> Unit = {
        if (!isScanning) scope.launch {
            isScanning = true
            try { scanner.scan { progress -> scanProgress = progress }; mediaReport = duplicateEngine.analyze(); permissionMessage = "Scan complete. Results are saved locally for review." }
            catch (_: SecurityException) { permissionMessage = "Media access was denied. Nothing was scanned." }
            catch (_: Exception) { permissionMessage = "The scan could not finish. No destructive action was taken." }
            finally { isScanning = false; scanProgress = null }
        }
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) startScan() else permissionMessage = "Access was not granted. Nothing was scanned."
    }
    val navItems = listOf(NavItem("Home", Icons.Default.Home), NavItem("Clean", Icons.Default.AutoAwesome), NavItem("Analyze", Icons.Default.PieChart), NavItem("Tools", Icons.Default.Build), NavItem("Settings", Icons.Default.Settings))
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) { navItems.forEachIndexed { index, item -> NavigationBarItem(selected = selected == index, onClick = { selected = index }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 11.sp) }) } } }) { padding ->
        when (selected) {
            0 -> HomeScreen(Modifier.padding(padding), fileCount, totalBytes, imageCount, videoCount, audioCount, freeBytes, totalStorageBytes, report, mediaReport, scanProgress, isScanning, onSmartScan = { showPermissionEducation = true }, onQuickClean = { selected = 1 })
            1 -> CleanScreen(Modifier.padding(padding), database, cleanupManager, adManager, onAdvancedScan = { showPermissionEducation = true })
            2 -> AnalyzeScreen(Modifier.padding(padding), database)
            3 -> ToolsScreen(Modifier.padding(padding), database, cleanupManager, compressionManager)
            4 -> Column(Modifier.padding(padding)) { PremiumPaywall(billingManager, isPremium); PrivacyCenterScreen(Modifier.weight(1f), preferences) }
            else -> PlaceholderScreen(navItems[selected].label, Modifier.padding(padding))
        }
    }
    if (showPermissionEducation) PermissionEducationDialog(onDismiss = { showPermissionEducation = false }, onContinue = { showPermissionEducation = false; val missing = StoragePermissionManager.missingPermissions(context); if (missing.isEmpty()) startScan() else permissionLauncher.launch(missing) })
    permissionMessage?.let { message -> AlertDialog(onDismissRequest = { permissionMessage = null }, confirmButton = { TextButton(onClick = { permissionMessage = null }) { Text("OK") } }, title = { Text("Scan status") }, text = { Text(message) }) }
    billingMessage?.let { message -> AlertDialog(onDismissRequest = { billingManager.clearMessage() }, confirmButton = { TextButton(onClick = { billingManager.clearMessage() }) { Text("OK") } }, title = { Text("Premium status") }, text = { Text(message) }) }
}

@Composable
fun HomeScreen(modifier: Modifier, fileCount: Int, totalBytes: Long, imageCount: Int, videoCount: Int, audioCount: Int, freeBytes: Long, totalStorageBytes: Long, report: SmartCleanupReport?, mediaReport: MediaAnalysisReport?, progress: ScanProgress?, isScanning: Boolean, onSmartScan: () -> Unit, onQuickClean: () -> Unit) {
    val findings = listOf(
        Finding("Photos", "$imageCount items · From MediaStore", "—", Icons.Default.PhotoLibrary, SoftMint),
        Finding("Videos", "$videoCount items · From MediaStore", "—", Icons.Default.VideoLibrary, SoftBlue),
        Finding("Audio", "$audioCount items · From MediaStore", "—", Icons.Default.Audiotrack, SoftLavender)
    )
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Header() }
        item { ScoreCard(fileCount, totalBytes, freeBytes, totalStorageBytes, report) }
        item { PrimaryActions(onSmartScan, onQuickClean, isScanning) }
        if (progress != null) item { ScanProgressCard(progress) }
        if (report != null) {
            item { ExplainableSummary(report) }
            items(report.recommendations) { RecommendationCard(it) }
        }
        if (mediaReport != null) item { MediaAnalysisSummary(mediaReport) }
        item { SectionTitle("On-device inventory", "Live counts from Android MediaStore") }
        items(findings) { FindingCard(it) }
        item { TrustNote() }
    }
}

@Composable private fun Header() { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Good afternoon", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary); Text("Your storage, simplified.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }; IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "Notifications") } } }

@Composable private fun ScoreCard(fileCount: Int, totalBytes: Long, freeBytes: Long, totalStorageBytes: Long, report: SmartCleanupReport?) {
    val used = (totalStorageBytes - freeBytes).coerceAtLeast(0L)
    val ratio = if (totalStorageBytes > 0) (used.toFloat() / totalStorageBytes).coerceIn(0f, 1f) else 0f
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(if (report == null) "Device storage" else "Smart Cleanup Score", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelLarge); Text(if (report == null) "Not analyzed" else "${report.healthScore} / 100", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text("$fileCount indexed media files", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall) }; Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.VerifiedUser, null, tint = Color.White, modifier = Modifier.size(34.dp)) } }; LinearProgressIndicator(progress = { if (report == null) ratio else report.healthScore / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = ScoreAccent, trackColor = Color.White.copy(alpha = .18f)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Used ${formatBytes(used)}", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall); Text("Free ${formatBytes(freeBytes)}", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall) } } }
}

@Composable private fun PrimaryActions(onSmartScan: () -> Unit, onQuickClean: () -> Unit, isScanning: Boolean) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onSmartScan, enabled = !isScanning, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { if (isScanning) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("SCANNING…") } else { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(10.dp)); Text("SMART SCAN", fontWeight = FontWeight.Bold) } }; Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onQuickClean, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.CleaningServices, null); Spacer(Modifier.width(6.dp)); Text("Quick clean") }; OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.PieChart, null); Spacer(Modifier.width(6.dp)); Text("Analyze") } } } }

@Composable private fun ScanProgressCard(progress: ScanProgress) { Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text(progress.stage, fontWeight = FontWeight.SemiBold) }; Text("${progress.scannedFiles} files indexed · ${formatBytes(progress.discoveredBytes)}", style = MaterialTheme.typography.bodySmall); if (progress.cacheHits > 0) Text("${progress.cacheHits} unchanged files reused from cache", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) } } }
@Composable private fun ExplainableSummary(report: SmartCleanupReport) { Card(colors = CardDefaults.cardColors(containerColor = SoftMint), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = ScoreAccent); Spacer(Modifier.width(8.dp)); Text("Explainable analysis", fontWeight = FontWeight.SemiBold) }; Text(if (report.potentialReviewBytes > 0) "Potentially recoverable for review: ${formatBytes(report.potentialReviewBytes)}" else "No high-confidence review category found", fontWeight = FontWeight.Bold); Text(report.explanation, style = MaterialTheme.typography.bodySmall) } } }
@Composable private fun RecommendationCard(recommendation: com.aisupercleaner.ultimate.data.Recommendation) { Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(recommendation.category, fontWeight = FontWeight.SemiBold); Text(formatBytes(recommendation.estimatedBytes), fontWeight = FontWeight.Bold) }; Text("${recommendation.fileCount} files · ${recommendation.confidencePercent}% confidence", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary); Text(recommendation.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun MediaAnalysisSummary(report: MediaAnalysisReport) { Card(colors = CardDefaults.cardColors(containerColor = SoftLavender), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PhotoFilter, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Text("Media analysis", fontWeight = FontWeight.SemiBold) }; Text("${report.duplicateGroups.size} exact duplicate groups · ${report.similarGroups.size} similar groups", fontWeight = FontWeight.Bold); Text("${report.blurredFiles.size} potentially blurry photos · ${report.screenshotFiles.size} screenshots", style = MaterialTheme.typography.bodySmall); Text("Groups are shown for review. Nothing is selected or deleted automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun AnalyzeScreen(modifier: Modifier, database: AppDatabase) {
    val largeFiles by database.storageDao().observeLargeFiles(500L * 1024L * 1024L).collectAsStateWithLifecycle(initialValue = emptyList())
    val downloads by database.storageDao().observeDownloads().collectAsStateWithLifecycle(initialValue = emptyList())
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
        item { Text("Storage analyzer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Large files and Downloads from Android MediaStore.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text("Large files · 500 MB+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (largeFiles.isEmpty()) item { EmptyState("No large files indexed", "Run Smart Scan to refresh the inventory.") }
        items(largeFiles.take(50)) { item -> AnalysisFileRow(item, "Large ${item.mediaType}") }
        item { Text("Downloads and APKs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (downloads.isEmpty()) item { EmptyState("No Downloads matched", "Only locations exposed by Android MediaStore are shown.") }
        items(downloads.take(50)) { item -> AnalysisFileRow(item, item.relativePath ?: "Download location unavailable") }
    }
}
@Composable private fun AnalysisFileRow(item: com.aisupercleaner.ultimate.data.FileMetadataEntity, label: String) { Card(shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (item.mediaType == "video") Icons.Default.VideoFile else if (item.mediaType == "audio") Icons.Default.AudioFile else Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(item.displayName, fontWeight = FontWeight.SemiBold); Text("$label · ${formatBytes(item.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (item.durationMillis > 0) Text("Duration ${item.durationMillis / 1000}s", style = MaterialTheme.typography.labelSmall) } } } }
@Composable private fun ToolsScreen(modifier: Modifier, database: AppDatabase, cleanupManager: CleanupManager, compressionManager: CompressionManager) {
    val compressionHistory by database.storageDao().observeCompressionHistory().collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedVideo by remember { mutableStateOf<android.net.Uri?>(null) }; var selectedImage by remember { mutableStateOf<android.net.Uri?>(null) }; var isCompressing by remember { mutableStateOf(false) }; var resultMessage by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> selectedVideo = uri }
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> selectedImage = uri }
    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp)) {
            item { Text("Tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Compress a copy without overwriting the original.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Video Compressor", fontWeight = FontWeight.SemiBold); Text(selectedVideo?.toString() ?: "No video selected", style = MaterialTheme.typography.bodySmall); Button(onClick = { picker.launch("video/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.VideoFile, null); Spacer(Modifier.width(8.dp)); Text("Choose video") }; Button(onClick = { selectedVideo?.let { uri -> scope.launch { isCompressing = true; try { val result = compressionManager.compressVideoCopy(uri, VideoPreset.BALANCED, 0L); resultMessage = when (result) { is CompressionResult.Success -> "Compressed copy exported to MediaStore: ${result.outputUri}"; is CompressionResult.Failure -> result.message } } finally { isCompressing = false } } } }, enabled = selectedVideo != null && !isCompressing, modifier = Modifier.fillMaxWidth()) { if (isCompressing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Compress and export copy") } } } }
            item { Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Image Compressor", fontWeight = FontWeight.SemiBold); Text(selectedImage?.toString() ?: "No image selected", style = MaterialTheme.typography.bodySmall); Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Choose image") }; Button(onClick = { selectedImage?.let { uri -> scope.launch { isCompressing = true; try { val result = compressionManager.compressImageCopy(uri, ImagePreset.BALANCED, 0L); resultMessage = when (result) { is CompressionResult.Success -> "Compressed image exported to MediaStore: ${result.outputUri}"; is CompressionResult.Failure -> result.message } } finally { isCompressing = false } } } }, enabled = selectedImage != null && !isCompressing, modifier = Modifier.fillMaxWidth()) { if (isCompressing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Compress and export copy") } } } }
            item { Text("Trash", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { Text("Compression history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (compressionHistory.isEmpty()) item { EmptyState("No compression history", "Completed and failed attempts will be recorded locally.") }
            items(compressionHistory) { item -> Card(shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (item.mediaType == "video") Icons.Default.VideoFile else Icons.Default.Image, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("${item.mediaType.replaceFirstChar { it.uppercase() }} · ${item.preset}", fontWeight = FontWeight.SemiBold); Text("${formatBytes(item.originalBytes)} → ${formatBytes(item.outputBytes)} · ${item.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        }
        TrashScreen(Modifier.weight(1f), database, cleanupManager)
    }
    resultMessage?.let { AlertDialog(onDismissRequest = { resultMessage = null }, confirmButton = { TextButton(onClick = { resultMessage = null }) { Text("OK") } }, title = { Text("Compression status") }, text = { Text(it) }) }
}
@Composable private fun CleanScreen(modifier: Modifier, database: AppDatabase, cleanupManager: CleanupManager, adManager: AdManager, onAdvancedScan: () -> Unit) {
    val candidates by database.storageDao().observeDuplicateCandidates().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope(); var selected by remember { mutableStateOf(setOf<String>()) }; var confirm by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    val selectedItems = candidates.filter { it.uri in selected }; val activity = LocalActivity.current
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
        item { Text("Clean safely", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Quick Clean shows only exact-hash candidates. Advanced Smart Scan adds broader analysis for review.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedButton(onClick = onAdvancedScan, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Run Advanced Smart Scan") } }
        item { TextButton(onClick = { activity?.let { adManager.showRewardedAd(it, onReward = onAdvancedScan, onUnavailable = { message = "Rewarded Ad is unavailable. Advanced Smart Scan remains available without an ad." }) } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayCircle, null); Spacer(Modifier.width(8.dp)); Text("Watch a short ad to unlock one Advanced Scan (optional)") } }
        item { Text("${selectedItems.size} selected · ${formatBytes(selectedItems.sumOf { it.sizeBytes })}", fontWeight = FontWeight.SemiBold) }
        if (candidates.isEmpty()) item { EmptyState("No analyzed duplicate candidates yet", "Run Smart Scan first. Nothing is selected by default.") }
        items(candidates) { item ->
            val checked = item.uri in selected
            Card(shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + item.uri else selected - item.uri }); Column(Modifier.weight(1f)) { Text(item.displayName, fontWeight = FontWeight.SemiBold); Text("${formatBytes(item.sizeBytes)} · duplicate hash available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        }
        item { Button(onClick = { confirm = true }, enabled = selectedItems.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Move selected to Trash") } }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Move to Trash?") }, text = { Text("You selected ${selectedItems.size} files (${formatBytes(selectedItems.sumOf { it.sizeBytes })}). Android will retain recoverable items where supported. Nothing will be permanently deleted now.") }, confirmButton = { Button(onClick = { confirm = false; scope.launch { when (val result = cleanupManager.moveToTrash(selectedItems)) { is com.aisupercleaner.ultimate.data.CleanupResult.Success -> { message = "${result.itemsMoved} items moved to Trash."; selected = emptySet() }; is com.aisupercleaner.ultimate.data.CleanupResult.Failure -> message = result.message } } }) { Text("Move to Trash") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }, title = { Text("Clean status") }, text = { Text(it) }) }
}

@Composable private fun TrashScreen(modifier: Modifier, database: AppDatabase, cleanupManager: CleanupManager) {
    val itemsInTrash by database.storageDao().observeTrash().collectAsStateWithLifecycle(initialValue = emptyList()); val scope = rememberCoroutineScope(); var permanentTarget by remember { mutableStateOf<com.aisupercleaner.ultimate.data.TrashItemEntity?>(null) }; var message by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
        item { Text("Trash", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Restore items or permanently delete them after review.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (itemsInTrash.isEmpty()) item { EmptyState("Trash is empty", "Items moved through the safe workflow will appear here.") }
        items(itemsInTrash) { item -> Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(item.displayName, fontWeight = FontWeight.SemiBold); Text(formatBytes(item.sizeBytes), style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = { scope.launch { when (val result = cleanupManager.restore(item)) { is com.aisupercleaner.ultimate.data.CleanupResult.Success -> message = "Item restored."; is com.aisupercleaner.ultimate.data.CleanupResult.Failure -> message = result.message } } }) { Text("Restore") } }; OutlinedButton(onClick = { permanentTarget = item }, modifier = Modifier.fillMaxWidth()) { Text("Delete permanently") } } } }
    }
    permanentTarget?.let { target -> AlertDialog(onDismissRequest = { permanentTarget = null }, title = { Text("Delete permanently?") }, text = { Text("This action cannot be undone. Delete ${target.displayName} permanently?") }, confirmButton = { Button(onClick = { permanentTarget = null; scope.launch { when (val result = cleanupManager.deletePermanently(target)) { is com.aisupercleaner.ultimate.data.CleanupResult.Success -> message = "Item deleted permanently."; is com.aisupercleaner.ultimate.data.CleanupResult.Failure -> message = result.message } } }) { Text("Delete permanently") } }, dismissButton = { TextButton(onClick = { permanentTarget = null }) { Text("Cancel") } }) }
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }, title = { Text("Trash status") }, text = { Text(it) }) }
}

@Composable private fun EmptyState(title: String, body: String) { Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.SemiBold); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun SectionTitle(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun FindingCard(finding: Finding) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(finding.tint), contentAlignment = Alignment.Center) { Icon(finding.icon, null, tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(finding.title, fontWeight = FontWeight.SemiBold); Text(finding.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(finding.size, fontWeight = FontWeight.Bold) } } }
@Composable private fun TrustNote() { Card(colors = CardDefaults.cardColors(containerColor = SoftMint), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Lock, null, tint = ScoreAccent); Spacer(Modifier.width(12.dp)); Column { Text("Private by design", fontWeight = FontWeight.SemiBold); Text("Media metadata is indexed locally. Review every recommendation before cleaning.", style = MaterialTheme.typography.bodySmall) } } } }
@Composable private fun PermissionEducationDialog(onDismiss: () -> Unit, onContinue: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onContinue) { Text("Continue") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }, icon = { Icon(Icons.Default.FolderOpen, null) }, title = { Text("Choose what can be scanned") }, text = { Text("Smart Scan reads only media categories needed for an on-device inventory. Nothing is uploaded, inaccessible files are not scanned, and no broad storage permission is used.") }) }
@Composable private fun PremiumPaywall(billing: BillingManager, isPremium: Boolean) { val activity = LocalActivity.current; val catalog by billing.catalog.collectAsStateWithLifecycle(); Card(Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 8.dp), colors = CardDefaults.cardColors(containerColor = SoftLavender), shape = RoundedCornerShape(20.dp)) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(if (isPremium) "Premium active" else "Unlock Premium", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("${if (isPremium) "No ads and all tools are available." else "Remove ads and unlock the advanced toolkit."}", style = MaterialTheme.typography.bodySmall); Text("Advanced AI Scan · Similar Photos · Video compression · Scheduled Scan · Storage history", style = MaterialTheme.typography.bodySmall); if (!isPremium) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { activity?.let { billing.launchMonthly(it) } }, enabled = catalog.monthly != null, modifier = Modifier.weight(1f)) { Text(catalog.monthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "Monthly") }; OutlinedButton(onClick = { activity?.let { billing.launchLifetime(it) } }, enabled = catalog.lifetime != null, modifier = Modifier.weight(1f)) { Text(catalog.lifetime?.oneTimePurchaseOfferDetails?.formattedPrice ?: "Lifetime") } }; Text("Free cleaning remains available. Purchase is never required to delete a file.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
@Composable private fun PrivacyCenterScreen(modifier: Modifier, preferences: AppPreferences) { var analyticsEnabled by remember { mutableStateOf(preferences.analyticsEnabled) }; LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 24.dp)) { item { Text("Privacy Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Clear answers about access and data.", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { PrivacyCard(Icons.Default.PhoneAndroid, "What is accessed", "Only media made available through Android MediaStore permissions, plus user-selected files through the Storage Access Framework.") }; item { PrivacyCard(Icons.Default.Security, "What stays on device", "Scanning, metadata, analysis results and settings remain local. Media content is not uploaded for normal cleaning.") }; item { PrivacyCard(Icons.Default.Block, "What is never requested", "No broad storage permission, contacts, SMS, microphone, location or camera permission is used by the core cleaner.") }; item { Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Optional product analytics", fontWeight = FontWeight.SemiBold); Text("Off by default. Never includes file names, contents or full paths.", style = MaterialTheme.typography.bodySmall) }; Switch(checked = analyticsEnabled, onCheckedChange = { analyticsEnabled = it; preferences.analyticsEnabled = it }) } } } } }
@Composable private fun PrivacyCard(icon: ImageVector, title: String, body: String) { Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
@Composable private fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Construction, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("This workspace is planned for the next implementation phase.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

private fun formatBytes(bytes: Long): String { if (bytes < 1024) return "$bytes B"; val units = arrayOf("KB", "MB", "GB", "TB"); var value = bytes.toDouble(); var index = -1; do { value /= 1024.0; index++ } while (value >= 1024 && index < units.lastIndex); return "%.1f %s".format(value, units[index]) }
