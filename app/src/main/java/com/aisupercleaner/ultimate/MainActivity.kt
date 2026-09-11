package com.aisupercleaner.ultimate

import android.os.Bundle
import android.os.Environment
import android.os.StatFs
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.data.AppDatabase
import com.aisupercleaner.ultimate.data.RecommendationEngine
import com.aisupercleaner.ultimate.data.DuplicateEngine
import com.aisupercleaner.ultimate.data.MediaAnalysisReport
import com.aisupercleaner.ultimate.data.SmartCleanupReport
import com.aisupercleaner.ultimate.data.StorageScanner
import com.aisupercleaner.ultimate.data.ScanProgress
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
            0 -> HomeScreen(Modifier.padding(padding), fileCount, totalBytes, imageCount, videoCount, audioCount, freeBytes, totalStorageBytes, report, mediaReport, scanProgress, isScanning, onSmartScan = { showPermissionEducation = true })
            4 -> PrivacyCenterScreen(Modifier.padding(padding), preferences)
            else -> PlaceholderScreen(navItems[selected].label, Modifier.padding(padding))
        }
    }
    if (showPermissionEducation) PermissionEducationDialog(onDismiss = { showPermissionEducation = false }, onContinue = { showPermissionEducation = false; val missing = StoragePermissionManager.missingPermissions(context); if (missing.isEmpty()) startScan() else permissionLauncher.launch(missing) })
    permissionMessage?.let { message -> AlertDialog(onDismissRequest = { permissionMessage = null }, confirmButton = { TextButton(onClick = { permissionMessage = null }) { Text("OK") } }, title = { Text("Scan status") }, text = { Text(message) }) }
}

@Composable
fun HomeScreen(modifier: Modifier, fileCount: Int, totalBytes: Long, imageCount: Int, videoCount: Int, audioCount: Int, freeBytes: Long, totalStorageBytes: Long, report: SmartCleanupReport?, mediaReport: MediaAnalysisReport?, progress: ScanProgress?, isScanning: Boolean, onSmartScan: () -> Unit) {
    val findings = listOf(
        Finding("Photos", "$imageCount items · From MediaStore", "—", Icons.Default.PhotoLibrary, SoftMint),
        Finding("Videos", "$videoCount items · From MediaStore", "—", Icons.Default.VideoLibrary, SoftBlue),
        Finding("Audio", "$audioCount items · From MediaStore", "—", Icons.Default.Audiotrack, SoftLavender)
    )
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Header() }
        item { ScoreCard(fileCount, totalBytes, freeBytes, totalStorageBytes, report) }
        item { PrimaryActions(onSmartScan, isScanning) }
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

@Composable private fun PrimaryActions(onSmartScan: () -> Unit, isScanning: Boolean) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onSmartScan, enabled = !isScanning, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { if (isScanning) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("SCANNING…") } else { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(10.dp)); Text("SMART SCAN", fontWeight = FontWeight.Bold) } }; Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.CleaningServices, null); Spacer(Modifier.width(6.dp)); Text("Quick clean") }; OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.PieChart, null); Spacer(Modifier.width(6.dp)); Text("Analyze") } } } }

