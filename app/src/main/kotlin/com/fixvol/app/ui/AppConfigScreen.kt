package com.fixvol.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fixvol.app.data.AppMetadata
import com.fixvol.app.rules.RuleMode
import com.fixvol.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppConfigScreen(
    app: AppMetadata,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val currentMode = settings.appRules[app.packageName]?.mode ?: RuleMode.FOLLOW_GLOBAL

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app.appName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBgBase)
            )
        },
        containerColor = LightBgBase
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Volume panel behavior for ${app.appName}",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LightSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val options = listOf(
                        RuleMode.FOLLOW_GLOBAL to "Follow global rules",
                        RuleMode.ALWAYS_SHOW to "Always show volume panel",
                        RuleMode.NEVER_SHOW to "Never show volume panel"
                    )

                    options.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (mode == currentMode),
                                    onClick = { viewModel.setAppRuleMode(app.packageName, mode) }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (mode == currentMode),
                                onClick = { viewModel.setAppRuleMode(app.packageName, mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryEmerald)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
