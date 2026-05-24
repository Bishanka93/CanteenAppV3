package com.example.canteenappv2.database

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.canteenappv2.ui.Canteen
import com.example.canteenappv2.ui.CartItem
import com.example.canteenappv2.ui.FoodItem
import com.example.canteenappv2.ui.OrderItem
import com.example.canteenappv2.ui.OrderStatus
import com.example.canteenappv2.ui.User
import kotlinx.coroutines.launch

/**
 * Example Composable showing how to use MySQL Database in your screens
 * Copy these patterns to your actual screens
 */

@Composable
fun ExampleScreenWithDatabase() {
    val scope = rememberCoroutineScope()
    val repository = remember { DatabaseRepository() }

    // State management
    val canteens = remember { mutableStateOf<List<Canteen>>(emptyList()) }
    val foodItems = remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }

    // Load data on composition
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading.value = true
                canteens.value = repository.getCanteens()
                foodItems.value = repository.getFoodItems()
                error.value = null
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                isLoading.value = false
            }
        }
    }

    // UI
    when {
        isLoading.value -> Text("Loading...")
        error.value != null -> Text("Error: ${error.value}")
        else -> {
            Text("Loaded ${canteens.value.size} canteens and ${foodItems.value.size} items")
        }
    }
}

@Composable
fun ExampleLoginScreen() {
    val scope = rememberCoroutineScope()
    val repository = remember { DatabaseRepository() }
    val loginResult = remember { mutableStateOf<User?>(null) }

    // When user taps login button:
    fun handleLogin(rollNo: String, password: String) {
        scope.launch {
            try {
                val user = repository.loginUser(rollNo, password)
                loginResult.value = user
                if (user != null) {
                    // Navigate to home screen
                    println("Login successful: ${user.name}")
                } else {
                    // Show error
                    println("Login failed: Invalid credentials")
                }
            } catch (e: Exception) {
                println("Login error: ${e.message}")
            }
        }
    }

    Text("Example: Call handleLogin(\"DC2024BTE0093\", \"12345\")")
}

@Composable
fun ExampleOrderScreen() {
    val scope = rememberCoroutineScope()
    val repository = remember { DatabaseRepository() }
    val orders = remember { mutableStateOf<List<OrderItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            orders.value = repository.getOrders()
        }
    }

    fun placeNewOrder(items: List<CartItem>, canteenId: Int, canteenName: String) {
        scope.launch {
            val token = repository.placeOrder(items, canteenId, canteenName)
            if (token != null) {
                println("Order placed with token: $token")
            }
        }
    }

    fun updateOrderStatus(token: Int, newStatus: OrderStatus) {
        scope.launch {
            val success = repository.updateOrderStatus(token, newStatus)
            if (success) {
                println("Order status updated to: $newStatus")
                // Refresh orders list
                orders.value = repository.getOrders()
            }
        }
    }

    Text("Orders loaded: ${orders.value.size}")
}

@Composable
fun ExampleAdminScreen() {
    val scope = rememberCoroutineScope()
    val repository = remember { DatabaseRepository() }

    fun toggleFoodItemAvailability(foodItemId: Int, isAvailable: Boolean) {
        scope.launch {
            val success = repository.setFoodItemAvailability(foodItemId, isAvailable)
            if (success) {
                println("Food item availability updated")
            }
        }
    }

    Text("Admin Screen - Toggle food item availability")
}

// Initialize database in MainActivity onCreate:
/*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                scope.launch {
                    val repository = DatabaseRepository()
                    val connected = repository.connectToDatabase()
                    if (connected) {
                        Log.d("Database", "Connected to MySQL")
                    } else {
                        Log.e("Database", "Failed to connect to MySQL")
                    }
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    val repository = DatabaseRepository()
                    repository.disconnectFromDatabase()
                }
            }

            CanteenAppV2Theme {
                // Your app content
            }
        }
    }
*/

