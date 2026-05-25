package com.example.canteenappv2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.canteenappv2.database.MySQLDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminApp(
    user: User,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(AdminDestinations.CANTEENS) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AdminDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Admin: ${currentDestination.label}", fontWeight = FontWeight.Bold) }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AdminDestinations.CANTEENS -> AdminCanteensScreen()
                    AdminDestinations.STAFF -> AdminUsersScreen(isStaff = true)
                    AdminDestinations.CUSTOMERS -> AdminUsersScreen(isStaff = false)
                    AdminDestinations.FOOD -> AdminAllFoodScreen()
                    AdminDestinations.SETTINGS -> SettingsScreen(
                        user = user,
                        darkTheme = darkTheme,
                        onDarkThemeChange = onDarkThemeChange,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Canteens
// ---------------------------------------------------------------------------

@Composable
fun AdminCanteensScreen() {
    val scope = rememberCoroutineScope()
    var canteens by remember { mutableStateOf<List<Canteen>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var editingCanteen by remember { mutableStateOf<Canteen?>(null) }

    // Load canteens from MySQL
    LaunchedEffect(Unit) {
        canteens = MySQLDatabase.getAllCanteens()
        isLoading = false
    }

    fun refresh() {
        scope.launch {
            isLoading = true
            canteens = MySQLDatabase.getAllCanteens()
            isLoading = false
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingCanteen = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Canteen")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(canteens) { canteen ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(canteen.name, style = MaterialTheme.typography.titleMedium)
                            Row {
                                IconButton(onClick = {
                                    editingCanteen = canteen
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        MySQLDatabase.deleteCanteen(canteen.id)
                                        refresh()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            CanteenDialog(
                canteen = editingCanteen,
                onDismiss = { showDialog = false },
                onSaved = { refresh() }
            )
        }
    }
}

@Composable
fun CanteenDialog(canteen: Canteen?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(canteen?.name ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (canteen == null) "Add Canteen" else "Edit Canteen") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Canteen Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                enabled = !isSaving && name.isNotBlank(),
                onClick = {
                    scope.launch {
                        isSaving = true
                        if (canteen == null) {
                            MySQLDatabase.addCanteen(name)
                        } else {
                            MySQLDatabase.updateCanteen(canteen.id, name)
                        }
                        isSaving = false
                        onSaved()
                        onDismiss()
                    }
                }
            ) { Text(if (isSaving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------------------
// Users
// ---------------------------------------------------------------------------

@Composable
fun AdminUsersScreen(isStaff: Boolean) {
    val scope = rememberCoroutineScope()
    var allUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var canteens by remember { mutableStateOf<List<Canteen>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        allUsers = MySQLDatabase.getAllUsers()
        canteens = MySQLDatabase.getAllCanteens()
        isLoading = false
    }

    fun refresh() {
        scope.launch {
            isLoading = true
            allUsers = MySQLDatabase.getAllUsers()
            isLoading = false
        }
    }

    val users = allUsers.filter { it.isStaff == isStaff && !it.isAdmin }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingUser = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(user.name, style = MaterialTheme.typography.titleMedium)
                                Text("ID: ${user.rollNo}", style = MaterialTheme.typography.bodySmall)
                                if (isStaff) {
                                    val canteenName = canteens.find { it.id == user.canteenId }?.name ?: "No Canteen"
                                    Text("Canteen: $canteenName", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Row {
                                IconButton(onClick = {
                                    editingUser = user
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        MySQLDatabase.deleteUser(user.rollNo)
                                        refresh()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            UserDialog(
                user = editingUser,
                isStaff = isStaff,
                canteens = canteens,
                onDismiss = { showDialog = false },
                onSaved = { refresh() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDialog(
    user: User?,
    isStaff: Boolean,
    canteens: List<Canteen>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(user?.name ?: "") }
    var rollNo by remember { mutableStateOf(user?.rollNo ?: "") }
    var password by remember { mutableStateOf(user?.password ?: "") }
    var canteenId by remember { mutableIntStateOf(user?.canteenId ?: canteens.firstOrNull()?.id ?: 0) }
    var expanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user == null) "Add ${if (isStaff) "Staff" else "Customer"}" else "Edit User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(
                    value = rollNo,
                    onValueChange = { rollNo = it },
                    label = { Text("ID / Roll No") },
                    enabled = user == null
                )
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })

                if (isStaff) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = canteens.find { it.id == canteenId }?.name ?: "Select Canteen",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Canteen") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            canteens.forEach { canteen ->
                                DropdownMenuItem(
                                    text = { Text(canteen.name) },
                                    onClick = {
                                        canteenId = canteen.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving && name.isNotBlank() && rollNo.isNotBlank() && password.isNotBlank(),
                onClick = {
                    scope.launch {
                        isSaving = true
                        val newUser = User(
                            name = name,
                            rollNo = rollNo,
                            password = password,
                            isStaff = isStaff,
                            canteenId = if (isStaff) canteenId else null
                        )
                        if (user == null) {
                            MySQLDatabase.addUser(newUser)
                        } else {
                            MySQLDatabase.updateUser(newUser)
                        }
                        isSaving = false
                        onSaved()
                        onDismiss()
                    }
                }
            ) { Text(if (isSaving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------------------------------------------------------------------
// All Food (read-only overview for admin)
// ---------------------------------------------------------------------------

@Composable
fun AdminAllFoodScreen() {
    var canteens by remember { mutableStateOf<List<Canteen>>(emptyList()) }
    var foodItems by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        canteens = MySQLDatabase.getAllCanteens()
        foodItems = MySQLDatabase.getAllFoodItems()
        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        canteens.forEach { canteen ->
            item {
                Text(
                    text = canteen.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            val items = foodItems.filter { it.canteenId == canteen.id }
            items(items) { food ->
                StaffFoodItemWidget(food) // Reusing the staff widget
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Nav destinations
// ---------------------------------------------------------------------------

enum class AdminDestinations(val label: String, val icon: ImageVector) {
    CANTEENS("Canteens", Icons.Default.Store),
    STAFF("Staff", Icons.Default.SupervisorAccount),
    CUSTOMERS("Customers", Icons.Default.People),
    FOOD("All Food", Icons.Default.Fastfood),
    SETTINGS("Settings", Icons.Default.Settings)
}