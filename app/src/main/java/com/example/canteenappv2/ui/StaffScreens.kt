package com.example.canteenappv2.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.canteenappv2.database.MySQLDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StaffApp(
    user: User,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var currentDestination by remember { mutableStateOf(StaffDestinations.ORDERS) }
    var canteens by remember { mutableStateOf<List<Canteen>>(emptyList()) }

    LaunchedEffect(Unit) {
        canteens = MySQLDatabase.getAllCanteens()
    }

    val canteen = canteens.find { it.id == user.canteenId }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            StaffDestinations.entries.forEach {
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
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(currentDestination.label, fontWeight = FontWeight.Bold)
                            canteen?.let {
                                Text(it.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                val canteenId = user.canteenId ?: 1
                when (currentDestination) {
                    StaffDestinations.FOOD -> StaffFoodScreen(canteenId)
                    StaffDestinations.ORDERS -> StaffOrdersScreen(canteenId)
                    StaffDestinations.SETTINGS -> SettingsScreen(
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
fun StaffFoodScreen(canteenId: Int) {
    var showAddDialog by remember { mutableStateOf(false) }
    var foodItems by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch { foodItems = MySQLDatabase.getFoodItemsByCanteen(canteenId) }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Food")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(foodItems, key = { it.id }) { item ->
                StaffFoodItemWidget(item, onRefresh = { refresh() })
            }
        }

        if (showAddDialog) {
            AddFoodDialog(
                canteenId = canteenId,
                onDismiss = { showAddDialog = false },
                onRefresh = { refresh() }
            )
        }
    }
}

/**
 * [onRefresh] is optional so this widget can be reused read-only in AdminAllFoodScreen.
 */
@Composable
fun StaffFoodItemWidget(item: FoodItem, onRefresh: (() -> Unit)? = null) {
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imageModel = remember(item.imageName) {
        if (item.imageName?.startsWith("content://") == true) {
            Uri.parse(item.imageName)
        } else {
            val resId = context.resources.getIdentifier(item.imageName, "drawable", context.packageName)
            if (resId != 0) resId else null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isAvailable) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = item.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Rs ${item.price}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (item.isAvailable) "In Stock" else "Out of Stock",
                    color = if (item.isAvailable) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Controls only shown when an onRefresh callback is provided (i.e. staff context)
            if (onRefresh != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Switch(
                        checked = item.isAvailable,
                        onCheckedChange = { available ->
                            scope.launch {
                                if (MySQLDatabase.updateFoodItemAvailability(item.id, available)) {
                                    onRefresh()
                                }
                            }
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            }
        }
    }

    if (showEditDialog && onRefresh != null) {
        EditFoodDialog(item = item, onDismiss = { showEditDialog = false }, onRefresh = onRefresh)
    }
}

@Composable
fun AddFoodDialog(canteenId: Int, onDismiss: () -> Unit, onRefresh: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Food") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Text("Tap to add photo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && price.isNotBlank(),
                onClick = {
                    val p = price.toDoubleOrNull() ?: 0.0
                    scope.launch {
                        if (MySQLDatabase.addFoodItem(name, p, canteenId, imageUri?.toString())) {
                            onRefresh()
                            onDismiss()
                        }
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditFoodDialog(item: FoodItem, onDismiss: () -> Unit, onRefresh: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var price by remember { mutableStateOf(item.price.toString()) }
    var imageUriString by remember { mutableStateOf(item.imageName) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) imageUriString = uri.toString() }

    val imageModel = remember(imageUriString) {
        if (imageUriString?.startsWith("content://") == true) {
            Uri.parse(imageUriString)
        } else {
            val resId = context.resources.getIdentifier(imageUriString, "drawable", context.packageName)
            if (resId != 0) resId else null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Food") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Food Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Text("Tap to change photo", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    if (MySQLDatabase.updateFoodItem(
                            item.id, name,
                            price.toDoubleOrNull() ?: item.price,
                            imageUriString
                        )
                    ) {
                        onRefresh()
                        onDismiss()
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StaffOrdersScreen(canteenId: Int) {
    var orders by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            // Filter by this canteen AND exclude completed orders
            orders = MySQLDatabase.getAllOrders(canteenId = canteenId)
                .filter { it.status != OrderStatus.COMPLETED }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(3000)
        }
    }

    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active orders", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                StaffOrderWidget(order, onRefresh = { refresh() })
            }
        }
    }
}

@Composable
fun StaffOrderWidget(order: OrderItem, onRefresh: () -> Unit) {
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Token: ${order.token}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                val statusColor = when (order.status) {
                    OrderStatus.PENDING -> MaterialTheme.colorScheme.error
                    OrderStatus.PREPARING -> Color(0xFFFFA000)
                    OrderStatus.READY -> Color(0xFF4CAF50)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(order.status.name, color = statusColor, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            order.items.forEach {
                Text("${it.foodItem.name} x ${it.quantity}", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (order.status) {
                    OrderStatus.PENDING -> Button(
                        onClick = {
                            scope.launch {
                                if (MySQLDatabase.updateOrderStatus(order.token, OrderStatus.PREPARING)) onRefresh()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Start Preparing") }

                    OrderStatus.PREPARING -> Button(
                        onClick = {
                            scope.launch {
                                if (MySQLDatabase.updateOrderStatus(order.token, OrderStatus.READY)) onRefresh()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Mark Ready") }

                    OrderStatus.READY -> Button(
                        onClick = {
                            scope.launch {
                                if (MySQLDatabase.updateOrderStatus(order.token, OrderStatus.COMPLETED)) onRefresh()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Complete Order") }

                    else -> {}
                }
            }
        }
    }
}

enum class StaffDestinations(val label: String, val icon: ImageVector) {
    FOOD("Food", Icons.Default.ShoppingCart),
    ORDERS("Orders", Icons.Default.Menu),
    SETTINGS("Settings", Icons.Default.AccountCircle)
}