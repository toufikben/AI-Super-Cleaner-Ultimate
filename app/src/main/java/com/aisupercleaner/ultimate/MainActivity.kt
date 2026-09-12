package com.aisupercleaner.ultimate

import android.os.Bundle
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings
import android.net.Uri
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
import com.aisupercleaner.ultimate.data.TrashPolicy
import com.aisupercleaner.ultimate.data.SmartCleanupReport
import com.aisupercleaner.ultimate.data.StorageScanner
import com.aisupercleaner.ultimate.data.ScanProgress
import com.aisupercleaner.ultimate.data.VideoPreset
import com.aisupercleaner.ultimate.data.CompressionManager
import com.aisupercleaner.ultimate.data.CompressionResult
import com.aisupercleaner.ultimate.data.ImagePreset
import com.aisupercleaner.ultimate.data.StorageCategoryAggregate
import com.aisupercleaner.ultimate.data.FileMetadataEntity
import com.aisupercleaner.ultimate.data.JunkCandidatePolicy
import com.aisupercleaner.ultimate.data.JunkClassification
import com.aisupercleaner.ultimate.ads.AdManager
import com.aisupercleaner.ultimate.billing.BillingManager
import com.aisupercleaner.ultimate.privacy.AppPreferences
import com.aisupercleaner.ultimate.privacy.StoragePermissionManager
import com.aisupercleaner.ultimate.privacy.ConsentManager
import com.aisupercleaner.ultimate.privacy.CleanerAlertType
import com.aisupercleaner.ultimate.privacy.NotificationCenter
import com.aisupercleaner.ultimate.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = AppPreferences(this)
        setContent {
            var darkTheme by remember { mutableStateOf(preferences.darkTheme) }
            var accentStyle by remember { mutableStateOf(preferences.accentStyle) }
            AISuperCleanerTheme(darkTheme, accentStyle) { CleanerApp { dark, accent -> darkTheme = dark; accentStyle = accent; preferences.darkTheme = dark; preferences.accentStyle = accent } }
        }
    }
    companion object { const val EXTRA_DESTINATION = "destination" }
}

data class NavItem(val label: String, val icon: ImageVector)
data class Finding(val title: String, val detail: String, val size: String, val icon: ImageVector, val tint: Color)

