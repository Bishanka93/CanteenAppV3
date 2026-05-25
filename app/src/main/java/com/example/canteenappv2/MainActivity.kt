package com.example.canteenappv2

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Box
import com.example.canteenappv2.ui.*
import com.example.canteenappv2.database.MySQLDatabase
import com.example.canteenappv2.ui.theme.CanteenAppV2Theme
import kotlinx.coroutines.launch
import android.util.Log
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    // Reconnect when the app comes back from background (screen off, switch apps, etc.)
    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsPref = getSharedPreferences("settings", MODE_PRIVATE)
        val authPref = getSharedPreferences("auth", MODE_PRIVATE)

        setContent {
            var currentUser by remember { mutableStateOf<User?>(null) }
            var isConnecting by remember { mutableStateOf(true) }

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
            val scope = rememberCoroutineScope()

            // Connect to MySQL, then restore the saved session if any
            LaunchedEffect(Unit) {
                val connected = MySQLDatabase.connect()
                if (connected) {
                    Log.d("Database", "Connected to MySQL successfully")
                    // Restore previously logged-in user from the saved roll_no
                    val savedRollNo = authPref.getString("roll_no", null)
                    if (savedRollNo != null) {
                        currentUser = MySQLDatabase.getUserByRollNo(savedRollNo)
                    }
                } else {
                    Log.e("Database", "MySQL connection failed")
                }
                isConnecting = false
            }

            DisposableEffect(Unit) {
                onDispose { MySQLDatabase.disconnect() }
            }

            CanteenAppV2Theme(darkTheme = useDarkTheme) {
                if (isConnecting) {
                    // Show a simple splash while the DB connects
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                } else if (currentUser == null) {
                    var showSignUp by remember { mutableStateOf(false) }
                    if (showSignUp) {
                        SignUpScreen(
                            onSignUpSuccess = { rollNo, name, password ->
                                scope.launch {
                                    val newUser = User(name, rollNo, password)
                                    val added = MySQLDatabase.addUser(newUser)
                                    if (added) {
                                        authPref.edit { putString("roll_no", rollNo) }
                                    } else {
                                        Log.e("SignUp", "Failed to insert user into MySQL")
                                    }
                                }
                            },
                            onNavigateToLogin = { showSignUp = false }
                        )
                    } else {
                        LoginScreen(
                            onLoginSuccess = { rollNo, _ ->
                                scope.launch {
                                    val user = MySQLDatabase.getUserByRollNo(rollNo)
                                    if (user != null) {
                                        currentUser = user
                                        authPref.edit { putString("roll_no", rollNo) }
                                    }
                                }
                            },
                            onNavigateToSignUp = { showSignUp = true }
                        )
                    }
                } else {
                    when {
                        currentUser!!.isAdmin -> AdminApp(
                            user = currentUser!!,
                            darkTheme = useDarkTheme,
                            onDarkThemeChange = { isDark ->
                                darkThemePreference = isDark
                                settingsPref.edit { putBoolean("dark_theme", isDark) }
                            },
                            onLogout = {
                                currentUser = null
                                authPref.edit { remove("roll_no") }
                            }
                        )
                        currentUser!!.isStaff -> StaffApp(
                            user = currentUser!!,
                            darkTheme = useDarkTheme,
                            onDarkThemeChange = { isDark ->
                                darkThemePreference = isDark
                                settingsPref.edit { putBoolean("dark_theme", isDark) }
                            },
                            onLogout = {
                                currentUser = null
                                authPref.edit { remove("roll_no") }
                            }
                        )
                        else -> CanteenAppV2App(
                            user = currentUser!!,
                            darkTheme = useDarkTheme,
                            onDarkThemeChange = { isDark ->
                                settingsPref.edit { putBoolean("dark_theme", isDark) }
                            },
                            onLogout = {
                                currentUser = null
                                authPref.edit { remove("roll_no") }
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

    suspend fun confirmOrder(canteenName: String, items: List<CartItem>): Int {
        val canteenId = items.first().foodItem.canteenId
        val token = MySQLDatabase.getNextToken()
        val success = MySQLDatabase.addOrder(token, items, canteenId, canteenName, user.rollNo)
        return if (success) {
            // Remove only the items belonging to the ordered canteen from the cart
            cartItems = cartItems.filter { it.foodItem.canteenId != canteenId }
            token
        } else {
            Log.e("Order", "Failed to save order to MySQL")
            -1 // signals failure; CartScreen should handle -1 gracefully
        }
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
                    onConfirmOrder = { canteenName, items ->
                        confirmOrder(canteenName, items)
                    },
                    onDone = { currentDestination = AppDestinations.CANTEENS }
                )
                AppDestinations.WAITLIST -> WaitlistScreen(
                    modifier = Modifier.padding(innerPadding),
                    currentUserRollNo = user.rollNo
                    // No orders parameter — WaitlistScreen now self-fetches from MySQL
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