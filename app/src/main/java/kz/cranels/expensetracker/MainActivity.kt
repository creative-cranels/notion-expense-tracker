package kz.cranels.expensetracker

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.yield
import kz.cranels.expensetracker.data.local.Category
import kz.cranels.expensetracker.ui.theme.CategoryColor
import kz.cranels.expensetracker.ui.theme.CategoryColorContent
import kz.cranels.expensetracker.ui.theme.CustomBackgroundWhite
import kz.cranels.expensetracker.ui.theme.CustomPrimaryTeal
import kz.cranels.expensetracker.ui.theme.ExpenseTrackerTheme
import kz.cranels.expensetracker.ui.theme.KeypadDelete
import kz.cranels.expensetracker.ui.theme.KeypadDeleteContent
import kz.cranels.expensetracker.ui.theme.KeypadNormal
import kz.cranels.expensetracker.ui.theme.KeypadSpecial
import kz.cranels.expensetracker.ui.theme.KeypadSpecialContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. You can now schedule notifications.
        } else {
            // Explain to the user that the feature is unavailable
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermission()

        setContent {
            ExpenseTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "expense_screen") {
        composable("expense_screen") {
            ExpenseScreen(navController = navController)
        }
        composable("settings_screen") {
            SettingsScreen(navController = navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ExpenseScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = viewModel(
        factory = ExpenseViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sharedPrefs = remember { context.getSharedPreferences("NotionPrefs", Application.MODE_PRIVATE) }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val categories by viewModel.categories.collectAsState()
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    val isSaving by viewModel.isSaving.collectAsState()

    val dateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)

        LaunchedEffect(datePickerState) {
            snapshotFlow { datePickerState.selectedDateMillis }
                .drop(1) // Ignore the initial null value
                .collect { millis ->
                    millis?.let {
                        selectedDate = Date(it)
                        showDatePicker = false
                    }
                }
        }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { /* Empty to hide the button */ }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    fun onNumberPress(number: String) {
        // If the amount is currently "0", replace it with the new number.
        // Otherwise, append the new number.
        if (amount == "0") {
            amount = number
        } else {
            amount += number
        }
    }

    fun onBackspacePress() {
        // If the amount string has more than one character, remove the last one.
        if (amount.length > 1) {
            amount = amount.dropLast(1)
        }
        // If it only has one character, reset it to "0" instead of making it empty.
        else {
            amount = "0"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {  },
                actions = {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.textButtonColors(
                                containerColor = KeypadSpecial,
                                contentColor = KeypadSpecialContent
                            )
                        ) {
                            Text(
                                dateFormatter.format(selectedDate),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        ExposedDropdownMenuBox(
                            expanded = isCategoryDropdownExpanded,
                            onExpandedChange = { isCategoryDropdownExpanded = !it },
                            modifier = Modifier.weight(1f)
                        ) {
                            TextButton(
                                onClick = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                                shape = RoundedCornerShape(24.dp), // Same shape as the date button
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .menuAnchor(),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = CustomPrimaryTeal,
                                    contentColor = CategoryColorContent
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        selectedCategory?.name ?: "Category",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            ExposedDropdownMenu(
                                expanded = isCategoryDropdownExpanded,
                                onDismissRequest = { isCategoryDropdownExpanded = false }
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = {
                                            selectedCategory = category
                                            isCategoryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { navController.navigate("settings_screen") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "₸$amount",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // This is the Text that acts as our placeholder
                if (description.isBlank()) {
                    Text(
                        text = "Add description...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline // Use a subtle color
                    )
                }
                BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    )
                )
            }
            // This pushes the keyboard to the bottom
            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }), // Slides in from the bottom
                exit = slideOutVertically(targetOffsetY = { it })  // Slides out to the bottom
            ) {
                // This is the container for our keyboard
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)

                ) {
                    // First row of keys
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNumberPress("1") },
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KeypadNormal)
                        ) {
                            Text("1", style = MaterialTheme.typography.headlineMedium)
                        }
                        Button(
                            onClick = { onNumberPress("2") },
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KeypadNormal)
                        ) {
                            Text("2", style = MaterialTheme.typography.headlineMedium)
                        }
                        Button(
                            onClick = { onNumberPress("3") },
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KeypadNormal)
                        ) {
                            Text("3", style = MaterialTheme.typography.headlineMedium)
                        }
                        IconButton(
                            onClick = { onBackspacePress() },
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = KeypadDelete, contentColor = KeypadDeleteContent),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNumberPress("4") },
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KeypadNormal)
                        ) {
                            Text("4", style = MaterialTheme.typography.headlineMedium)
                        }
                        Button(
                            onClick = { onNumberPress("5") },
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KeypadNormal)
                        ) {
                            Text("5", style = MaterialTheme.typography.headlineMedium)
                        }
                        Button(
                            onClick = { onNumberPress("6") },
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KeypadNormal)
                        ) {
                            Text("6", style = MaterialTheme.typography.headlineMedium)
                        }
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = KeypadSpecial, contentColor = KeypadSpecialContent) // <-- The correct defaults object
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "DateRange")
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(3f), // Takes up 3/4 of the space
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick={ onNumberPress("7") },
                                    modifier=Modifier
                                        .weight(1f)
                                        .height(80.dp),
                                    shape=RoundedCornerShape(24.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor = KeypadNormal)){
                                    Text("7",style=MaterialTheme.typography.headlineMedium)
                                }
                                Button(
                                    onClick={ onNumberPress("8") },
                                    modifier=Modifier
                                        .weight(1f)
                                        .height(80.dp),
                                    shape=RoundedCornerShape(24.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor = KeypadNormal)){
                                    Text("8",style=MaterialTheme.typography.headlineMedium)
                                }
                                Button(
                                    onClick={ onNumberPress("9") },
                                    modifier=Modifier
                                        .weight(1f)
                                        .height(80.dp),
                                    shape=RoundedCornerShape(24.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor = KeypadNormal)){
                                    Text("9",style=MaterialTheme.typography.headlineMedium)
                                }
                            }
                            // The new "0" button goes below
                            Row( modifier = Modifier.fillMaxWidth() ) {
                                Button(
                                    onClick={ onNumberPress("0") },
                                    modifier=Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    shape=RoundedCornerShape(24.dp),
                                    colors=ButtonDefaults.buttonColors(containerColor = KeypadNormal)){
                                    Text("0",style=MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                        Button(
                            onClick = {
                                val token = sharedPrefs.getString("integration_secret", null)
                                val dbId = sharedPrefs.getString("database_id", null)
                                if (token == null || dbId == null) {
                                    Toast.makeText(context, "Please set credentials in settings", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (amount.isBlank() || description.isBlank()) {
                                    Toast.makeText(context, "Please enter amount and description", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedCategory == null) {
                                    Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                viewModel.saveExpense(token, dbId, description, amount, selectedCategory!!.id, selectedDate) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Expense Saved!", Toast.LENGTH_SHORT).show()
                                        amount = ""
                                        description = ""
                                        selectedCategory = null
                                        selectedDate = Date()
                                    } else {
                                        Toast.makeText(context, "Error saving expense", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(1f) // Takes up the last 1/4 of the space
                                .height(168.dp), // Spans two rows (80dp + 8dp space + 80dp)
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpenseScreenPreview() {
    ExpenseTrackerTheme {
        val navController = rememberNavController()
        ExpenseScreen(navController)
    }
}
