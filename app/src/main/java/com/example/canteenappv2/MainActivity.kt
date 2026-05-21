package com.example.canteenappv2

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.canteenappv2.ui.*
import com.example.canteenappv2.ui.theme.CanteenAppV2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settingsPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val authPref = getSharedPreferences("auth", Context.MODE_PRIVATE)
        
        fun loadUsers(): List<User> {
            val usersStr = authPref.getString("all_users", null) ?: return listOf(
                User("Bishanka Sarma", "DC2024BTE0093", "12345"),
                User("Canteen Manager", "STAFF_A", "admin123", isStaff = true, canteenId = 1)
            )
            return usersStr.split("|").filter { it.isNotBlank() }.mapNotNull {
                val parts = it.split(";")
                if (parts.size >= 3) {
                    User(
                        parts[0], 
                        parts[1], 
                        parts[2], 
                        isStaff = parts.getOrNull(3)?.toBoolean() ?: false,
                        canteenId = parts.getOrNull(4)?.toIntOrNull()
                    )
                } else null
            }
        }

        fun saveUsers(users: List<User>) {
            val usersStr = users.joinToString("|") { 
                "${it.name};${it.rollNo};${it.password};${it.isStaff};${it.canteenId ?: ""}" 
            }
            authPref.edit().putString("all_users", usersStr).apply()
        }

        setContent {
            var usersList by remember { mutableStateOf(loadUsers()) }
            
            var currentUser by remember { 
                val rollNo = authPref.getString("roll_no", null)
                val initialUser = usersList.find { it.rollNo == rollNo }
                mutableStateOf(initialUser)
            }
            
            var darkThemePreference by remember { 
                mutableStateOf(
                    if (settingsPref.contains("dark_theme")) {
                        settingsPref.getBoolean("dark_theme", false)
                    } else {
                        null
                    }
                ) 
            }
            
            val useDarkTheme = darkThemePreference ?: isSystemInDarkTheme()

            CanteenAppV2Theme(darkTheme = useDarkTheme) {
                if (currentUser == null) {
                    var showSignUp by remember { mutableStateOf(false) }
                    if (showSignUp) {
                        SignUpScreen(
                            onSignUpSuccess = { signedUpRollNo, name, password ->
                                val newUser = User(name, signedUpRollNo, password)
                                usersList = usersList + newUser
                                saveUsers(usersList)
                                currentUser = newUser
                                authPref.edit().putString("roll_no", signedUpRollNo).apply()
                            },
                            onNavigateToLogin = { showSignUp = false }
                        )
                    } else {
                        LoginScreen(
                            onLoginSuccess = { loggedInRollNo, _ ->
                                currentUser = usersList.find { it.rollNo == loggedInRollNo }
                                authPref.edit().putString("roll_no", loggedInRollNo).apply()
                            },
                            onNavigateToSignUp = { showSignUp = true },
                            users = usersList
                        )
                    }
                } else {
                    if (currentUser!!.isStaff) {
                        StaffApp(
                            user = currentUser!!,
                            darkTheme = useDarkTheme,
                            onDarkThemeChange = { isDark -> 
                                darkThemePreference = isDark
                                settingsPref.edit().putBoolean("dark_theme", isDark).apply()
                            },
                            onLogout = {
                                currentUser = null
                                authPref.edit().remove("roll_no").apply()
                            }
                        )
                    } else {
                        CanteenAppV2App(
                            user = currentUser!!,
                            darkTheme = useDarkTheme,
                            onDarkThemeChange = { isDark -> 
                                darkThemePreference = isDark
                                settingsPref.edit().putBoolean("dark_theme", isDark).apply()
                            },
                            onLogout = {
                                currentUser = null
                                authPref.edit().remove("roll_no").apply()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CanteenAppV2App(
    user: User,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CANTEENS) }
    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }
    val orders = Database.orders
    
    var persistentSelectedCanteen by remember { mutableStateOf<Canteen?>(null) }
    var persistentSelectedFoodItem by remember { mutableStateOf<FoodItem?>(null) }

    fun addToCart(foodItem: FoodItem, quantity: Int) {
        if (quantity <= 0) {
            cartItems = cartItems.filter { it.foodItem.id != foodItem.id }
            return
        }
        val existing = cartItems.find { it.foodItem.id == foodItem.id }
        cartItems = if (existing != null) {
            cartItems.map { if (it.foodItem.id == foodItem.id) it.copy(quantity = quantity) else it }
        } else {
            cartItems + CartItem(foodItem, quantity)
        }
    }

    fun confirmOrder(canteenName: String, items: List<CartItem>): Int {
        val token = Database.getNextToken()
        val newOrder = OrderItem(
            token = token,
            items = items,
            canteenName = canteenName
        )
        Database.orders.add(newOrder)
        cartItems = cartItems.filter { it.foodItem.canteenId != items.first().foodItem.canteenId }
        return token
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = { Icon(it.icon, contentDescription = it.label) },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.CANTEENS -> CanteensScreen(
                    modifier = Modifier.padding(innerPadding),
                    cartItems = cartItems,
                    onAddToCart = ::addToCart,
                    selectedCanteen = persistentSelectedCanteen,
                    onCanteenSelected = { persistentSelectedCanteen = it },
                    selectedFoodItem = persistentSelectedFoodItem,
                    onFoodItemSelected = { persistentSelectedFoodItem = it }
                )
                AppDestinations.CART -> CartScreen(
                    modifier = Modifier.padding(innerPadding),
                    cartItems = cartItems,
                    onConfirmOrder = ::confirmOrder,
                    onDone = { currentDestination = AppDestinations.CANTEENS }
                )
                AppDestinations.WAITLIST -> WaitlistScreen(
                    modifier = Modifier.padding(innerPadding),
                    orders = orders
                )
                AppDestinations.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    user = user,
                    darkTheme = darkTheme,
                    onDarkThemeChange = onDarkThemeChange,
                    onLogout = onLogout
                )
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: ImageVector) {
    CANTEENS("Canteens", Icons.Default.Home),
    CART("Cart", Icons.Default.ShoppingCart),
    WAITLIST("Waitlist", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", Icons.Default.AccountCircle),
}