@Composable
fun CleanerApp(onThemeChanged: (Boolean, String) -> Unit = { _, _ -> }) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }
    val database = remember { AppDatabase.get(context) }
    val scanner = remember { StorageScanner(context.contentResolver, database.storageDao()) }
    val recommendationEngine = remember { RecommendationEngine(database.storageDao()) }
    val duplicateEngine = remember { DuplicateEngine(context.contentResolver, database.storageDao()) }
    val cleanupManager = remember { CleanupManager(context.contentResolver, database.storageDao()) }
    val compressionManager = remember { CompressionManager(context, context.contentResolver, database.storageDao()) }
    val adManager = remember { AdManager(context) }
    val consentManager = remember { ConsentManager(context) }
    val canRequestAds by consentManager.canRequestAds.collectAsStateWithLifecycle()
    val billingManager = remember { BillingManager(context) }
    val isPremium by billingManager.isPremium.collectAsStateWithLifecycle()
    val billingMessage by billingManager.message.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    LaunchedEffect(activity) {
        activity?.let { current ->
            consentManager.requestConsent(current) { adManager.setCanRequestAds(canRequestAds && !isPremium) }
        }
    }
    LaunchedEffect(canRequestAds, isPremium) { adManager.setCanRequestAds(canRequestAds && !isPremium) }
    val consentError by consentManager.error.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val fileCount by database.storageDao().observeFileCount().collectAsStateWithLifecycle(initialValue = 0)
    val totalBytes by database.storageDao().observeTotalBytes().collectAsStateWithLifecycle(initialValue = 0L)
    val trashItems by database.storageDao().observeTrash().collectAsStateWithLifecycle(initialValue = emptyList())
    val largeFileItems by database.storageDao().observeLargeFiles(500L * 1024L * 1024L).collectAsStateWithLifecycle(initialValue = emptyList())
    val imageCount by database.storageDao().observeCountByType("image").collectAsStateWithLifecycle(initialValue = 0)
    val videoCount by database.storageDao().observeCountByType("video").collectAsStateWithLifecycle(initialValue = 0)
    val audioCount by database.storageDao().observeCountByType("audio").collectAsStateWithLifecycle(initialValue = 0)
    var storageRefreshToken by remember { mutableIntStateOf(0) }
    val statFs = remember(storageRefreshToken) { StatFs(Environment.getDataDirectory().path) }
    val freeBytes = remember { statFs.availableBytes }
    val totalStorageBytes = remember { statFs.totalBytes }
    var selected by remember { mutableIntStateOf((context as? MainActivity)?.intent?.getIntExtra(MainActivity.EXTRA_DESTINATION, 0) ?: 0) }
    var showPermissionEducation by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    var scanState by remember { mutableStateOf<ScanUiState>(ScanUiState.Idle) }
    val isScanning = scanState.isBusy()
    val scanProgress = (scanState as? ScanUiState.Scanning)?.progress
    var report by remember { mutableStateOf<SmartCleanupReport?>(null) }
    var mediaReport by remember { mutableStateOf<MediaAnalysisReport?>(null) }
    var mediaReportFileCount by remember { mutableIntStateOf(0) }
    val notificationCenter = remember { NotificationCenter(context) }
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(fileCount, totalBytes, isScanning) {
        if (!isScanning && mediaReport != null && mediaReportFileCount != fileCount) mediaReport = null
        if (!isScanning && fileCount > 0) {
            report = recommendationEngine.analyze(totalStorageBytes - freeBytes, freeBytes)
        }
    }
    LaunchedEffect(totalBytes, trashItems.size, largeFileItems.size, report, isScanning) {
        if (!isScanning && totalStorageBytes > 0L) {
            val usedRatio = (totalStorageBytes - freeBytes).toFloat() / totalStorageBytes.toFloat()
            if (usedRatio >= .9f) notificationCenter.notifyIfAllowed(CleanerAlertType.STORAGE, context.getString(R.string.notification_alert_storage), "Storage usage is above 90%. Review Storage Analyzer.", 2)
        }
        val trashBytes = trashItems.sumOf { it.sizeBytes }
        if (trashBytes >= 500L * 1024L * 1024L) notificationCenter.notifyIfAllowed(CleanerAlertType.TRASH, context.getString(R.string.notification_alert_trash), "Trash contains ${formatBytes(trashBytes)}. Review Trash Manager.", 3)
        if (largeFileItems.size >= 10) notificationCenter.notifyIfAllowed(CleanerAlertType.LARGE_FILES, context.getString(R.string.notification_alert_large_files), "${largeFileItems.size} files are at least 500 MB. Review Large Files.", 2)
        val memoryInfo = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memoryInfo)
        if (memoryInfo.totalMem > 0L && memoryInfo.availMem < memoryInfo.totalMem / 10L) notificationCenter.notifyIfAllowed(CleanerAlertType.RAM, context.getString(R.string.notification_alert_ram), "Available RAM is below 10%. Open Device Insights for details.", 2)
        if ((report?.potentialReviewBytes ?: 0L) > 0L) notificationCenter.notifyIfAllowed(CleanerAlertType.OPPORTUNITY, context.getString(R.string.notification_alert_opportunity), "${formatBytes(report?.potentialReviewBytes ?: 0L)} may be worth reviewing.", 1)
    }
    val startScan: () -> Unit = {
        if (!isScanning) scope.launch {
            scanState = ScanUiState.Scanning()
            try {
                scanner.scan { progress -> scanState = ScanUiState.Scanning(progress) }
                scanState = ScanUiState.Analyzing
                mediaReport = duplicateEngine.analyze()
                mediaReportFileCount = database.storageDao().allFiles().size
                scanState = ScanUiState.Success("Scan complete. Results are saved locally for review.")
                permissionMessage = (scanState as ScanUiState.Success).message
            } catch (_: SecurityException) {
                scanState = ScanUiState.PermissionRequired
                permissionMessage = "Media access was denied. Nothing was scanned."
            } catch (_: kotlinx.coroutines.CancellationException) {
                scanState = ScanUiState.Cancelled
                permissionMessage = "Scan cancelled. No destructive action was taken."
            } catch (_: Exception) {
                scanState = ScanUiState.Error("The scan could not finish. No destructive action was taken.")
                permissionMessage = (scanState as ScanUiState.Error).message
            }
        }
    }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) startScan() else permissionMessage = "Access was not granted. Nothing was scanned."
    }
    val navItems = listOf(NavItem("Home", Icons.Default.Home), NavItem("Clean", Icons.Default.AutoAwesome), NavItem("Analyze", Icons.Default.PieChart), NavItem("Tools", Icons.Default.Build), NavItem("Settings", Icons.Default.Settings))
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface) { navItems.forEachIndexed { index, item -> NavigationBarItem(selected = selected == index, onClick = { selected = index }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 11.sp) }) } } }) { padding ->
        when (selected) {
            0 -> HomeScreen(Modifier.padding(padding), fileCount, totalBytes, imageCount, videoCount, audioCount, freeBytes, totalStorageBytes, report, mediaReport, scanProgress, isScanning, database, cleanupManager, onInvalidated = { mediaReport = null; report = null; storageRefreshToken++ }, onSmartScan = { showPermissionEducation = true }, onQuickClean = { selected = 1 })
            1 -> CleanScreen(Modifier.padding(padding), database, cleanupManager, adManager, isPremium, onInvalidated = { mediaReport = null; report = null; storageRefreshToken++ }, onAdvancedScan = { showPermissionEducation = true })
            2 -> AnalyzeScreen(Modifier.padding(padding), database, cleanupManager, onInvalidated = { mediaReport = null; report = null; storageRefreshToken++ })
            3 -> ToolsScreen(Modifier.padding(padding), database, cleanupManager, compressionManager, onInvalidated = { mediaReport = null; report = null; storageRefreshToken++ })
            4 -> Column(Modifier.padding(padding)) { PremiumPaywall(billingManager, isPremium); ThemeSettings(preferences, onThemeChanged); NotificationCenterSettings(preferences) { notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }; PrivacyOptionsEntry(consentManager); PrivacyCenterScreen(Modifier.weight(1f), preferences) }
            else -> PlaceholderScreen(navItems[selected].label, Modifier.padding(padding))
        }
    }
    if (showPermissionEducation) PermissionEducationDialog(onDismiss = { showPermissionEducation = false }, onContinue = { showPermissionEducation = false; val missing = StoragePermissionManager.missingPermissions(context); if (missing.isEmpty()) startScan() else permissionLauncher.launch(missing) })
    permissionMessage?.let { message -> AlertDialog(onDismissRequest = { permissionMessage = null }, confirmButton = { TextButton(onClick = { permissionMessage = null }) { Text("OK") } }, title = { Text("Scan status") }, text = { Text(message) }) }
    billingMessage?.let { message -> AlertDialog(onDismissRequest = { billingManager.clearMessage() }, confirmButton = { TextButton(onClick = { billingManager.clearMessage() }) { Text("OK") } }, title = { Text("Premium status") }, text = { Text(message) }) }
    consentError?.let { message -> AlertDialog(onDismissRequest = { consentManager.clearError() }, confirmButton = { TextButton(onClick = { consentManager.clearError() }) { Text("OK") } }, title = { Text("Privacy consent") }, text = { Text("Consent update could not complete: $message. Ads remain disabled until consent can be evaluated.") }) }
}

