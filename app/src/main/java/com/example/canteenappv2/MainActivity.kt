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
        
        setContent {
            var rollNo by remember { 
                mutableStateOf(authPref.getString("roll_no", null)) 
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
                if (rollNo == null) {
                    LoginScreen(onLoginSuccess = { loggedInRollNo ->
                        rollNo = loggedInRollNo
                        authPref.edit().putString("roll_no", loggedInRollNo).apply()
                    })
                } else {
                    CanteenAppV2App(
                        rollNo = rollNo!!,
                        darkTheme = useDarkTheme,
                        onDarkThemeChange = { isDark -> 
                            darkThemePreference = isDark
                            settingsPref.edit().putBoolean("dark_theme", isDark).apply()
                        },
                        onLogout = {
                            rollNo = null
                            authPref.edit().remove("roll_no").apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CanteenAppV2App(
    rollNo: String,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.CANTEENS) }
    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }
    var orders by remember { mutableStateOf(listOf<OrderItem>()) }
    var nextToken by remember { mutableIntStateOf(101) }
    
    // Persistent canteen selection for the first tab
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

    fun confirmOrder(): Int {
        val token = nextToken++
        val newOrder = OrderItem(
            token = token,
            items = cartItems.toList(),
            canteenName = persistentSelectedCanteen?.name ?: "Unknown Canteen"
        )
        orders = orders + newOrder
        cartItems = emptyList()
        return token
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
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
                    onDone = {
                        currentDestination = AppDestinations.CANTEENS
                    }
                )
                AppDestinations.WAITLIST -> WaitlistScreen(
                    modifier = Modifier.padding(innerPadding),
                    orders = orders
                )
                AppDestinations.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    rollNo = rollNo,
                    darkTheme = darkTheme,
                    onDarkThemeChange = onDarkThemeChange,
                    onLogout = onLogout
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    CANTEENS("Canteens", Icons.Default.Home),
    CART("Cart", Icons.Default.ShoppingCart),
    WAITLIST("Waitlist", Icons.AutoMirrored.Filled.List),
    SETTINGS("Settings", Icons.Default.AccountCircle),
}