@Composable private fun ScanProgressCard(progress: ScanProgress) { Card(colors = CardDefaults.cardColors(containerColor = SoftBlue), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text(progress.stage, fontWeight = FontWeight.SemiBold) }; Text("${progress.scannedFiles} files indexed · ${formatBytes(progress.discoveredBytes)}", style = MaterialTheme.typography.bodySmall) } } }
@Composable private fun ExplainableSummary(report: SmartCleanupReport) { Card(colors = CardDefaults.cardColors(containerColor = SoftMint), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = ScoreAccent); Spacer(Modifier.width(8.dp)); Text("Explainable analysis", fontWeight = FontWeight.SemiBold) }; Text(if (report.potentialReviewBytes > 0) "Potentially recoverable for review: ${formatBytes(report.potentialReviewBytes)}" else "No high-confidence review category found", fontWeight = FontWeight.Bold); Text(report.explanation, style = MaterialTheme.typography.bodySmall) } } }
@Composable private fun RecommendationCard(recommendation: com.aisupercleaner.ultimate.data.Recommendation) { Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(recommendation.category, fontWeight = FontWeight.SemiBold); Text(formatBytes(recommendation.estimatedBytes), fontWeight = FontWeight.Bold) }; Text("${recommendation.fileCount} files · ${recommendation.confidencePercent}% confidence", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary); Text(recommendation.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun MediaAnalysisSummary(report: MediaAnalysisReport) { Card(colors = CardDefaults.cardColors(containerColor = SoftLavender), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.PhotoFilter, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(8.dp)); Text("Media analysis", fontWeight = FontWeight.SemiBold) }; Text("${report.duplicateGroups.size} exact duplicate groups · ${report.similarGroups.size} similar groups", fontWeight = FontWeight.Bold); Text("${report.blurredFiles.size} potentially blurry photos · ${report.screenshotFiles.size} screenshots", style = MaterialTheme.typography.bodySmall); Text("Groups are shown for review. Nothing is selected or deleted automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun SectionTitle(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun FindingCard(finding: Finding) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(finding.tint), contentAlignment = Alignment.Center) { Icon(finding.icon, null, tint = MaterialTheme.colorScheme.primary) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(finding.title, fontWeight = FontWeight.SemiBold); Text(finding.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(finding.size, fontWeight = FontWeight.Bold) } } }
@Composable private fun TrustNote() { Card(colors = CardDefaults.cardColors(containerColor = SoftMint), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Lock, null, tint = ScoreAccent); Spacer(Modifier.width(12.dp)); Column { Text("Private by design", fontWeight = FontWeight.SemiBold); Text("Media metadata is indexed locally. Review every recommendation before cleaning.", style = MaterialTheme.typography.bodySmall) } } } }
@Composable private fun PermissionEducationDialog(onDismiss: () -> Unit, onContinue: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onContinue) { Text("Continue") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }, icon = { Icon(Icons.Default.FolderOpen, null) }, title = { Text("Choose what can be scanned") }, text = { Text("Smart Scan reads only media categories needed for an on-device inventory. Nothing is uploaded, inaccessible files are not scanned, and no broad storage permission is used.") }) }
@Composable private fun PrivacyCenterScreen(modifier: Modifier, preferences: AppPreferences) { var analyticsEnabled by remember { mutableStateOf(preferences.analyticsEnabled) }; LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 24.dp)) { item { Text("Privacy Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Clear answers about access and data.", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { PrivacyCard(Icons.Default.PhoneAndroid, "What is accessed", "Only media made available through Android MediaStore permissions, plus user-selected files through the Storage Access Framework.") }; item { PrivacyCard(Icons.Default.Security, "What stays on device", "Scanning, metadata, analysis results and settings remain local. Media content is not uploaded for normal cleaning.") }; item { PrivacyCard(Icons.Default.Block, "What is never requested", "No broad storage permission, contacts, SMS, microphone, location or camera permission is used by the core cleaner.") }; item { Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Optional product analytics", fontWeight = FontWeight.SemiBold); Text("Off by default. Never includes file names, contents or full paths.", style = MaterialTheme.typography.bodySmall) }; Switch(checked = analyticsEnabled, onCheckedChange = { analyticsEnabled = it; preferences.analyticsEnabled = it }) } } } } }
@Composable private fun PrivacyCard(icon: ImageVector, title: String, body: String) { Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
@Composable private fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Construction, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("This workspace is planned for the next implementation phase.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

private fun formatBytes(bytes: Long): String { if (bytes < 1024) return "$bytes B"; val units = arrayOf("KB", "MB", "GB", "TB"); var value = bytes.toDouble(); var index = -1; do { value /= 1024.0; index++ } while (value >= 1024 && index < units.lastIndex); return "%.1f %s".format(value, units[index]) }