@Composable
fun HomeScreen(modifier: Modifier, fileCount: Int, totalBytes: Long, imageCount: Int, videoCount: Int, audioCount: Int, freeBytes: Long, totalStorageBytes: Long, report: SmartCleanupReport?, mediaReport: MediaAnalysisReport?, progress: ScanProgress?, isScanning: Boolean, database: AppDatabase, cleanupManager: CleanupManager, onInvalidated: () -> Unit, onSmartScan: () -> Unit, onQuickClean: () -> Unit) {
    val findings = listOf(
        Finding("Photos", "$imageCount items · From MediaStore", "—", Icons.Default.PhotoLibrary, SoftMint),
        Finding("Videos", "$videoCount items · From MediaStore", "—", Icons.Default.VideoLibrary, SoftBlue),
        Finding("Audio", "$audioCount items · From MediaStore", "—", Icons.Default.Audiotrack, SoftLavender)
    )
    var indexedFiles by remember { mutableStateOf(emptyList<com.aisupercleaner.ultimate.data.FileMetadataEntity>()) }
    var selectedUris by remember { mutableStateOf(setOf<String>()) }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(report) { indexedFiles = if (report == null) emptyList() else database.storageDao().allFiles() }
    val selectedItems = indexedFiles.filter { it.uri in selectedUris }
    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Header() }
            item { ScoreCard(fileCount, totalBytes, freeBytes, totalStorageBytes, report) }
            if (report != null) item { Text("Storage pressure ${report.health.pressurePercent}% · Review potential ${formatBytes(report.health.cleanupPotentialBytes)} · Select recommendations for review", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { PrimaryActions(onSmartScan, onQuickClean, isScanning) }
            if (progress != null) item { ScanProgressCard(progress) }
            if (report != null) {
                item { ExplainableSummary(report) }
                items(report.recommendations) { recommendation ->
                    val candidates = indexedFiles.filter { com.aisupercleaner.ultimate.data.RecommendationCandidatePolicy.matches(recommendation.category, it) }
                    RecommendationCard(recommendation, candidates, selectedUris) { uri, checked -> selectedUris = if (checked) selectedUris + uri else selectedUris - uri }
                }
            }
            if (mediaReport != null) item { MediaAnalysisSummary(mediaReport) }
            item { SectionTitle("On-device inventory", "Live counts from Android MediaStore") }
            items(findings) { FindingCard(it) }
            item { TrustNote() }
        }
        if (selectedItems.isNotEmpty()) {
            Text("${selectedItems.size} recommendation files selected · ${formatBytes(selectedItems.sumOf { it.sizeBytes })}", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Move selected to Trash") }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Move recommendation files to Trash?") }, text = { Text("${selectedItems.size} selected files (${formatBytes(selectedItems.sumOf { it.sizeBytes })}) will be revalidated before the safe Trash operation.") }, confirmButton = { Button(onClick = { confirm = false; scope.launch { when (val result = cleanupManager.moveToTrash(selectedItems)) { is com.aisupercleaner.ultimate.data.CleanupResult.Success -> { message = "${result.itemsMoved} items moved to Trash."; selectedUris = emptySet() }; is com.aisupercleaner.ultimate.data.CleanupResult.PartialSuccess -> { message = result.message; selectedUris = emptySet() }; is com.aisupercleaner.ultimate.data.CleanupResult.Failure -> message = result.message }; onInvalidated() } }) { Text("Move to Trash") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }, title = { Text("Recommendation cleanup status") }, text = { Text(it) }) }
}

