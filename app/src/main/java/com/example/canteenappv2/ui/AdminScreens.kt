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

@Composable
fun AdminCanteensScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var editingCanteen by remember { mutableStateOf<Canteen?>(null) }

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
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Database.canteens) { canteen ->
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
                            IconButton(onClick = { Database.canteens.remove(canteen) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            CanteenDialog(
                canteen = editingCanteen,
                onDismiss = { showDialog = false }
            )
        }
    }
}

@Composable
fun CanteenDialog(canteen: Canteen?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(canteen?.name ?: "") }

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
            Button(onClick = {
                if (canteen == null) {
                    val id = (Database.canteens.maxOfOrNull { it.id } ?: 0) + 1
                    Database.canteens.add(Canteen(id, name))
                } else {
                    val index = Database.canteens.indexOf(canteen)
                    if (index != -1) Database.canteens[index] = canteen.copy(name = name)
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminUsersScreen(isStaff: Boolean) {
    val users = Database.users.filter { it.isStaff == isStaff && !it.isAdmin }
    var showDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<User?>(null) }

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
                                val canteenName = Database.canteens.find { it.id == user.canteenId }?.name ?: "No Canteen"
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
                            IconButton(onClick = { Database.users.remove(user) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
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
                onDismiss = { showDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDialog(user: User?, isStaff: Boolean, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var rollNo by remember { mutableStateOf(user?.rollNo ?: "") }
    var password by remember { mutableStateOf(user?.password ?: "") }
    var canteenId by remember { mutableStateOf(user?.canteenId ?: Database.canteens.firstOrNull()?.id ?: 0) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (user == null) "Add ${if (isStaff) "Staff" else "Customer"}" else "Edit User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = rollNo, onValueChange = { rollNo = it }, label = { Text("ID / Roll No") })
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
                
                if (isStaff) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = Database.canteens.find { it.id == canteenId }?.name ?: "Select Canteen",
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
                            Database.canteens.forEach { canteen ->
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
            Button(onClick = {
                val newUser = User(name, rollNo, password, isStaff = isStaff, canteenId = if (isStaff) canteenId else null)
                if (user == null) {
                    Database.users.add(newUser)
                } else {
                    val index = Database.users.indexOf(user)
                    if (index != -1) Database.users[index] = newUser
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminAllFoodScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Database.canteens.forEach { canteen ->
            item {
                Text(
                    text = canteen.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            val items = Database.foodItems.filter { it.canteenId == canteen.id }
            items(items) { food ->
                StaffFoodItemWidget(food) // Reusing the staff widget
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
        }
    }
}

enum class AdminDestinations(val label: String, val icon: ImageVector) {
    CANTEENS("Canteens", Icons.Default.Store),
    STAFF("Staff", Icons.Default.SupervisorAccount),
    CUSTOMERS("Customers", Icons.Default.People),
    FOOD("All Food", Icons.Default.Fastfood),
    SETTINGS("Settings", Icons.Default.Settings)
}
