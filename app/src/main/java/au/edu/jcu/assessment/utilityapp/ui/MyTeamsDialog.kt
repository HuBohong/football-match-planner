package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun MyTeamsDialog(
    followedTeams: List<String>,
    teamsByLeague: Map<String, List<String>>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Edit a draft so Cancel/Back cannot change the active schedule filter.
    var draftTeams by rememberSaveable { mutableStateOf(ArrayList(followedTeams)) }
    var expandedCompetition by rememberSaveable { mutableStateOf<String?>(null) }
    val unavailableTeams = followedTeams - teamsByLeague.values.flatten().toSet()
    val selectableTeams = teamsByLeague + if (unavailableTeams.isEmpty()) emptyMap()
        else mapOf("Previously followed · not in current coverage" to unavailableTeams)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My teams") },
        confirmButton = {
            TextButton(onClick = { onSave(draftTeams.distinct()) }) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Choose a competition, then select teams. A team followed in one list is followed in every list.")
                selectableTeams.forEach { (league, teams) ->
                    OutlinedButton(
                        onClick = {
                            expandedCompetition = if (expandedCompetition == league) null else league
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(league, modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleSmall)
                            Text("${teams.size} teams · ${teams.count { it in draftTeams }} followed",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Text(if (expandedCompetition == league) "Hide" else "Choose")
                    }
                    if (expandedCompetition == league) {
                        teams.forEach { team ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .toggleable(
                                        value = team in draftTeams,
                                        role = Role.Checkbox,
                                        onValueChange = { checked ->
                                            draftTeams = ArrayList(
                                                if (checked) (draftTeams + team).distinct() else draftTeams - team
                                            )
                                        }
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Checkbox(checked = team in draftTeams, onCheckedChange = null)
                                Text(team, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    )
}