@Composable private fun RecommendationCard(recommendation: com.aisupercleaner.ultimate.data.Recommendation, candidates: List<com.aisupercleaner.ultimate.data.FileMetadataEntity>, selectedUris: Set<String>, onSelectedChange: (String, Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(recommendation.category, fontWeight = FontWeight.SemiBold); Text("${recommendation.fileCount} files · ${recommendation.confidencePercent}% confidence", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary) }; Text(formatBytes(recommendation.estimatedBytes), fontWeight = FontWeight.Bold) }
        Text(recommendation.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (candidates.isEmpty()) Text("No current file rows match this recommendation. Run Smart Scan again.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        candidates.take(50).forEach { file -> Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = file.uri in selectedUris, onCheckedChange = { onSelectedChange(file.uri, it) }); Column { Text(file.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold); Text(formatBytes(file.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
    } }
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
@Composable private fun MediaAnalysisSummary(report: MediaAnalysisReport) { Card(colors = CardDefaults.cardColors(containerColor = SoftLavender), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PhotoFilter, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Text("Media analysis", fontWeight = FontWeight.SemiBold) }; Text("${report.duplicateGroups.size} exact duplicate groups · ${report.similarGroups.size} similar groups", fontWeight = FontWeight.Bold); Text("${report.blurredFiles.size} potentially blurry photos · ${report.screenshotFiles.size} screenshots", style = MaterialTheme.typography.bodySmall); Text("Groups are shown for review. Nothing is selected or deleted automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

private enum class LargeFileSort(val label: String, val comparator: Comparator<FileMetadataEntity>) {
    SIZE("Largest", compareByDescending<FileMetadataEntity> { it.sizeBytes }.thenBy { it.displayName.lowercase() }),
    RECENT("Recently changed", compareByDescending<FileMetadataEntity> { it.modifiedEpochSeconds }.thenByDescending { it.sizeBytes }),
    TYPE("By type", compareBy<FileMetadataEntity> { it.mediaType }.thenByDescending { it.sizeBytes })
}

@Composable
private fun LargeFilesControls(minimumBytes: Long, sortMode: LargeFileSort, onMinimumChange: (Long) -> Unit, onSortChange: (LargeFileSort) -> Unit, resultCount: Int) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Smart sorting", fontWeight = FontWeight.SemiBold)
            Text("$resultCount matching files · threshold ${formatBytes(minimumBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(100L, 500L, 1024L).forEach { mb -> OutlinedButton(onClick = { onMinimumChange(mb * 1024L * 1024L) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) { Text("${mb}MB") } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LargeFileSort.values().forEach { mode -> FilterChip(selected = sortMode == mode, onClick = { onSortChange(mode) }, label = { Text(mode.label) }, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FolderBreakdownCard(files: List<FileMetadataEntity>) {
    val folders = remember(files) {
        files.groupBy { file -> file.relativePath?.substringBeforeLast('/', missingDelimiterValue = "Device root") ?: "Location unavailable" }
            .map { (folder, items) -> folder to items.sumOf { it.sizeBytes } }
            .sortedByDescending { it.second }
            .take(5)
    }
    val total = folders.sumOf { it.second }.coerceAtLeast(1L)
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftLavender)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Top folders", fontWeight = FontWeight.Bold)
            Text("Folder paths are shown only when Android exposes them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (folders.isEmpty()) Text("Run Smart Scan to build folder insights.", style = MaterialTheme.typography.bodySmall) else folders.forEach { (folder, bytes) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(folder.ifBlank { "Unnamed folder" }, Modifier.weight(1f), maxLines = 1); Text(formatBytes(bytes), fontWeight = FontWeight.SemiBold) }
                LinearProgressIndicator(progress = { (bytes.toFloat() / total).coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable private fun AnalyzeScreen(modifier: Modifier, database: AppDatabase, cleanupManager: CleanupManager, onInvalidated: () -> Unit) {
    val context = LocalContext.current
    val categoryAggregates by database.storageDao().observeCategoryAggregates().collectAsStateWithLifecycle(initialValue = emptyList())
    val allFiles by database.storageDao().observeAllFiles().collectAsStateWithLifecycle(initialValue = emptyList())
    var minimumBytes by remember { mutableLongStateOf(500L * 1024L * 1024L) }
    var sortMode by remember { mutableStateOf(LargeFileSort.SIZE) }
    val largeFiles = remember(allFiles, minimumBytes, sortMode) { allFiles.filter { it.sizeBytes >= minimumBytes }.sortedWith(sortMode.comparator) }
    val downloads = remember(allFiles) { allFiles.filter { it.relativePath?.contains("Download", ignoreCase = true) == true } }
    val generalFiles = remember(allFiles) { allFiles.filter { it.mediaType in setOf("document", "archive", "apk", "other") } }
    val allVisible = allFiles
    var selectedUris by remember { mutableStateOf(setOf<String>()) }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val selectedItems = allVisible.filter { it.uri in selectedUris }
    fun toggle(uri: String, checked: Boolean) { selectedUris = if (checked) selectedUris + uri else selectedUris - uri }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp)) {
            item { Text("Storage Analyzer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("See what is using space, review large files, and sort them without fake cleaning claims.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { StorageBreakdownCard(categoryAggregates) }
            item { FolderBreakdownCard(allFiles) }
            item { DeviceSignalsCard(context) }
            item { LargeFilesControls(minimumBytes, sortMode, onMinimumChange = { minimumBytes = it }, onSortChange = { sortMode = it }, resultCount = largeFiles.size) }
            item { Text("Large files · ${formatBytes(minimumBytes)}+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (largeFiles.isEmpty()) item { EmptyState("No large files indexed", "Run Smart Scan to refresh the inventory.") }
            items(largeFiles.take(100), key = { it.uri }) { item -> SelectableAnalysisFileRow(item, "Large ${item.mediaType}", item.uri in selectedUris) { checked -> toggle(item.uri, checked) } }
            item { Text("Downloads", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            val downloadItems = downloads.filter { it.relativePath?.contains("Download", ignoreCase = true) == true }
            if (downloadItems.isEmpty()) item { EmptyState("No Downloads matched", "Only locations exposed by Android MediaStore are shown.") }
            items(downloadItems.take(50)) { item -> SelectableAnalysisFileRow(item, item.relativePath ?: "Download location unavailable", item.uri in selectedUris) { checked -> toggle(item.uri, checked) } }
            item { Text("APKs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            val apkItems = generalFiles.filter { it.mediaType == "apk" || it.mimeType.equals("application/vnd.android.package-archive", ignoreCase = true) || it.displayName.endsWith(".apk", ignoreCase = true) }
            if (apkItems.isEmpty()) item { EmptyState("No APKs indexed", "APK files exposed by Android MediaStore will appear here.") }
            items(apkItems.take(50)) { item -> SelectableAnalysisFileRow(item, "APK", item.uri in selectedUris) { checked -> toggle(item.uri, checked) } }
            item { Text("Documents and other files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            val documentItems = generalFiles.filter { it.mediaType == "document" || it.mediaType == "archive" || it.mediaType == "other" }.filterNot { it.mediaType == "apk" }
            if (documentItems.isEmpty()) item { EmptyState("No documents indexed", "PDF, Office, text, archive, and other exposed files appear here after scanning.") }
            items(documentItems.take(100)) { item -> SelectableAnalysisFileRow(item, item.mediaType.replaceFirstChar { it.uppercase() }, item.uri in selectedUris) { checked -> toggle(item.uri, checked) } }
        }
        if (selectedItems.isNotEmpty()) {
            Text("${selectedItems.size} selected · ${formatBytes(selectedItems.sumOf { it.sizeBytes })}", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
            Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Move selected to Trash") }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Move selected files to Trash?") }, text = { Text("${selectedItems.size} files (${formatBytes(selectedItems.sumOf { it.sizeBytes })}) will be revalidated before being moved. Nothing is permanently deleted now.") }, confirmButton = { Button(onClick = { confirm = false; scope.launch { when (val result = cleanupManager.moveToTrash(selectedItems)) { is com.aisupercleaner.ultimate.data.CleanupResult.Success -> { message = "${result.itemsMoved} items moved to Trash."; selectedUris = emptySet() }; is com.aisupercleaner.ultimate.data.CleanupResult.PartialSuccess -> { message = result.message; selectedUris = emptySet() }; is com.aisupercleaner.ultimate.data.CleanupResult.Failure -> message = result.message }; onInvalidated() } }) { Text("Move to Trash") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }, title = { Text("Cleanup status") }, text = { Text(it) }) }
}

@Composable
private fun StorageBreakdownCard(categories: List<StorageCategoryAggregate>) {
    val total = categories.sumOf { it.totalBytes }.coerceAtLeast(1L)
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftMint)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DonutLarge, null, tint = ScoreAccent)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Storage breakdown", fontWeight = FontWeight.Bold)
                    Text("Based on the files currently indexed on-device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (categories.isEmpty()) {
                Text("Run Smart Scan to build your local storage map.", style = MaterialTheme.typography.bodySmall)
            } else {
                categories.take(5).forEach { category ->
                    val fraction = (category.totalBytes.toFloat() / total).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category.mediaType.storageLabel(), fontWeight = FontWeight.SemiBold)
                            Text("${formatBytes(category.totalBytes)} · ${category.fileCount}", style = MaterialTheme.typography.bodySmall)
                        }
                        LinearProgressIndicator(progress = { fraction }, Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = category.mediaType.storageColor(), trackColor = MaterialTheme.colorScheme.surface.copy(alpha = .65f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSignalsCard(context: android.content.Context) {
    val activityManager = remember(context) { context.getSystemService(ActivityManager::class.java) }
    val batteryIntent = remember(context) { context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }
    val memoryInfo = remember(activityManager) { ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) } }
    val batteryPercent = remember(batteryIntent) { batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)?.takeIf { it >= 0 } }
    val batteryStatus = remember(batteryIntent) { batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) }
    val isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING || batteryStatus == BatteryManager.BATTERY_STATUS_FULL
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text("Device signals", fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SignalTile(Modifier.weight(1f), Icons.Default.Memory, "RAM available", formatBytes(memoryInfo.availMem))
                SignalTile(Modifier.weight(1f), Icons.Default.BatteryStd, "Battery", batteryPercent?.let { "$it%${if (isCharging) " · charging" else ""}" } ?: "Unavailable")
            }
            Text("Android does not provide a safe universal RAM-cleaning action. These are read-only device signals.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }, modifier = Modifier.weight(1f)) { Text("System settings") }
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) }, modifier = Modifier.weight(1f)) { Text("Battery settings") }
            }
        }
    }
}

@Composable
private fun SignalTile(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }
    }
}

private fun String.storageLabel(): String = when (lowercase()) {
    "image" -> "Photos"
    "video" -> "Videos"
    "audio" -> "Audio"
    "document" -> "Documents"
    "archive" -> "Archives"
    "apk" -> "APK files"
    else -> "Other files"
}

private fun String.storageColor(): Color = when (lowercase()) {
    "image" -> ScoreAccent
    "video" -> Color(0xFF5C7CFA)
    "audio" -> Color(0xFF8E6CCF)
    "document" -> Color(0xFFD18B37)
    else -> Color(0xFF159A9C)
}

@Composable private fun SelectableAnalysisFileRow(item: com.aisupercleaner.ultimate.data.FileMetadataEntity, label: String, selected: Boolean, onSelectedChange: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = onSelectedChange)
        Icon(if (item.mediaType == "video") Icons.Default.VideoFile else if (item.mediaType == "audio") Icons.Default.AudioFile else Icons.Default.InsertDriveFile, null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(item.displayName, fontWeight = FontWeight.SemiBold); Text("$label · ${formatBytes(item.sizeBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (item.durationMillis > 0) Text("Duration ${item.durationMillis / 1000}s", style = MaterialTheme.typography.labelSmall) }
    } }
}

private data class InstalledAppRow(val packageName: String, val label: String, val apkBytes: Long, val isSystem: Boolean)

@Composable
private fun AppManagerPanel() {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledAppRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            context.packageManager.getInstalledApplications(0)
                .asSequence()
                .filter { it.packageName != context.packageName }
                .map { info ->
                    val source = info.sourceDir?.let { java.io.File(it) }
                    InstalledAppRow(info.packageName, info.loadLabel(context.packageManager).toString().ifBlank { info.packageName }, source?.takeIf { it.exists() }?.length() ?: 0L, (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
                }
                .sortedByDescending { it.apkBytes }
                .take(30)
                .toList()
        }
        loading = false
    }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftLavender)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Apps, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("App Manager", fontWeight = FontWeight.Bold)
                    Text("Largest visible app packages · APK size only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Android does not expose a safe universal API for clearing another app’s private cache. Use each app’s system page for permitted actions.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Reading installed apps…", style = MaterialTheme.typography.bodySmall) }
            } else if (apps.isEmpty()) {
                EmptyState("No visible apps", "Package visibility or device policy limited the list.")
            } else {
                apps.take(5).forEach { AppManagerRow(it, context) }
                Text("Showing ${apps.size} visible apps, sorted by APK size. App data and cache are not guessed.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AppManagerRow(app: InstalledAppRow, context: android.content.Context) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(38.dp), shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .7f)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Android, null, tint = MaterialTheme.colorScheme.secondary) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${formatBytes(app.apkBytes)}${if (app.isSystem) " · system" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))) }) { Icon(Icons.Default.Settings, "App settings") }
        if (!app.isSystem) IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))) }) { Icon(Icons.Default.DeleteOutline, "Uninstall") }
    }
}

