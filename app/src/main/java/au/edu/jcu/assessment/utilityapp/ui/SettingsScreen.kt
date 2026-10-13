package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.data.FootballSchedule
import au.edu.jcu.assessment.utilityapp.ui.theme.ThemeMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    use24HourFormat: Boolean,
    onTimeFormatChanged: (Boolean) -> Unit,
    selectedTimezone: String,
    onTimezoneChanged: (String) -> Unit,
    displayZone: ZoneId,
    followedTeams: List<String>?,
    schedule: FootballSchedule?,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, color = Color.White)
        ThemeSettingsCard(themeMode, onThemeModeChanged)
        SettingsGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(value = use24HourFormat, role = Role.Switch,
                        onValueChange = onTimeFormatChanged).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Use 24-hour time", style = MaterialTheme.typography.titleMedium)
                    Text(if (use24HourFormat) "Example: 20:30" else "Example: 8:30 PM",
                        style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
                }
                Switch(checked = use24HourFormat, onCheckedChange = null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4B8148),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.75f),
                        uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = Color.White.copy(alpha = 0.5f)
                    ))
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
            Text("Timezone", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            Text("Kickoff times and dates follow this timezone.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            Column(modifier = Modifier.selectableGroup()) {
                listOf(
                    "Device" to "Phone timezone (${ZoneId.systemDefault().id})",
                    "Asia/Shanghai" to "Beijing · Asia/Shanghai",
                    "Australia/Brisbane" to "Brisbane · Australia/Brisbane",
                    "Europe/London" to "London · Europe/London",
                    "UTC" to "UTC"
                ).forEach { (zoneId, label) ->
                    SettingsRadioRow(label, selectedTimezone == zoneId) { onTimezoneChanged(zoneId) }
                }
            }
        }
        SettingsGlassCard {
            Text("Schedule information", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            Text("Data provided by football-data.org", style = MaterialTheme.typography.bodyMedium)
            Text("Current coverage: " + (schedule?.competitions?.joinToString() ?: "Not loaded"),
                style = MaterialTheme.typography.bodyMedium)
            Text("Free-plan scores may be delayed. Kickoff times can change.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            Text("Schedules are cached for up to 3 hours; updates are checked when you reopen the app.",
                style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            schedule?.let {
                Text("Updated: " + it.fetchedAt.atZone(displayZone).format(
                    DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH)),
                    style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
            Text("All times: ${displayZone.id}", style = MaterialTheme.typography.bodyMedium)
            Text("Showing: All competitions", style = MaterialTheme.typography.bodyMedium)
            Text("Following: ${followedTeams?.joinToString()?.ifEmpty { "No teams selected" } ?: "Not loaded"}",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SettingsGlassCard(content: @Composable () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun SettingsRadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = selected, onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Color.White,
                unselectedColor = Color.White.copy(alpha = 0.6f)))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
private fun ThemeSettingsCard(themeMode: ThemeMode, onThemeModeChanged: (ThemeMode) -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    SettingsGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() })
                Text(if (dark) "Dark theme" else "Light theme",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f))
            }
            IconToggleButton(
                checked = dark,
                onCheckedChange = { onThemeModeChanged(if (it) ThemeMode.DARK else ThemeMode.LIGHT) },
                modifier = Modifier.size(48.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .semantics {
                        contentDescription = "Dark theme"
                        stateDescription = if (dark) "On" else "Off"
                    }
            ) {
                MorphingThemeIcon(dark = dark, modifier = Modifier.size(24.dp))
            }
        }
        if (themeMode == ThemeMode.SYSTEM) {
            Text("Following device setting", style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f))
        } else {
            TextButton(onClick = { onThemeModeChanged(ThemeMode.SYSTEM) },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text("Use device setting")
            }
        }
    }
}
