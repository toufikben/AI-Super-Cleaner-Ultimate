package com.aisupercleaner.ultimate

import android.os.Bundle
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
import com.aisupercleaner.ultimate.privacy.AppPreferences
import com.aisupercleaner.ultimate.privacy.StoragePermissionManager
import com.aisupercleaner.ultimate.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AISuperCleanerTheme { CleanerApp() } }
    }
}

data class NavItem(val label: String, val icon: ImageVector)
data class Finding(val title: String, val detail: String, val size: String, val icon: ImageVector, val tint: Color)

@Composable
fun CleanerApp() {
    val context = LocalContext.current
    val preferences = remember { AppPreferences(context) }
    var selected by remember { mutableIntStateOf(0) }
    var showPermissionEducation by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionMessage = if (result.values.all { it }) {
            "Access granted. Smart Scan can now inspect supported media through Android's official APIs."
        } else {
            "Access was not granted. Nothing was scanned. You can enable media access later from Android Settings."
        }
    }
    val navItems = listOf(
        NavItem("Home", Icons.Default.Home), NavItem("Clean", Icons.Default.AutoAwesome),
        NavItem("Analyze", Icons.Default.PieChart), NavItem("Tools", Icons.Default.Build),
        NavItem("Settings", Icons.Default.Settings)
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(selected = selected == index, onClick = { selected = index },
                        icon = { Icon(item.icon, contentDescription = item.label) }, label = { Text(item.label, fontSize = 11.sp) })
                }
            }
        }
    ) { padding ->
        when (selected) {
            0 -> HomeScreen(Modifier.padding(padding), onSmartScan = { showPermissionEducation = true })
            4 -> PrivacyCenterScreen(Modifier.padding(padding), preferences)
            else -> PlaceholderScreen(navItems[selected].label, Modifier.padding(padding))
        }
    }
    if (showPermissionEducation) {
        PermissionEducationDialog(
            onDismiss = { showPermissionEducation = false },
            onContinue = {
                showPermissionEducation = false
                val missing = StoragePermissionManager.missingPermissions(context)
                if (missing.isEmpty()) permissionMessage = "Media access is already available. The real scanner will use it in the next phase."
                else permissionLauncher.launch(missing)
            }
        )
    }
    permissionMessage?.let { message ->
        AlertDialog(onDismissRequest = { permissionMessage = null }, confirmButton = { TextButton(onClick = { permissionMessage = null }) { Text("OK") } }, title = { Text("Permission status") }, text = { Text(message) })
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onSmartScan: () -> Unit) {
    val findings = listOf(
        Finding("Large videos", "12 items · Review recommended", "3.2 GB", Icons.Default.VideoLibrary, SoftBlue),
        Finding("Duplicate photos", "38 items · High confidence", "2.1 GB", Icons.Default.PhotoLibrary, SoftMint),
        Finding("Screenshots", "146 items · Review recommended", "1.4 GB", Icons.Default.Screenshot, SoftLavender),
        Finding("Old downloads", "24 items · User review", "900 MB", Icons.Default.Download, SoftAmber)
    )
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 24.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Header() }
        item { ScoreCard() }
        item { PrimaryActions(onSmartScan) }
        item { SectionTitle("Smart recommendations", "Explainable findings based on your storage") }
        items(findings) { FindingCard(it) }
        item { TrustNote() }
    }
}

@Composable private fun Header() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column { Text("Good afternoon", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary); Text("Your storage, simplified.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications") }
    }
}

@Composable private fun ScoreCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Storage health", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelLarge); Text("78 / 100", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
                Box(Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.VerifiedUser, null, tint = Color.White, modifier = Modifier.size(34.dp)) }
            }
            LinearProgressIndicator(progress = { .78f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = ScoreAccent, trackColor = Color.White.copy(alpha = .18f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Used 92.4 GB", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall); Text("Free 35.6 GB", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable private fun PrimaryActions(onSmartScan: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onSmartScan, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(10.dp)); Text("SMART SCAN", fontWeight = FontWeight.Bold) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.CleaningServices, null); Spacer(Modifier.width(6.dp)); Text("Quick clean") }
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.PieChart, null); Spacer(Modifier.width(6.dp)); Text("Analyze") }
        }
    }
}

@Composable private fun SectionTitle(title: String, subtitle: String) { Column(verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun FindingCard(finding: Finding) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(finding.tint), contentAlignment = Alignment.Center) { Icon(finding.icon, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(finding.title, fontWeight = FontWeight.SemiBold); Text(finding.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Column(horizontalAlignment = Alignment.End) { Text(finding.size, fontWeight = FontWeight.Bold); Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun TrustNote() { Card(colors = CardDefaults.cardColors(containerColor = SoftMint), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Lock, null, tint = ScoreAccent); Spacer(Modifier.width(12.dp)); Column { Text("Private by design", fontWeight = FontWeight.SemiBold); Text("Your media stays on your device. Review every recommendation before cleaning.", style = MaterialTheme.typography.bodySmall) } } } }

@Composable private fun PermissionEducationDialog(onDismiss: () -> Unit, onContinue: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { Button(onClick = onContinue) { Text("Continue") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }, icon = { Icon(Icons.Default.FolderOpen, null) }, title = { Text("Choose what can be scanned") }, text = { Text("AI Super Cleaner requests access only to media categories needed for Smart Scan. Your photos, videos and audio stay on this device. The app does not use MANAGE_EXTERNAL_STORAGE and will never scan inaccessible files.") })
}

@Composable private fun PrivacyCenterScreen(modifier: Modifier, preferences: AppPreferences) {
    var analyticsEnabled by remember { mutableStateOf(preferences.analyticsEnabled) }
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 24.dp)) {
        item { Text("Privacy Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Clear answers about access and data.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { PrivacyCard(Icons.Default.PhoneAndroid, "What is accessed", "Only media made available through Android MediaStore permissions, plus user-selected files through the Storage Access Framework.") }
        item { PrivacyCard(Icons.Default.Security, "What stays on device", "Scanning, hashes, analysis results, recommendations, selections and settings remain local. Media content is not uploaded for normal cleaning.") }
        item { PrivacyCard(Icons.Default.Block, "What is never requested", "No MANAGE_EXTERNAL_STORAGE, contacts, SMS, microphone, location or camera permission is used by the core cleaner.") }
        item { Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Optional product analytics", fontWeight = FontWeight.SemiBold); Text("Off by default. Never includes file names, contents or full paths.", style = MaterialTheme.typography.bodySmall) }; Switch(checked = analyticsEnabled, onCheckedChange = { analyticsEnabled = it; preferences.analyticsEnabled = it }) } } }
    }
}

@Composable private fun PrivacyCard(icon: ImageVector, title: String, body: String) { Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(4.dp)); Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }

@Composable private fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) { Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Construction, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("This workspace is planned for the next implementation phase.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