private data class BrowserTarget(val label: String, val packageName: String)

@Composable
private fun JunkCleanerPanel(database: AppDatabase, cleanupManager: CleanupManager, onInvalidated: () -> Unit) {
    val files by database.storageDao().observeGeneralFiles().collectAsStateWithLifecycle(initialValue = emptyList())
    val now = remember { System.currentTimeMillis() / 1000L }
    val candidates: List<Pair<FileMetadataEntity, JunkClassification>> = remember(files, now) {
        files.mapNotNull { file ->
            val classification = JunkCandidatePolicy.classify(file, now)
            if (classification is JunkClassification.NotCandidate) null else file to classification
        }
    }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val selectedItems = candidates.map { it.first }.filter { it.uri in selected }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftAmber)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoDelete, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text("Junk Cleaner", fontWeight = FontWeight.Bold); Text("Review-only temporary leftovers and old APKs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            Text("Only files exposed by Android MediaStore are considered. Thresholds: temporary names are at least 7 days old and 50 MB or smaller; APK review starts at 30 days. Nothing is selected automatically.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (candidates.isEmpty()) EmptyState("No safe junk candidates", "Run Smart Scan and expose general files through Android permissions.")
            candidates.take(5).forEach { (file, classification) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(file.uri in selected, onCheckedChange = { checked -> selected = if (checked) selected + file.uri else selected - file.uri })
                    Column(Modifier.weight(1f)) { Text(file.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1); Text("${formatBytes(file.sizeBytes)} · ${junkLabel(classification)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            if (candidates.size > 5) Text("${candidates.size - 5} more candidates available in the full review list.", style = MaterialTheme.typography.labelSmall)
            Button(onClick = { confirm = true }, enabled = selectedItems.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Move selected to Trash · ${formatBytes(selectedItems.sumOf { it.sizeBytes })}") }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Review junk selection") }, text = { Text("${selectedItems.size} selected files will be revalidated and moved to recoverable Trash. Android may reject files that changed or are no longer accessible.") }, confirmButton = { Button(onClick = { confirm = false; scope.launch { message = when (val result = cleanupManager.moveToTrash(selectedItems)) { is com.aisupercleaner.ultimate.data.CleanupResult.Success -> { selected = emptySet(); onInvalidated(); "${result.itemsMoved} files moved to Trash." }; is com.aisupercleaner.ultimate.data.CleanupResult.PartialSuccess -> { selected = emptySet(); onInvalidated(); result.message }; is com.aisupercleaner.ultimate.data.CleanupResult.Failure -> result.message } } }) { Text("Move to Trash") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }, title = { Text("Junk Cleaner") }, text = { Text(it) }) }
}

private fun junkLabel(classification: JunkClassification): String = when (classification) {
    is JunkClassification.TemporaryLeftover -> "temporary · ${classification.ageDays} days old"
    is JunkClassification.ObsoleteApkReview -> "APK · ${classification.ageDays} days old"
    JunkClassification.NotCandidate -> "not selected"
}

@Composable
private fun BrowserCleanerPanel() {
    val context = LocalContext.current
    val targets = remember { listOf(BrowserTarget("Chrome", "com.android.chrome"), BrowserTarget("Firefox", "org.mozilla.firefox"), BrowserTarget("Edge", "com.microsoft.emmx"), BrowserTarget("Brave", "com.brave.browser"), BrowserTarget("Opera", "com.opera.browser"), BrowserTarget("Samsung Internet", "com.sec.android.app.sbrowser")) }
    val installed = remember(targets) { targets.filter { runCatching { context.packageManager.getApplicationInfo(it.packageName, 0) }.isSuccess } }
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftBlue)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text("Browser Cleaner", fontWeight = FontWeight.Bold); Text("Permission-aware browser storage shortcuts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            Text("Android does not let this app silently clear another app’s private cache or cookies. These buttons open the browser’s system App info page so you can review and use the actions Android permits.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (installed.isEmpty()) Text("No supported browser was visible to Android on this device.", style = MaterialTheme.typography.bodySmall)
            installed.forEach { browser ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(10.dp)); Text(browser.label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${browser.packageName}"))) }) { Text("Open settings") } }
            }
        }
    }
}

