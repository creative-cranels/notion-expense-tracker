package kz.cranels.expensetracker

import android.app.Application
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kz.cranels.expensetracker.ui.theme.ExpenseTrackerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("NotionPrefs", Context.MODE_PRIVATE)

    var integrationSecret by remember { mutableStateOf(sharedPrefs.getString("integration_secret", "") ?: "") }
    var databaseId by remember { mutableStateOf(sharedPrefs.getString("database_id", "") ?: "") }
    val isSyncing by viewModel.isSyncing.collectAsState()

    var reminderEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("reminder_enabled", false)) }
    var reminderHour by remember { mutableStateOf(sharedPrefs.getInt("reminder_hour", 11)) }
    var reminderMinute by remember { mutableStateOf(sharedPrefs.getInt("reminder_minute", 30)) }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            reminderHour = hour
            reminderMinute = minute
        },
        reminderHour,
        reminderMinute,
        true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notion Integration", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = integrationSecret,
                        onValueChange = { integrationSecret = it },
                        label = { Text("Internal Integration Secret") },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = databaseId,
                        onValueChange = { databaseId = it },
                        label = { Text("Database ID") },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Button(
                        onClick = {
                            viewModel.syncCategories(integrationSecret, databaseId) { success ->
                                val message = if (success) "Categories Synced!" else "Sync Failed"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sync Categories")
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notifications", style = MaterialTheme.typography.titleLarge)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Daily Reminder", modifier = Modifier.weight(1f))
                        Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                    }
                    Text(
                        text = String.format("%02d:%02d", reminderHour, reminderMinute),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = reminderEnabled) { timePickerDialog.show() }
                            .padding(vertical = 16.dp)
                    )
                }
            }

            Button(
                onClick = {
                    with(sharedPrefs.edit()) {
                        putString("integration_secret", integrationSecret)
                        putString("database_id", databaseId)
                        putBoolean("reminder_enabled", reminderEnabled)
                        putInt("reminder_hour", reminderHour)
                        putInt("reminder_minute", reminderMinute)
                        apply()
                    }

                    if (reminderEnabled) {
                        ReminderManager.scheduleReminder(context, reminderHour, reminderMinute)
                    } else {
                        ReminderManager.cancelReminder(context)
                    }

                    Toast.makeText(context, "Settings Saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ExpenseTrackerTheme {
        SettingsScreen(rememberNavController())
    }
}
