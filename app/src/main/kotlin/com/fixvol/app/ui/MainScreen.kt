package com.fixvol.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fixvol.app.audio.AudioCategory
import com.fixvol.app.data.AppMetadata
import com.fixvol.app.rules.RuleMode
import com.fixvol.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToAppConfig: (AppMetadata) -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FixVol",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Native volume control for broken buttons",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDebug) {
                        Icon(Icons.Default.BugReport, contentDescription = "Debug Diagnostics")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBgBase)
            )
        },
        containerColor = LightBgBase
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Status Card
            item {
                StatusCard(
                    enabled = settings.enabled,
                    onToggle = { viewModel.toggleMasterEnable(it) }
                )
            }

            // Test Volume Panel Action
            item {
                TestVolumeCard(
                    testResult = testResult,
                    onTestClick = { viewModel.testVolumePanel() }
                )
            }

            // Category Rules Section Header
            item {
                Text(
                    text = "Automatic volume panel",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Global Category Toggles
            item {
                CategoryTogglesCard(
                    categoryRules = settings.globalRules.categoryRules,
                    onCategoryToggle = { cat, enabled -> viewModel.toggleCategory(cat, enabled) }
                )
            }

            // App Rules Section Header
            item {
                Text(
                    text = "Applications",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Installed Apps List
            items(installedApps) { app ->
                AppRowItem(
                    app = app,
                    ruleMode = settings.appRules[app.packageName]?.mode ?: RuleMode.FOLLOW_GLOBAL,
                    onClick = { onNavigateToAppConfig(app) }
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (enabled) StatusActiveGreen else StatusPausedAmber)
                )
                Column {
                    Text(
                        text = if (enabled) "Active" else "Paused",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (enabled) "Listening for audio playback" else "Monitoring is turned off",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LightSurface,
                    checkedTrackColor = PrimaryEmerald
                )
            )
        }
    }
}

@Composable
fun TestVolumeCard(
    testResult: String?,
    onTestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Test Native Volume Panel",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Triggers SystemUI volume panel & verifies volume stays unchanged",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onTestClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Test")
                }
            }

            if (testResult != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = testResult,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = if (testResult.startsWith("✓")) StatusActiveGreen else Color.Red
                )
            }
        }
    }
}

@Composable
fun CategoryTogglesCard(
    categoryRules: Map<AudioCategory, Boolean>,
    onCategoryToggle: (AudioCategory, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            val categories = listOf(
                AudioCategory.MEDIA to "Media",
                AudioCategory.ALARM to "Alarm",
                AudioCategory.RINGTONE to "Ringtone",
                AudioCategory.NOTIFICATION to "Notifications",
                AudioCategory.VOICE_CALL to "Calls",
                AudioCategory.SYSTEM to "System"
            )

            categories.forEachIndexed { index, (category, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Switch(
                        checked = categoryRules[category] ?: false,
                        onCheckedChange = { onCategoryToggle(category, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LightSurface,
                            checkedTrackColor = PrimaryEmerald
                        )
                    )
                }
                if (index < categories.size - 1) {
                    HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun AppRowItem(
    app: AppMetadata,
    ruleMode: RuleMode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (ruleMode) {
                        RuleMode.FOLLOW_GLOBAL -> "Global"
                        RuleMode.ALWAYS_SHOW -> "Always"
                        RuleMode.NEVER_SHOW -> "Never"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextSecondary
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
        }
    }
}