@Composable private fun ToolsScreen(modifier: Modifier, database: AppDatabase, cleanupManager: CleanupManager, compressionManager: CompressionManager, onInvalidated: () -> Unit) {
    val compressionHistory by database.storageDao().observeCompressionHistory().collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedVideo by remember { mutableStateOf<android.net.Uri?>(null) }; var selectedImage by remember { mutableStateOf<android.net.Uri?>(null) }; var isCompressing by remember { mutableStateOf(false) }; var resultMessage by remember { mutableStateOf<String?>(null) }; val scope = rememberCoroutineScope()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> selectedVideo = uri }
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri -> selectedImage = uri }
    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp)) {
            item { Text("Tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Manage apps and compress copies without overwriting originals.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { AppManagerPanel() }
            item { Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Video Compressor", fontWeight = FontWeight.SemiBold); Text(selectedVideo?.toString() ?: "No video selected", style = MaterialTheme.typography.bodySmall); Button(onClick = { picker.launch("video/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.VideoFile, null); Spacer(Modifier.width(8.dp)); Text("Choose video") }; Button(onClick = { selectedVideo?.let { uri -> scope.launch { isCompressing = true; try { val result = compressionManager.compressVideoCopy(uri, VideoPreset.BALANCED, 0L); resultMessage = when (result) { is CompressionResult.Success -> "Compressed copy exported to MediaStore: ${result.outputUri}"; is CompressionResult.Failure -> result.message } } finally { isCompressing = false } } } }, enabled = selectedVideo != null && !isCompressing, modifier = Modifier.fillMaxWidth()) { if (isCompressing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Compress and export copy") } } } }
            item { Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Image Compressor", fontWeight = FontWeight.SemiBold); Text(selectedImage?.toString() ?: "No image selected", style = MaterialTheme.typography.bodySmall); Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Image, null); Spacer(Modifier.width(8.dp)); Text("Choose image") }; Button(onClick = { selectedImage?.let { uri -> scope.launch { isCompressing = true; try { val result = compressionManager.compressImageCopy(uri, ImagePreset.BALANCED, 0L); resultMessage = when (result) { is CompressionResult.Success -> "Compressed image exported to MediaStore: ${result.outputUri}"; is CompressionResult.Failure -> result.message } } finally { isCompressing = false } } } }, enabled = selectedImage != null && !isCompressing, modifier = Modifier.fillMaxWidth()) { if (isCompressing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Compress and export copy") } } } }
            item { Text("Trash", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { Text("Compression history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (compressionHistory.isEmpty()) item { EmptyState("No compression history", "Completed and failed attempts will be recorded locally.") }
            items(compressionHistory) { item -> Card(shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (item.mediaType == "video") Icons.Default.VideoFile else Icons.Default.Image, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("${item.mediaType.replaceFirstChar { it.uppercase() }} · ${item.preset}", fontWeight = FontWeight.SemiBold); Text("${formatBytes(item.originalBytes)} → ${formatBytes(item.outputBytes)} · ${item.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        }
        TrashScreen(Modifier.weight(1f), database, cleanupManager, onInvalidated)
    }
    resultMessage?.let { AlertDialog(onDismissRequest = { resultMessage = null }, confirmButton = { TextButton(onClick = { resultMessage = null }) { Text("OK") } }, title = { Text("Compression status") }, text = { Text(it) }) }
}
@Composable private fun CleanScreen(modifier: Modifier, database: AppDatabase, cleanupManager: CleanupManager, adManager: AdManager, isPremium: Boolean, onInvalidated: () -> Unit, onAdvancedScan: () -> Unit) {
    val candidates by database.storageDao().observeDuplicateCandidates().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope(); var selected by remember { mutableStateOf(setOf<String>()) }; var confirm by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    val selectedItems = candidates.filter { it.uri in selected }; val activity = LocalActivity.current
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
        item { Text("Clean safely", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Quick Clean shows only exact-hash candidates. Advanced Smart Scan adds broader analysis for review.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { JunkCleanerPanel(database, cleanupManager, onInvalidated) }
        item { BrowserCleanerPanel() }
        item { OutlinedButton(onClick = onAdvancedScan, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Run Advanced Smart Scan") } }
        if (!isPremium) item { TextButton(onClick = { activity?.let { adManager.showRewardedAd(it, isPremium, onReward = onAdvancedScan, onUnavailable = { message = "Rewarded Ad is unavailable. Advanced Smart Scan remains available without an ad." }) } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayCircle, null); Spacer(Modifier.width(8.dp)); Text("Watch a short ad to unlock one Advanced Scan (optional)") } }
        item { Text("${selectedItems.size} selected · ${formatBytes(selectedItems.sumOf { it.sizeBytes })}", fontWeight = FontWeight.SemiBold) }
        if (candidates.isEmpty()) item { EmptyState("No analyzed duplicate candidates yet", "Run Smart Scan first. Nothing is selected by default.") }
        items(candidates) { item ->
            val checked = item.uri in selected
            Card(shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + item.uri else selected - item.uri }); Column(Modifier.weight(1f)) { Text(item.displayName, fontWeight = FontWeight.SemiBold); Text("${formatBytes(item.sizeBytes)} · duplicate hash available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
        }
        item { Button(onClick = { confirm = true }, enabled = selectedItems.isNotEmpty(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(8.dp)); Text("Move selected to Trash") } }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Move to Trash?") }, text = { Text("You selected ${selectedItems.size} files (${formatBytes(selectedItems.sumOf { it.sizeBytes })}). Android will retain recoverable items where supported. Nothing will be permanently deleted now.") }, confirmButton = { Button(onClick = { confirm = false; scope.launch { when (val result = cleanupManager.moveToTrash(selectedItems)) { is com.aisupercleaner.ultimate.data.CleanupResult.Success -> { message = "${result.itemsMoved} items moved to Trash."; selected = emptySet(); onInvalidated(); activity?.let { adManager.showInterstitialAfterCleanup(it, isPremium) {} } }; is com.aisupercleaner.ultimate.data.CleanupResult.PartialSuccess -> { message = result.message; selected = emptySet(); onInvalidated() }; is com.aisupercleaner.ultimate.data.CleanupResult.Failure -> { message = result.message; onInvalidated() } } } }) { Text("Move to Trash") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } })
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }, title = { Text("Clean status") }, text = { Text(it) }) }
}

@Composable private fun TrashScreen(modifier: Modifier, database: AppDatabase, cleanupManager: CleanupManager, onInvalidated: () -> Unit) {
    val appContext = LocalContext.current
    val itemsInTrash by database.storageDao().observeTrash().collectAsStateWithLifecycle(initialValue = emptyList()); val scope = rememberCoroutineScope(); var permanentTarget by remember { mutableStateOf<com.aisupercleaner.ultimate.data.TrashItemEntity?>(null) }; var message by remember { mutableStateOf<String?>(null) }; val preferences = remember { AppPreferences(appContext) }; var recoveredBytes by remember { mutableLongStateOf(preferences.recoveredBytes) }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
        item { Text("Trash Manager", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Restore items or permanently delete them after review.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard(Modifier.weight(1f), Icons.Default.DeleteSweep, "In Trash", formatBytes(itemsInTrash.sumOf { it.sizeBytes })); MetricCard(Modifier.weight(1f), Icons.Default.SdStorage, "Recovered", formatBytes(recoveredBytes)) } }
        item { Text("Recovered is counted only after Android confirms permanent deletion. Moving an item to Trash is not counted as recovered space.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (itemsInTrash.isEmpty()) item { EmptyState("Trash is empty", "Items moved through the safe workflow will appear here.") }
        items(itemsInTrash) { item -> Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(item.displayName, fontWeight = FontWeight.SemiBold); Text(formatBytes(item.sizeBytes), style = MaterialTheme.typography.bodySmall); Text("About ${TrashPolicy.remainingDays(item.trashedAtEpochMillis, System.currentTimeMillis())} days remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }; TextButton(onClick = { scope.launch { when (val result = cleanupManager.restore(item)) { is com.aisupercleaner.ultimate.data.CleanupResult.Success -> { message = "Item restored."; onInvalidated() }; is com.aisupercleaner.ultimate.data.CleanupResult.PartialSuccess -> { message = result.message; onInvalidated() }; is com.aisupercleaner.ultimate.data.CleanupResult.Failure -> { message = result.message; onInvalidated() } } } }) { Text("Restore") } }; OutlinedButton(onClick = { permanentTarget = item }, modifier = Modifier.fillMaxWidth()) { Text("Delete permanently") } } } }
    }
    permanentTarget?.let { target -> AlertDialog(onDismissRequest = { permanentTarget = null }, title = { Text("Delete permanently?") }, text = { Text("This action cannot be undone. Delete ${target.displayName} permanently?") }, confirmButton = { Button(onClick = { permanentTarget = null; scope.launch { when (val result = cleanupManager.deletePermanently(target)) { is com.aisupercleaner.ultimate.data.CleanupResult.Success -> { recoveredBytes += result.bytesMoved; preferences.recoveredBytes = recoveredBytes; message = "Permanently deleted ${formatBytes(result.bytesMoved)}. This is the confirmed space recovered by Android."; onInvalidated() }; is com.aisupercleaner.ultimate.data.CleanupResult.PartialSuccess -> { message = result.message; onInvalidated() }; is com.aisupercleaner.ultimate.data.CleanupResult.Failure -> { message = result.message; onInvalidated() } } } }) { Text("Delete permanently") } }, dismissButton = { TextButton(onClick = { permanentTarget = null }) { Text("Cancel") } }) }
    message?.let { AlertDialog(onDismissRequest = { message = null }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }, title = { Text("Trash status") }, text = { Text(it) }) }
}

@Composable
private fun MetricCard(modifier: Modifier, icon: ImageVector, label: String, value: String) {
    Card(modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SoftBlue)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun EmptyState(title: String, body: String) { Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Inventory2, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.SemiBold); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun SectionTitle(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun FindingCard(finding: Finding) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(finding.tint), contentAlignment = Alignment.Center) { Icon(finding.icon, null, tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(finding.title, fontWeight = FontWeight.SemiBold); Text(finding.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(finding.size, fontWeight = FontWeight.Bold) } } }
@Composable private fun TrustNote() { Card(colors = CardDefaults.cardColors(containerColor = SoftMint), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Lock, null, tint = ScoreAccent); Spacer(Modifier.width(12.dp)); Column { Text("Private by design", fontWeight = FontWeight.SemiBold); Text("Media metadata is indexed locally. Review every recommendation before cleaning.", style = MaterialTheme.typography.bodySmall) } } } }
@Composable private fun PermissionEducationDialog(onDismiss: () -> Unit, onContinue: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onContinue) { Text("Continue") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }, icon = { Icon(Icons.Default.FolderOpen, null) }, title = { Text("Choose what can be scanned") }, text = { Text("Smart Scan reads only media categories needed for an on-device inventory. Nothing is uploaded, inaccessible files are not scanned, and no broad storage permission is used.") }) }
@Composable
private fun ThemeSettings(preferences: AppPreferences, onChanged: (Boolean, String) -> Unit) {
    var dark by remember { mutableStateOf(preferences.darkTheme) }
    var accent by remember { mutableStateOf(preferences.accentStyle) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftLavender)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(if (dark) Icons.Default.DarkMode else Icons.Default.LightMode, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Switch(checked = dark, onCheckedChange = { dark = it; preferences.darkTheme = it; onChanged(it, accent) }) }
            Text("Choose a comfortable light or dark surface and a restrained cleaner accent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("teal" to "Teal", "violet" to "Violet", "coral" to "Coral").forEach { (key, label) -> FilterChip(selected = accent == key, onClick = { accent = key; preferences.accentStyle = key; onChanged(dark, key) }, label = { Text(label) }, modifier = Modifier.weight(1f)) } }
        }
    }
}

@Composable
private fun NotificationCenterSettings(preferences: AppPreferences, requestPermission: () -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(preferences.notificationsEnabled) }
    var alertStates by remember { mutableStateOf(CleanerAlertType.values().associateWith { preferences.alertEnabled(it) }) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SoftBlue)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(context.getString(R.string.notifications_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(context.getString(R.string.notifications_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(context.getString(R.string.notifications_enable), Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(checked = enabled, onCheckedChange = { enabled = it; preferences.notificationsEnabled = it; if (it) requestPermission() }) }
            if (!enabled) Text(context.getString(R.string.notifications_disabled), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (enabled) CleanerAlertType.values().forEach { type -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(context.getString(type.preferenceLabelRes), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Switch(checked = alertStates[type] == true, onCheckedChange = { checked -> preferences.setAlertEnabled(type, checked); alertStates = alertStates + (type to checked) }) } }
        }
    }
}

@Composable private fun PrivacyOptionsEntry(consentManager: ConsentManager) { val required by consentManager.privacyOptionsRequired.collectAsStateWithLifecycle(); val activity = LocalActivity.current; if (required) TextButton(onClick = { activity?.let { consentManager.showPrivacyOptions(it) {} } }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { Icon(Icons.Default.PrivacyTip, null); Spacer(Modifier.width(8.dp)); Text("Manage privacy and advertising choices") } }
@Composable private fun PremiumPaywall(billing: BillingManager, isPremium: Boolean) { val activity = LocalActivity.current; val catalog by billing.catalog.collectAsStateWithLifecycle(); Card(Modifier.fillMaxWidth().padding(20.dp, 16.dp, 20.dp, 8.dp), colors = CardDefaults.cardColors(containerColor = SoftLavender), shape = RoundedCornerShape(20.dp)) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(if (isPremium) "Premium active" else "Unlock Premium", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("${if (isPremium) "No ads and all tools are available." else "Remove ads and unlock the advanced toolkit."}", style = MaterialTheme.typography.bodySmall); Text("Advanced AI Scan · Similar Photos · Video compression · Scheduled Scan · Storage history", style = MaterialTheme.typography.bodySmall); if (!isPremium) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { activity?.let { billing.launchMonthly(it) } }, enabled = catalog.monthly != null, modifier = Modifier.weight(1f)) { Text(catalog.monthly?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "Monthly") }; OutlinedButton(onClick = { activity?.let { billing.launchLifetime(it) } }, enabled = catalog.lifetime != null, modifier = Modifier.weight(1f)) { Text(catalog.lifetime?.oneTimePurchaseOfferDetails?.formattedPrice ?: "Lifetime") } }; Text("Free cleaning remains available. Purchase is never required to delete a file.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
@Composable private fun PrivacyCenterScreen(modifier: Modifier, preferences: AppPreferences) { var analyticsEnabled by remember { mutableStateOf(preferences.analyticsEnabled) }; LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 24.dp)) { item { Text("Privacy Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Clear answers about access and data.", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { PrivacyCard(Icons.Default.PhoneAndroid, "What is accessed", "Only media made available through Android MediaStore permissions, plus user-selected files through the Storage Access Framework.") }; item { PrivacyCard(Icons.Default.Security, "What stays on device", "Scanning, metadata, analysis results and settings remain local. Media content is not uploaded for normal cleaning.") }; item { PrivacyCard(Icons.Default.Block, "What is never requested", "No broad storage permission, contacts, SMS, microphone, location or camera permission is used by the core cleaner.") }; item { Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Optional product analytics", fontWeight = FontWeight.SemiBold); Text("Off by default. Never includes file names, contents or full paths.", style = MaterialTheme.typography.bodySmall) }; Switch(checked = analyticsEnabled, onCheckedChange = { analyticsEnabled = it; preferences.analyticsEnabled = it }) } } } } }
@Composable private fun PrivacyCard(icon: ImageVector, title: String, body: String) { Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
@Composable private fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Construction, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("This workspace is planned for the next implementation phase.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

private fun formatBytes(bytes: Long): String { if (bytes < 1024) return "$bytes B"; val units = arrayOf("KB", "MB", "GB", "TB"); var value = bytes.toDouble(); var index = -1; do { value /= 1024.0; index++ } while (value >= 1024 && index < units.lastIndex); return "%.1f %s".format(value, units[index]) }
